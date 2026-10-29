package rsnrdk.websec.async;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class LoggingAsyncExceptionHandlerTest {

    @Test
    void handleUncaughtException_asyncVoidMethodFailed_logsMethodArgsAndReason(CapturedOutput output) throws Exception {
        // Arrange
        LoggingAsyncExceptionHandler handler = new LoggingAsyncExceptionHandler();
        Method method = NotificationService.class.getMethod("sendWelcomeEmail", String.class);
        IllegalArgumentException failure = new IllegalArgumentException("recipient is empty");

        // Act
        handler.handleUncaughtException(failure, method, " ");

        // Assert: the error is recorded, not swallowed
        assertTrue(output.getOut().contains("ERROR"));
        assertTrue(output.getOut().contains("Async method NotificationService.sendWelcomeEmail failed"));
        assertTrue(output.getOut().contains("recipient is empty"));
    }
}
