package rsnrdk.websec.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Limits how many times a client (by IP address) may call the method within a time window.
 * Exceeding the limit results in HTTP 429 Too Many Requests. Handled by {@link RateLimitAspect}.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    int maxCalls();

    long windowSeconds();
}