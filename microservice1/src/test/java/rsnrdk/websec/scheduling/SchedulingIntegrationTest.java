package rsnrdk.websec.scheduling;

import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import rsnrdk.websec.entity.ActivityReport;
import rsnrdk.websec.entity.AuditLog;
import rsnrdk.websec.repository.ActivityReportRepository;
import rsnrdk.websec.repository.AuditLogRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Real Spring context + H2 + Liquibase + Quartz JDBC job store.
 */
@SpringBootTest
class SchedulingIntegrationTest {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    @Autowired
    private AuditLogCleanupService cleanupService;

    @Autowired
    private ActivityReportService reportService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ActivityReportRepository activityReportRepository;

    @Autowired
    private Clock clock;

    private Instant daysAgo(int days, int hour) {
        return LocalDate.now(clock).minusDays(days).atStartOfDay(clock.getZone()).plusHours(hour).toInstant();
    }

    private AuditLog audit(String username, int status, Instant createdAt) {
        return auditLogRepository.save(AuditLog.builder()
                .username(username).method("GET").uri("/api/v1/items").status(status).ip("127.0.0.1")
                .createdAt(createdAt).build());
    }

    // ---------- Scenario: schedule is stored in the database via Liquibase ----------

    @Test
    void quartzTables_areCreatedByLiquibase_notBySpringBoot() {
        Integer quartzTables = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where upper(table_name) like 'QRTZ%'", Integer.class);
        Integer changeSets = jdbcTemplate.queryForObject(
                "select count(*) from databasechangelog where id = '006-create-quartz-tables'", Integer.class);

        assertEquals(11, quartzTables);
        assertEquals(1, changeSets);
        assertEquals("never", environment.getProperty("spring.quartz.jdbc.initialize-schema"));
        assertEquals("jdbc", environment.getProperty("spring.quartz.job-store-type"));
    }

    @Test
    void jobsAndTriggers_arePersistedInQuartzTables() throws Exception {
        assertTrue(scheduler.getMetaData().isJobStoreSupportsPersistence());

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select trigger_name, job_name, job_group from qrtz_triggers order by trigger_name");
        assertEquals(3, rows.size());
        assertEquals("auditLogCleanupAnonymousTrigger", rows.get(0).get("TRIGGER_NAME"));
        assertEquals("auditLogCleanupJob", rows.get(0).get("JOB_NAME"));

        // read back from the database: time zone and misfire instruction survived storage
        CronTrigger stored = (CronTrigger) scheduler.getTrigger(QuartzJobsConfig.CLEANUP_NIGHTLY_TRIGGER);
        assertEquals("Europe/Kyiv", stored.getTimeZone().getID());
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, stored.getMisfireInstruction());
        List<? extends Trigger> cleanupTriggers = scheduler.getTriggersOfJob(QuartzJobsConfig.AUDIT_CLEANUP_JOB);
        assertEquals(2, cleanupTriggers.size());
        assertEquals(30, scheduler.getJobDetail(QuartzJobsConfig.AUDIT_CLEANUP_JOB).getJobDataMap()
                .get(AuditLogCleanupJob.RETENTION_DAYS));
    }

    // ---------- Scenario: re-running is safe (idempotency) ----------

    @Test
    void cleanup_runTwiceInARow_secondRunChangesNothing() {
        // Arrange
        String user = "cleanup-" + UUID.randomUUID();
        AuditLog old = audit(user, 200, daysAgo(40, 10));
        AuditLog fresh = audit(user, 200, daysAgo(0, 0));

        // Act
        int firstRun = cleanupService.deleteOlderThan(30, false, false);
        long countAfterFirst = auditLogRepository.count();
        int secondRun = cleanupService.deleteOlderThan(30, false, false);

        // Assert
        assertTrue(firstRun >= 1);
        assertFalse(auditLogRepository.existsById(old.getId()));
        assertEquals(0, secondRun);
        assertEquals(countAfterFirst, auditLogRepository.count());
        assertTrue(auditLogRepository.existsById(fresh.getId()));
    }

    @Test
    void report_runTwiceInARow_secondRunChangesNothing() {
        // Arrange: activity 3 days ago
        audit("alice", 200, daysAgo(3, 9));
        audit("alice", 404, daysAgo(3, 10));
        audit("anonymous", 200, daysAgo(3, 11));
        LocalDate reportDate = LocalDate.now(clock).minusDays(3);

        // Act
        reportService.generateDailyReport(3);
        ActivityReport afterFirst = activityReportRepository.findByReportDate(reportDate).orElseThrow();
        reportService.generateDailyReport(3);
        ActivityReport stored = activityReportRepository.findByReportDate(reportDate).orElseThrow();

        // Assert: same single row with the same values, not even generatedAt was rewritten
        assertEquals(afterFirst.getId(), stored.getId());
        assertEquals(afterFirst.getGeneratedAt(), stored.getGeneratedAt());
        assertEquals(3, stored.getTotalRequests());
        assertEquals(1, stored.getUniqueUsers());
        assertEquals(1, stored.getErrorRequests());
        Integer rowsForDate = jdbcTemplate.queryForObject(
                "select count(*) from activity_report where report_date = ?", Integer.class, reportDate);
        assertEquals(1, rowsForDate);
    }

    // ---------- End to end: JDBC job store -> Spring job factory -> job -> service -> DB ----------

    @Test
    void triggeringStoredJob_runsThroughSpring_withTriggerOverrides() throws Exception {
        // Arrange
        AuditLog oldAnonymous = audit("anonymous", 200, daysAgo(10, 12));
        AuditLog oldUser = audit("bob-" + UUID.randomUUID(), 200, daysAgo(10, 12));
        JobDataMap overrides = new JobDataMap(Map.of(
                AuditLogCleanupJob.RETENTION_DAYS, 7,
                AuditLogCleanupJob.ANONYMOUS_ONLY, true));

        scheduler.start();
        try {
            // Act
            scheduler.triggerJob(QuartzJobsConfig.AUDIT_CLEANUP_JOB, overrides);

            // Assert: anonymous record removed, regular user's record of the same age kept
            long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
            while (auditLogRepository.existsById(oldAnonymous.getId())) {
                if (System.nanoTime() > deadline) {
                    fail("Job did not run within 10 seconds");
                }
                Thread.sleep(50);
            }
            assertTrue(auditLogRepository.existsById(oldUser.getId()));
        } finally {
            scheduler.standby();
        }
    }
}
