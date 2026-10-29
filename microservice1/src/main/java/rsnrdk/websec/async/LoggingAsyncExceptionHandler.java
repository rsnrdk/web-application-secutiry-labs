package rsnrdk.websec.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Exceptions from @Async void methods have no caller to propagate to.
 * Without this handler they would only be visible as a vague stack trace, so we log them explicitly.
 */
@Slf4j
public class LoggingAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("Async method {}.{} failed with args {}: {}",
                method.getDeclaringClass().getSimpleName(),
                method.getName(),
                Arrays.toString(params),
                ex.getMessage(),
                ex);
    }
}
