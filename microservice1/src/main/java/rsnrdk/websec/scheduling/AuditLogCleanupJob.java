package rsnrdk.websec.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Thin job: reads its parameters and makes one service call.
 * Failure strategy: record the error and wait for the next scheduled run (no immediate retry).
 */
@Slf4j
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class AuditLogCleanupJob implements Job {

    public static final String RETENTION_DAYS = "retentionDays";
    public static final String ANONYMOUS_ONLY = "anonymousOnly";
    public static final String DRY_RUN = "dryRun";

    private final AuditLogCleanupService cleanupService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();
        try {
            cleanupService.deleteOlderThan(
                    JobParameters.requireInt(data, RETENTION_DAYS),
                    JobParameters.requireBoolean(data, ANONYMOUS_ONLY),
                    JobParameters.requireBoolean(data, DRY_RUN));
        } catch (RuntimeException e) {
            log.error("Job {} (trigger {}) failed, waiting for the next scheduled run: {}",
                    context.getJobDetail().getKey(), context.getTrigger().getKey(), e.getMessage(), e);
            throw new JobExecutionException(e, false);
        }
    }
}
