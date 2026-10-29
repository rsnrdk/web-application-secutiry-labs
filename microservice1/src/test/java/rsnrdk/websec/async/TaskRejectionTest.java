package rsnrdk.websec.async;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import rsnrdk.websec.controller.ItemInsightsController;
import rsnrdk.websec.controller.RestExceptionHandler;
import rsnrdk.websec.dto.ItemDto;
import rsnrdk.websec.service.ItemService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskRejectionTest {

    private ThreadPoolTaskExecutor tinyExecutor;
    private final CountDownLatch release = new CountDownLatch(1);

    @BeforeEach
    void setUp() {
        // deliberately tiny: 1 thread + 1 queued task = capacity of 2
        tinyExecutor = new ThreadPoolTaskExecutor();
        tinyExecutor.setCorePoolSize(1);
        tinyExecutor.setMaxPoolSize(1);
        tinyExecutor.setQueueCapacity(1);
        tinyExecutor.setThreadNamePrefix("tiny-");
        tinyExecutor.initialize();
    }

    @AfterEach
    void tearDown() {
        release.countDown();
        tinyExecutor.shutdown();
    }

    private void fillExecutor() {
        Runnable blocker = () -> {
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        tinyExecutor.execute(blocker); // occupies the only thread
        tinyExecutor.execute(blocker); // occupies the only queue slot
    }

    @Test
    void execute_moreTasksThanPoolAndQueue_throwsTaskRejectedException() {
        // Arrange
        fillExecutor();

        // Act + Assert
        assertThrows(TaskRejectedException.class, () -> tinyExecutor.execute(() -> { }));
    }

    @Test
    void insightsEndpoint_executorFull_returns503WithRetryAfterInsteadOf500() throws Exception {
        // Arrange: clients submit their work to the saturated executor, exactly like @Async would
        fillExecutor();
        ItemService itemService = mock(ItemService.class);
        when(itemService.getItem(1L)).thenReturn(ItemDto.builder().id(1L).name("Laptop").build());
        PricingClient pricingClient = mock(PricingClient.class);
        when(pricingClient.fetchMarketPrice(any()))
                .thenAnswer(inv -> CompletableFuture.supplyAsync(() -> null, tinyExecutor));
        WarehouseClient warehouseClient = mock(WarehouseClient.class);
        when(warehouseClient.fetchStock(anyLong()))
                .thenAnswer(inv -> CompletableFuture.supplyAsync(() -> 0, tinyExecutor));

        ItemInsightsService insightsService =
                new ItemInsightsService(itemService, pricingClient, warehouseClient, 1_000);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ItemInsightsController(insightsService))
                .setControllerAdvice(new RestExceptionHandler())
                .build();

        // Act + Assert: explicit decision = clear "try again later", not a random 500
        mockMvc.perform(get("/api/v1/items/1/insights"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "5"))
                .andExpect(jsonPath("$.detail").value("Server is busy, please try again later"));
    }
}
