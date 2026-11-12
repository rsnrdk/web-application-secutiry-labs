package rsnrdk.websec.scheduling;

import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuartzJobsConfigTest {

    private final QuartzJobsConfig config = new QuartzJobsConfig("Europe/Kyiv");

    private final JobDetail cleanupJob = config.auditLogCleanupJobDetail();
    private final JobDetail reportJob = config.dailyActivityReportJobDetail();
    private final CronTrigger nightly = (CronTrigger) config.auditLogCleanupNightlyTrigger();
    private final CronTrigger anonymous = (CronTrigger) config.auditLogCleanupAnonymousTrigger();
    private final CronTrigger report = (CronTrigger) config.dailyActivityReportTrigger();

    /**
     * Exactly what Quartz does before every execution: job defaults first, then trigger values on top.
     */
    private static JobDataMap mergeLikeQuartz(JobDetail job, Trigger trigger) {
        TriggerFiredBundle bundle = new TriggerFiredBundle(job, (OperableTrigger) trigger, null, false,
                new Date(), new Date(), null, null);
        return new JobExecutionContextImpl(null, bundle, null).getMergedJobDataMap();
    }

    // ---------- Scenario: jobs and triggers are declared in one place ----------

    @Test
    void everyJobAndTrigger_hasExplicitNameAndGroup() {
        assertEquals("auditLogCleanupJob", cleanupJob.getKey().getName());
        assertEquals("maintenance", cleanupJob.getKey().getGroup());
        assertEquals("dailyActivityReportJob", reportJob.getKey().getName());
        assertEquals("reports", reportJob.getKey().getGroup());

        assertEquals(QuartzJobsConfig.CLEANUP_NIGHTLY_TRIGGER, nightly.getKey());
        assertEquals(QuartzJobsConfig.CLEANUP_ANONYMOUS_TRIGGER, anonymous.getKey());
        assertEquals(QuartzJobsConfig.ACTIVITY_REPORT_TRIGGER, report.getKey());
        assertEquals("maintenance", nightly.getKey().getGroup());
        assertEquals("reports", report.getKey().getGroup());
    }

    @Test
    void everyTrigger_pointsToItsJobByJobKey_andCleanupJobHasTwoTriggers() {
        assertEquals(cleanupJob.getKey(), nightly.getJobKey());
        assertEquals(cleanupJob.getKey(), anonymous.getJobKey());
        assertEquals(reportJob.getKey(), report.getJobKey());

        long cleanupTriggers = List.of(nightly, anonymous, report).stream()
                .filter(trigger -> trigger.getJobKey().equals(QuartzJobsConfig.AUDIT_CLEANUP_JOB))
                .count();
        assertEquals(2, cleanupTriggers);
    }

    @Test
    void jobs_areDurable_recoverable_andForbidConcurrentExecution() {
        for (JobDetail job : List.of(cleanupJob, reportJob)) {
            assertTrue(job.isDurable(), job.getKey() + " must exist without triggers");
            assertTrue(job.requestsRecovery(), job.getKey() + " must be re-run after a crash");
            assertTrue(job.isConcurrentExecutionDisallowed(), job.getKey() + " must not run twice at the same time");
        }
    }

    // ---------- Scenario: trigger parameters override job defaults ----------

    @Test
    void jobDetail_containsDefaultParameters() {
        JobDataMap defaults = cleanupJob.getJobDataMap();
        assertEquals(30, defaults.get(AuditLogCleanupJob.RETENTION_DAYS));
        assertEquals(false, defaults.get(AuditLogCleanupJob.ANONYMOUS_ONLY));
        assertEquals(false, defaults.get(AuditLogCleanupJob.DRY_RUN));
    }

    @Test
    void mergedData_triggerValueWins_andMissingKeysKeepJobDefaults() {
        // Act
        JobDataMap merged = mergeLikeQuartz(cleanupJob, anonymous);

        // Assert: overridden by the trigger
        assertEquals(7, merged.get(AuditLogCleanupJob.RETENTION_DAYS));
        assertEquals(true, merged.get(AuditLogCleanupJob.ANONYMOUS_ONLY));
        // not defined in the trigger -> job default
        assertEquals(false, merged.get(AuditLogCleanupJob.DRY_RUN));
        // the job's own defaults are untouched
        assertEquals(30, cleanupJob.getJobDataMap().get(AuditLogCleanupJob.RETENTION_DAYS));
    }

    @Test
    void mergedData_triggerWithoutOverrides_usesAllJobDefaults() {
        JobDataMap merged = mergeLikeQuartz(cleanupJob, nightly);

        assertTrue(nightly.getJobDataMap().isEmpty());
        assertEquals(30, merged.get(AuditLogCleanupJob.RETENTION_DAYS));
        assertEquals(false, merged.get(AuditLogCleanupJob.ANONYMOUS_ONLY));
        assertEquals(false, merged.get(AuditLogCleanupJob.DRY_RUN));
    }

    // ---------- Scenario: every trigger has its own misfire instruction ----------

    @Test
    void everyTrigger_hasExplicitMisfireInstruction() {
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, nightly.getMisfireInstruction());
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING, anonymous.getMisfireInstruction());
        assertEquals(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, report.getMisfireInstruction());
        for (Trigger trigger : List.of(nightly, anonymous, report)) {
            assertFalse(trigger.getMisfireInstruction() == Trigger.MISFIRE_INSTRUCTION_SMART_POLICY,
                    trigger.getKey() + " relies on the implicit smart policy");
        }
    }

    // ---------- Scenario: cron triggers do not depend on the server time zone ----------

    @Test
    void cronTriggers_haveExplicitTimeZone() {
        for (CronTrigger trigger : List.of(nightly, anonymous, report)) {
            assertEquals("Europe/Kyiv", trigger.getTimeZone().getID(), trigger.getKey().toString());
        }
    }

    @Test
    void unknownTimeZoneId_isReplacedWithGmt() {
        QuartzJobsConfig misconfigured = new QuartzJobsConfig("Mars/Olympus_Mons");

        CronTrigger trigger = (CronTrigger) misconfigured.auditLogCleanupNightlyTrigger();

        assertEquals("GMT", trigger.getTimeZone().getID());
    }
}
