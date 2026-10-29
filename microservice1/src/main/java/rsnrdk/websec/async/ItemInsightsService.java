package rsnrdk.websec.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rsnrdk.websec.dto.ItemDto;
import rsnrdk.websec.service.ItemService;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ItemInsightsService {

    private final ItemService itemService;
    private final PricingClient pricingClient;
    private final WarehouseClient warehouseClient;
    private final long timeoutMs;

    public ItemInsightsService(
            ItemService itemService,
            PricingClient pricingClient,
            WarehouseClient warehouseClient,
            @Value("${insights.timeout-ms:2000}") long timeoutMs
    ) {
        this.itemService = itemService;
        this.pricingClient = pricingClient;
        this.warehouseClient = warehouseClient;
        this.timeoutMs = timeoutMs;
    }

    public ItemInsights getInsights(Long itemId) {
        ItemDto item = itemService.getItem(itemId);

        // both calls start right away and run in parallel on the app-async executor
        CompletableFuture<BigDecimal> price = pricingClient.fetchMarketPrice(item)
                .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    log.warn("Market price unavailable for item {}: {}", itemId, describe(ex));
                    return null;
                });

        CompletableFuture<Integer> stock = warehouseClient.fetchStock(itemId)
                .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .handle((value, ex) -> {
                    if (ex != null) {
                        log.warn("Stock unavailable for item {}: {}", itemId, describe(ex));
                        return null;
                    }
                    return value;
                });

        // join() cannot hang: each source is already bounded by orTimeout and never fails
        return price.thenCombine(stock, (p, s) ->
                new ItemInsights(item.getId(), item.getName(), p, s, p != null && s != null)
        ).join();
    }

    private static String describe(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        return cause.getClass().getSimpleName() + (cause.getMessage() != null ? ": " + cause.getMessage() : "");
    }
}
