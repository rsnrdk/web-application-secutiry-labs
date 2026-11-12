package rsnrdk.websec.scheduling;

import org.quartz.JobDataMap;

/**
 * Strict reading of JobDataMap values: a missing key or a wrong type fails loudly
 * instead of silently turning into 0 / false like JobDataMap.getIntValue / getBooleanValue would.
 */
public final class JobParameters {

    private JobParameters() {
    }

    public static int requireInt(JobDataMap data, String key) {
        Object value = require(data, key);
        if (value instanceof Integer number) {
            return number;
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException e) {
                throw wrongType(key, "an integer", value);
            }
        }
        throw wrongType(key, "an integer", value);
    }

    public static boolean requireBoolean(JobDataMap data, String key) {
        Object value = require(data, key);
        if (value instanceof Boolean flag) {
            return flag;
        }
        if ("true".equals(value) || "false".equals(value)) {
            return Boolean.parseBoolean((String) value);
        }
        throw wrongType(key, "a boolean (true/false)", value);
    }

    private static Object require(JobDataMap data, String key) {
        Object value = data.get(key);
        if (value == null) {
            throw new JobParameterException("Missing required job parameter '" + key + "', available keys: "
                    + String.join(", ", data.getKeys()));
        }
        return value;
    }

    private static JobParameterException wrongType(String key, String expected, Object value) {
        return new JobParameterException("Job parameter '" + key + "' must be " + expected + " but was '" + value + "'");
    }
}
