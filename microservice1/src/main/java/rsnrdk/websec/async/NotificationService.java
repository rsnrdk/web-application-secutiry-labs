package rsnrdk.websec.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Slow side effect #1: sending a welcome email (simulated SMTP call).
 */
@Slf4j
@Service
public class NotificationService {

    private final long latencyMs;

    public NotificationService(@Value("${external.latency-ms:1000}") long latencyMs) {
        this.latencyMs = latencyMs;
    }

    @Async
    public void sendWelcomeEmail(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Cannot send welcome email: recipient is empty");
        }
        SimulatedLatency.pause(latencyMs);
        log.info("Welcome email sent to {}", username);
    }
}
