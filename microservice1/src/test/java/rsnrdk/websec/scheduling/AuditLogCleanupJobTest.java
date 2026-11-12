package rsnrdk.websec.scheduling;

import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AuditLogCleanupJobTest {

    private final AuditLogCleanupService cleanupService = mock(AuditLogCleanupService.class);
    private final AuditLogCleanupJob job = new AuditLogCleanupJob(cleanupService);
    private final QuartzJobsConfig config = new QuartzJobsConfig("Europe/Kyiv");

    private JobExecutionContext contextWith(Map<String, Object> data) {
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap(data));
        when(context.getJobDetail()).thenReturn(config.auditLogCleanupJobDetail());
        when(context.getTrigger()).thenReturn(config.auditLogCleanupNightlyTrigger());
        return context;
    }

    // ---------- Scenario: jobs are thin ----------

    @Test
    void execute_readsParameters_andMakesExactlyOneServiceCall() throws Exception {
        // Arrange
        JobExecutionContext context = contextWith(Map.of(
                AuditLogCleanupJob.RETENTION_DAYS, 7,
                AuditLogCleanupJob.ANONYMOUS_ONLY, true,
                AuditLogCleanupJob.DRY_RUN, false));

        // Act
        job.execute(context);

        // Assert
        verify(cleanupService).deleteOlderThan(7, true, false);
        verifyNoMoreInteractions(cleanupService);
    }

    @Test
    void transactionBoundary_isOnServiceMethod_notOnJob() throws Exception {
        assertTrue(AnnotatedElementUtils.hasAnnotation(
                AuditLogCleanupService.class.getMethod("deleteOlderThan", int.class, boolean.class, boolean.class),
                Transactional.class));
        assertFalse(AnnotatedElementUtils.hasAnnotation(
                AuditLogCleanupJob.class.getMethod("execute", JobExecutionContext.class), Transactional.class));
        assertFalse(AnnotatedElementUtils.hasAnnotation(AuditLogCleanupJob.class, Transactional.class));
        // the only dependency of the job is the service
        assertEquals(1, AuditLogCleanupJob.class.getConstructors()[0].getParameterCount());
    }

    // ---------- Scenario: wrong parameters fail explicitly ----------

    @Test
    void execute_keyMisspelled_failsExplicitly_andServiceIsNotCalled() {
        // Arrange: "retentionDay" instead of "retentionDays"
        JobExecutionContext context = contextWith(Map.of(
                "retentionDay", 7,
                AuditLogCleanupJob.ANONYMOUS_ONLY, true,
                AuditLogCleanupJob.DRY_RUN, false));

        // Act
        JobExecutionException ex = assertThrows(JobExecutionException.class, () -> job.execute(context));

        // Assert
        assertInstanceOf(JobParameterException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("'retentionDays'"));
        assertFalse(ex.refireImmediately());
        verifyNoInteractions(cleanupService);
    }

    // ---------- Scenario: failure is handled on purpose ----------

    @Test
    void execute_serviceFails_errorIsRecorded_andJobWaitsForNextRun() {
        // Arrange
        IllegalStateException dbDown = new IllegalStateException("database is down");
        JobExecutionContext context = contextWith(Map.of(
                AuditLogCleanupJob.RETENTION_DAYS, 30,
                AuditLogCleanupJob.ANONYMOUS_ONLY, false,
                AuditLogCleanupJob.DRY_RUN, false));
        when(cleanupService.deleteOlderThan(30, false, false)).thenThrow(dbDown);

        // Act
        JobExecutionException ex = assertThrows(JobExecutionException.class, () -> job.execute(context));

        // Assert: Quartz gets the error, but is told NOT to re-fire -> no loop, next run is the scheduled one
        assertSame(dbDown, ex.getCause());
        assertFalse(ex.refireImmediately());
    }
}
