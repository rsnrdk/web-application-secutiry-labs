package rsnrdk.websec.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * The single place where all Quartz jobs and triggers are declared.
 * Fixed names and groups mean a redeploy updates the stored schedule (overwrite-existing-jobs)
 * instead of silently creating duplicates.
 */
@Slf4j
@Configuration
public class QuartzJobsConfig {

    public static final String MAINTENANCE_GROUP = "maintenance";
    public static final String REPORTS_GROUP = "reports";

    public static final JobKey AUDIT_CLEANUP_JOB = JobKey.jobKey("auditLogCleanupJob", MAINTENANCE_GROUP);
    public static final JobKey ACTIVITY_REPORT_JOB = JobKey.jobKey("dailyActivityReportJob", REPORTS_GROUP);

    public static final TriggerKey CLEANUP_NIGHTLY_TRIGGER = TriggerKey.triggerKey("auditLogCleanupNightlyTrigger", MAINTENANCE_GROUP);
    public static final TriggerKey CLEANUP_ANONYMOUS_TRIGGER = TriggerKey.triggerKey("auditLogCleanupAnonymousTrigger", MAINTENANCE_GROUP);
    public static final TriggerKey ACTIVITY_REPORT_TRIGGER = TriggerKey.triggerKey("dailyActivityReportTrigger", REPORTS_GROUP);

    private final TimeZone timeZone;

    public QuartzJobsConfig(@Value("${scheduling.time-zone:Europe/Kyiv}") String timeZoneId) {
        this.timeZone = resolveTimeZone(timeZoneId);
    }

    /**
     * TimeZone.getTimeZone silently returns GMT for an unknown id, so we at least make it visible in the log.
     */
    static TimeZone resolveTimeZone(String timeZoneId) {
        TimeZone zone = TimeZone.getTimeZone(timeZoneId);
        if (!zone.getID().equals(timeZoneId)) {
            log.warn("Unknown time zone '{}', cron triggers fall back to {}", timeZoneId, zone.getID());
        }
        return zone;
    }

    // ---------- Job 1: audit log cleanup (two triggers) ----------

    @Bean
    public JobDetail auditLogCleanupJobDetail() {
        return JobBuilder.newJob(AuditLogCleanupJob.class)
                .withIdentity(AUDIT_CLEANUP_JOB)
                .withDescription("Deletes old audit log records")
                .usingJobData(AuditLogCleanupJob.RETENTION_DAYS, 30)
                .usingJobData(AuditLogCleanupJob.ANONYMOUS_ONLY, false)
                .usingJobData(AuditLogCleanupJob.DRY_RUN, false)
                .storeDurably()
                .requestRecovery()
                .build();
    }

    /**
     * Every night at 03:00: all records older than 30 days (job defaults, nothing overridden).
     * Misfire: the app was down at 03:00 -> run once as soon as it is back (cleanup is idempotent).
     */
    @Bean
    public Trigger auditLogCleanupNightlyTrigger() {
        return TriggerBuilder.newTrigger()
                .withIdentity(CLEANUP_NIGHTLY_TRIGGER)
                .forJob(AUDIT_CLEANUP_JOB)
                .withDescription("Nightly full cleanup")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 3 * * ?")
                        .inTimeZone(timeZone)
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }

    /**
     * Every 6 hours: anonymous noise older than 7 days. Overrides two keys, dryRun stays the job default.
     * Misfire: skip the missed run, the next one is at most 6 hours away.
     */
    @Bean
    public Trigger auditLogCleanupAnonymousTrigger() {
        return TriggerBuilder.newTrigger()
                .withIdentity(CLEANUP_ANONYMOUS_TRIGGER)
                .forJob(AUDIT_CLEANUP_JOB)
                .withDescription("Frequent cleanup of anonymous requests")
                .usingJobData(AuditLogCleanupJob.RETENTION_DAYS, 7)
                .usingJobData(AuditLogCleanupJob.ANONYMOUS_ONLY, true)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 */6 * * ?")
                        .inTimeZone(timeZone)
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    // ---------- Job 2: daily activity report ----------

    @Bean
    public JobDetail dailyActivityReportJobDetail() {
        return JobBuilder.newJob(DailyActivityReportJob.class)
                .withIdentity(ACTIVITY_REPORT_JOB)
                .withDescription("Builds the activity report for the previous day")
                .usingJobData(DailyActivityReportJob.DAYS_BACK, 1)
                .usingJobData(DailyActivityReportJob.MAX_RETRIES, 2)
                .storeDurably()
                .requestRecovery()
                .build();
    }

    /**
     * Every day at 00:15: report for yesterday.
     * Misfire: yesterday's report must exist, so run once as soon as possible.
     */
    @Bean
    public Trigger dailyActivityReportTrigger() {
        return TriggerBuilder.newTrigger()
                .withIdentity(ACTIVITY_REPORT_TRIGGER)
                .forJob(ACTIVITY_REPORT_JOB)
                .withDescription("Daily report shortly after midnight")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 15 0 * * ?")
                        .inTimeZone(timeZone)
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }
}
