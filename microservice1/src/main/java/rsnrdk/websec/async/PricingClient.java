package rsnrdk.websec.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import rsnrdk.websec.dto.ItemDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CompletableFuture;

/**
 * Slow side effect #2: asking an external pricing service for the current market price.
 */
@Slf4j
@Service
public class PricingClient {

    private static final BigDecimal MARKET_MARKUP = new BigDecimal("1.05");

    private final long latencyMs;

    public PricingClient(@Value("${external.latency-ms:1000}") long latencyMs) {
        this.latencyMs = latencyMs;
    }

    @Async
    public CompletableFuture<BigDecimal> fetchMarketPrice(ItemDto item) {
        SimulatedLatency.pause(latencyMs);
        BigDecimal base = item.getPrice() != null ? item.getPrice() : new BigDecimal("100.00");
        BigDecimal marketPrice = base.multiply(MARKET_MARKUP).setScale(2, RoundingMode.HALF_UP);
        log.info("Market price {} fetched for item {}", marketPrice, item.getId());
        return CompletableFuture.completedFuture(marketPrice);
    }
}
