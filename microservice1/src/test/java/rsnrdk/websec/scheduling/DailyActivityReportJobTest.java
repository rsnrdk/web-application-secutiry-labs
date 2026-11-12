package rsnrdk.websec.scheduling;

import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DailyActivityReportJobTest {

    private final ActivityReportService reportService = mock(ActivityReportService.class);
    private final DailyActivityReportJob job = new DailyActivityReportJob(reportService);
    private final QuartzJobsConfig config = new QuartzJobsConfig("Europe/Kyiv");

    private JobExecutionContext contextWith(Map<String, Object> data, int refireCount) {
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap(data));
        when(context.getJobDetail()).thenReturn(config.dailyActivityReportJobDetail());
        when(context.getRefireCount()).thenReturn(refireCount);
        return context;
    }

    private JobExecutionContext defaultContext(int refireCount) {
        return contextWith(config.dailyActivityReportJobDetail().getJobDataMap().getWrappedMap(), refireCount);
    }

    @Test
    void execute_readsDefaults_andMakesOneServiceCall() throws Exception {
        job.execute(defaultContext(0));

        verify(reportService).generateDailyReport(1);
        verifyNoMoreInteractions(reportService);
    }

    @Test
    void execute_firstAttemptFails_asksQuartzToRefire() {
        when(reportService.generateDailyReport(1)).thenThrow(new IllegalStateException("db timeout"));

        JobExecutionException ex = assertThrows(JobExecutionException.class, () -> job.execute(defaultContext(0)));

        assertTrue(ex.refireImmediately());
    }

    @Test
    void execute_lastAllowedAttemptFails_stopsRetrying() {
        // maxRetries = 2 -> attempts with refireCount 0, 1, 2; the third failure is final
        when(reportService.generateDailyReport(1)).thenThrow(new IllegalStateException("db timeout"));

        JobExecutionException ex = assertThrows(JobExecutionException.class, () -> job.execute(defaultContext(2)));

        assertFalse(ex.refireImmediately());
    }

    @Test
    void execute_wrongParameterType_isNotRetried_andServiceIsNotCalled() {
        JobExecutionContext context = contextWith(Map.of(
                DailyActivityReportJob.DAYS_BACK, 1,
                DailyActivityReportJob.MAX_RETRIES, "two"), 0);

        JobExecutionException ex = assertThrows(JobExecutionException.class, () -> job.execute(context));

        assertInstanceOf(JobParameterException.class, ex.getCause());
        assertFalse(ex.refireImmediately(), "retrying a configuration error is pointless");
        verifyNoInteractions(reportService);
    }
}
