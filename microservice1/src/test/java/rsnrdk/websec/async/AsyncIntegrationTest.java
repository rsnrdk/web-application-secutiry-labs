package rsnrdk.websec.async;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import rsnrdk.websec.config.AsyncConfig;
import rsnrdk.websec.entity.Item;
import rsnrdk.websec.repository.ItemRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Runs with the real Spring context: @Async only works through Spring proxies and the configured executor.
 */
@SpringBootTest(properties = {"external.latency-ms=500", "insights.timeout-ms=3000"})
@ExtendWith(OutputCaptureExtension.class)
class AsyncIntegrationTest {

    private static final long LATENCY_MS = 500;

    @Autowired
    private ThreadPoolTaskExecutor appTaskExecutor;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ItemInsightsService itemInsightsService;

    @Autowired
    private ItemRepository itemRepository;

    private static void waitUntil(BooleanSupplier condition, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("Condition not met within " + timeout);
            }
            Thread.sleep(20);
        }
    }

    private static boolean logLineContains(CapturedOutput output, String message, String text) {
        return output.getOut().lines().anyMatch(line -> line.contains(message) && line.contains(text));
    }

    @Test
    void executor_isExplicitlyConfiguredWithBoundedResources() {
        assertEquals(2, appTaskExecutor.getCorePoolSize());
        assertEquals(4, appTaskExecutor.getMaxPoolSize());
        assertEquals(20, appTaskExecutor.getQueueCapacity());
        assertEquals(AsyncConfig.THREAD_NAME_PREFIX, appTaskExecutor.getThreadNamePrefix());
    }

    @Test
    void sendWelcomeEmail_returnsImmediately_andFinishesLaterOnAppAsyncThread(CapturedOutput output) throws Exception {
        // Act
        long start = System.nanoTime();
        notificationService.sendWelcomeEmail("newbie");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Assert: the caller got control back long before the 500 ms "SMTP call" finished
        assertTrue(elapsedMs < LATENCY_MS / 2, "caller was blocked for " + elapsedMs + " ms");
        assertFalse(output.getOut().contains("Welcome email sent to newbie"));

        // ...and the email is sent later, on a thread of OUR executor
        waitUntil(() -> output.getOut().contains("Welcome email sent to newbie"), Duration.ofSeconds(5));
        assertTrue(logLineContains(output, "Welcome email sent to newbie", AsyncConfig.THREAD_NAME_PREFIX));
    }

    @Test
    void getInsights_runsBothExternalCallsConcurrently(CapturedOutput output) {
        // Arrange
        Long id = itemRepository.save(Item.builder().name("Laptop").price(new BigDecimal("1000.00")).build()).getId();

        // Act
        long start = System.nanoTime();
        ItemInsights insights = itemInsightsService.getInsights(id);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Assert: combined result is correct
        assertEquals(new BigDecimal("1050.00"), insights.marketPrice());
        assertEquals((int) (id * 7 % 50), insights.stock());
        assertTrue(insights.complete());
        // two 500 ms calls one after another would need >= 1000 ms; in parallel it is about 500 ms
        assertTrue(elapsedMs < 2 * LATENCY_MS - 100, "calls look sequential, took " + elapsedMs + " ms");
        // both ran on our executor, not on the caller thread
        assertTrue(logLineContains(output, "Market price", AsyncConfig.THREAD_NAME_PREFIX));
        assertTrue(logLineContains(output, "Stock", AsyncConfig.THREAD_NAME_PREFIX));
    }

    @Test
    void sendWelcomeEmail_failsInsideAsyncVoid_callerUnaffectedAndErrorIsLogged(CapturedOutput output) throws Exception {
        // Act: an empty recipient makes the async method throw, but the caller sees nothing
        notificationService.sendWelcomeEmail(" ");

        // Assert: AsyncUncaughtExceptionHandler recorded the failure
        waitUntil(() -> output.getOut().contains("Async method NotificationService.sendWelcomeEmail failed"),
                Duration.ofSeconds(5));
        assertTrue(output.getOut().contains("recipient is empty"));
    }
}
