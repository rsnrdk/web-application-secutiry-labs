package rsnrdk.websec.scheduling;

/**
 * A job parameter is missing or has a wrong type: a configuration error, retrying will not help.
 */
public class JobParameterException extends IllegalArgumentException {

    public JobParameterException(String message) {
        super(message);
    }
}
