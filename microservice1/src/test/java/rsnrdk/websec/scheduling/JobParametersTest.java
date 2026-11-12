package rsnrdk.websec.scheduling;

import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobParametersTest {

    @Test
    void requireInt_valueStoredAsIntegerOrString_isRead() {
        JobDataMap data = new JobDataMap(Map.of("a", 30, "b", "45"));

        assertEquals(30, JobParameters.requireInt(data, "a"));
        assertEquals(45, JobParameters.requireInt(data, "b"));
    }

    @Test
    void requireInt_keyWithTypo_failsWithClearMessage() {
        JobDataMap data = new JobDataMap(Map.of("retentionDay", 30));

        JobParameterException ex = assertThrows(JobParameterException.class,
                () -> JobParameters.requireInt(data, "retentionDays"));

        assertTrue(ex.getMessage().contains("Missing required job parameter 'retentionDays'"));
        assertTrue(ex.getMessage().contains("retentionDay"), "message should list the keys that do exist");
    }

    @Test
    void requireInt_notANumber_failsInsteadOfBecomingZero() {
        JobDataMap data = new JobDataMap(Map.of("retentionDays", "thirty"));

        JobParameterException ex = assertThrows(JobParameterException.class,
                () -> JobParameters.requireInt(data, "retentionDays"));

        assertEquals("Job parameter 'retentionDays' must be an integer but was 'thirty'", ex.getMessage());
    }

    @Test
    void requireBoolean_acceptsOnlyRealBooleans() {
        JobDataMap data = new JobDataMap(Map.of("a", true, "b", "false", "c", "yes"));

        assertTrue(JobParameters.requireBoolean(data, "a"));
        assertFalse(JobParameters.requireBoolean(data, "b"));
        // Boolean.parseBoolean("yes") would silently give false
        assertThrows(JobParameterException.class, () -> JobParameters.requireBoolean(data, "c"));
    }
}
