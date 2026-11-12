package rsnrdk.websec.scheduling;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;

import java.time.Duration;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs our real job classes and job/trigger definitions in a real (in-memory) Quartz scheduler,
 * so merging, @DisallowConcurrentExecution and re-firing are done by Quartz itself, not simulated.
 */
class QuartzRuntimeBehaviourTest {

    private static final Duration WAIT = Duration.ofSeconds(5);

    private final QuartzJobsConfig config = new QuartzJobsConfig("Europe/Kyiv");
    private final AuditLogCleanupService cleanupService = mock(AuditLogCleanupService.class);
    private final ActivityReportService reportService = mock(ActivityReportService.class);

    private Scheduler scheduler;

    @AfterEach
    void tearDown() throws SchedulerException {
        if (scheduler != null) {
            scheduler.shutdown(true);
        }
    }

    private void startScheduler(JobFactory jobFactory) throws SchedulerException {
        Properties props = new Properties();
        props.setProperty("org.quartz.scheduler.instanceName", "test-" + UUID.randomUUID());
        props.setProperty("org.quartz.threadPool.threadCount", "3");
        props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        scheduler = new StdSchedulerFactory(props).getScheduler();
        scheduler.setJobFactory(jobFactory);
        scheduler.start();
    }

    private Job createJob(Class<? extends Job> jobClass) {
        if (jobClass == AuditLogCleanupJob.class) {
            return new AuditLogCleanupJob(cleanupService);
        }
        if (jobClass == DailyActivityReportJob.class) {
            return new DailyActivityReportJob(reportService);
        }
        return new ConcurrentProbeJob(cleanupService);
    }

    /**
     * Same key, job and JobDataMap as the configured trigger, but fires right now.
     */
    private static Trigger firingNow(Trigger configured) {
        return TriggerBuilder.newTrigger()
                .withIdentity(configured.getKey())
                .forJob(configured.getJobKey())
                .usingJobData(configured.getJobDataMap())
                .startNow()
                .build();
    }

