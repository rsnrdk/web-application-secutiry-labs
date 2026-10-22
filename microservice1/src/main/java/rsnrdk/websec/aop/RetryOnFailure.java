package rsnrdk.websec.aop;

import org.springframework.dao.TransientDataAccessException;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Re-invokes the method when it fails with a transient (temporary) error.
 * Use only on idempotent operations, e.g. reads. Handled by {@link RetryAspect}.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RetryOnFailure {

    int maxAttempts() default 3;

    Class<? extends Throwable>[] retryOn() default {TransientDataAccessException.class};
}