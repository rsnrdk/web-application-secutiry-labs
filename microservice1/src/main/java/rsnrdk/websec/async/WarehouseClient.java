package rsnrdk.websec.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Slow side effect #3: asking an external warehouse service how many units are in stock.
 */
@Slf4j
@Service
public class WarehouseClient {

    private final long latencyMs;

    public WarehouseClient(@Value("${external.latency-ms:1000}") long latencyMs) {
        this.latencyMs = latencyMs;
    }

    @Async
    public CompletableFuture<Integer> fetchStock(Long itemId) {
        SimulatedLatency.pause(latencyMs);
        int stock = (int) (itemId * 7 % 50);
        log.info("Stock {} fetched for item {}", stock, itemId);
        return CompletableFuture.completedFuture(stock);
    }
}