    private static Trigger nowTrigger(String name, JobDetail job) {
        return TriggerBuilder.newTrigger().withIdentity(name, "test").forJob(job).startNow().build();
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + WAIT.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("Condition not met within " + WAIT);
            }
            Thread.sleep(20);
        }
    }

    // ---------- Scenario: trigger parameters override job defaults (merged by Quartz) ----------

    @Test
    void realScheduler_mergesJobDefaultsWithEachTriggerData() throws Exception {
        // Arrange
        startScheduler((bundle, sched) -> createJob(bundle.getJobDetail().getJobClass()));
        JobDetail job = config.auditLogCleanupJobDetail();
        Trigger nightly = firingNow(config.auditLogCleanupNightlyTrigger());
        Trigger anonymous = firingNow(config.auditLogCleanupAnonymousTrigger());

        // Act
        scheduler.scheduleJob(job, Set.of(nightly, anonymous), true);

        // Assert: nightly trigger -> pure defaults; anonymous trigger -> its overrides + default dryRun
        verify(cleanupService, timeout(WAIT.toMillis())).deleteOlderThan(30, false, false);
        verify(cleanupService, timeout(WAIT.toMillis())).deleteOlderThan(7, true, false);
    }

    // ---------- Scenario: parallel execution is forbidden ----------

    @Test
    void disallowConcurrentExecution_secondFireWaitsForTheFirst() throws Exception {
        // Arrange: each run takes 300 ms and records how many runs are active at the same time
        AtomicInteger running = new AtomicInteger();
        AtomicInteger maxRunning = new AtomicInteger();
        AtomicInteger finished = new AtomicInteger();
        doAnswer(inv -> {
            maxRunning.accumulateAndGet(running.incrementAndGet(), Math::max);
            Thread.sleep(300);
            running.decrementAndGet();
            finished.incrementAndGet();
            return 0;
        }).when(cleanupService).deleteOlderThan(anyInt(), anyBoolean(), anyBoolean());
        startScheduler((bundle, sched) -> createJob(bundle.getJobDetail().getJobClass()));
        JobDetail job = config.auditLogCleanupJobDetail();

        // Act: two triggers of the same job fire at the same moment, 3 worker threads are free
        scheduler.scheduleJob(job, Set.of(nowTrigger("first", job), nowTrigger("second", job)), true);
        waitUntil(() -> finished.get() == 2);

        // Assert
        assertEquals(1, maxRunning.get(), "the second run must wait for the first one");
    }

    @Test
    void withoutTheAnnotation_sameSetupRunsInParallel() throws Exception {
        // control experiment: proves the previous test really measures @DisallowConcurrentExecution
        AtomicInteger running = new AtomicInteger();
        AtomicInteger maxRunning = new AtomicInteger();
        AtomicInteger finished = new AtomicInteger();
        doAnswer(inv -> {
            maxRunning.accumulateAndGet(running.incrementAndGet(), Math::max);
            Thread.sleep(300);
            running.decrementAndGet();
            finished.incrementAndGet();
            return 0;
        }).when(cleanupService).deleteOlderThan(anyInt(), anyBoolean(), anyBoolean());
        startScheduler((bundle, sched) -> createJob(bundle.getJobDetail().getJobClass()));
        JobDetail job = JobBuilder.newJob(ConcurrentProbeJob.class).withIdentity("probe", "test").build();

        scheduler.scheduleJob(job, Set.of(nowTrigger("first", job), nowTrigger("second", job)), true);
        waitUntil(() -> finished.get() == 2);

        assertEquals(2, maxRunning.get());
    }

    // ---------- Scenario: failure is handled on purpose, no endless loop ----------

    @Test
    void reportJob_alwaysFailing_isRetriedLimitedTimes_thenStops() throws Exception {
        // Arrange
        AtomicInteger attempts = new AtomicInteger();
        when(reportService.generateDailyReport(1)).thenAnswer(inv -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("database is down");
        });
        startScheduler((bundle, sched) -> createJob(bundle.getJobDetail().getJobClass()));

        // Act
        scheduler.scheduleJob(config.dailyActivityReportJobDetail(), firingNow(config.dailyActivityReportTrigger()));

        // Assert: 1 attempt + maxRetries(2) re-fires = 3, and then it stays at 3
        waitUntil(() -> attempts.get() == 3);
        Thread.sleep(500);
        assertEquals(3, attempts.get());
    }

    @Test
    void reportJob_failingOnce_succeedsOnRetry_andStops() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        when(reportService.generateDailyReport(1)).thenAnswer(inv -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("temporary glitch");
            }
            return null;
        });
        startScheduler((bundle, sched) -> createJob(bundle.getJobDetail().getJobClass()));

        scheduler.scheduleJob(config.dailyActivityReportJobDetail(), firingNow(config.dailyActivityReportTrigger()));

        waitUntil(() -> attempts.get() == 2);
        Thread.sleep(500);
        assertEquals(2, attempts.get());
    }

    @Test
    void cleanupJob_failing_isNotRetried() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        when(cleanupService.deleteOlderThan(anyInt(), anyBoolean(), anyBoolean())).thenAnswer(inv -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("database is down");
        });
        startScheduler((bundle, sched) -> createJob(bundle.getJobDetail().getJobClass()));

        scheduler.scheduleJob(config.auditLogCleanupJobDetail(), firingNow(config.auditLogCleanupNightlyTrigger()));

        waitUntil(() -> attempts.get() == 1);
        Thread.sleep(500);
        assertEquals(1, attempts.get());
    }

    /**
     * Same work as the cleanup job but WITHOUT @DisallowConcurrentExecution.
     */
    public static class ConcurrentProbeJob implements Job {

        private final AuditLogCleanupService service;

        public ConcurrentProbeJob(AuditLogCleanupService service) {
            this.service = service;
        }

        @Override
        public void execute(JobExecutionContext context) {
            service.deleteOlderThan(30, false, false);
        }
    }
}
