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
 * Failure strategy: bounded retry. Quartz re-fires the job immediately up to "maxRetries" times,
 * then the error is recorded and the job waits for the next scheduled run.
 * A wrong parameter is a configuration error and is never retried.
 */
@Slf4j
@DisallowConcurrentExecution
@RequiredArgsConstructor
public class DailyActivityReportJob implements Job {

    public static final String DAYS_BACK = "daysBack";
    public static final String MAX_RETRIES = "maxRetries";

    private final ActivityReportService reportService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();
        int daysBack;
        int maxRetries;
        try {
            daysBack = JobParameters.requireInt(data, DAYS_BACK);
            maxRetries = JobParameters.requireInt(data, MAX_RETRIES);
        } catch (JobParameterException e) {
            log.error("Job {} is misconfigured, not retrying: {}", context.getJobDetail().getKey(), e.getMessage());
            throw new JobExecutionException(e, false);
        }

        try {
            reportService.generateDailyReport(daysBack);
        } catch (RuntimeException e) {
            int attempt = context.getRefireCount() + 1;
            boolean retry = context.getRefireCount() < maxRetries;
            if (retry) {
                log.warn("Job {} attempt {} of {} failed, retrying: {}",
                        context.getJobDetail().getKey(), attempt, maxRetries + 1, e.getMessage());
            } else {
                log.error("Job {} failed after {} attempts, waiting for the next scheduled run: {}",
                        context.getJobDetail().getKey(), attempt, e.getMessage(), e);
            }
            throw new JobExecutionException(e, retry);
        }
    }
}
