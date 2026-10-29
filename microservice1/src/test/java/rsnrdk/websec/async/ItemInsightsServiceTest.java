package rsnrdk.websec.async;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rsnrdk.websec.dto.ItemDto;
import rsnrdk.websec.service.ItemService;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemInsightsServiceTest {

    private static final long TIMEOUT_MS = 200;

    @Mock
    private ItemService itemService;

    @Mock
    private PricingClient pricingClient;

    @Mock
    private WarehouseClient warehouseClient;

    private ItemInsightsService service;
    private final ItemDto laptop = ItemDto.builder().id(1L).name("Laptop").price(new BigDecimal("1000.00")).build();

    @BeforeEach
    void setUp() {
        service = new ItemInsightsService(itemService, pricingClient, warehouseClient, TIMEOUT_MS);
        when(itemService.getItem(1L)).thenReturn(laptop);
    }

    @Test
    void getInsights_bothSourcesRespond_combinesResults() {
        // Arrange
        when(pricingClient.fetchMarketPrice(laptop)).thenReturn(CompletableFuture.completedFuture(new BigDecimal("1050.00")));
        when(warehouseClient.fetchStock(1L)).thenReturn(CompletableFuture.completedFuture(7));

        // Act
        ItemInsights insights = service.getInsights(1L);

        // Assert
        assertEquals("Laptop", insights.name());
        assertEquals(new BigDecimal("1050.00"), insights.marketPrice());
        assertEquals(7, insights.stock());
        assertTrue(insights.complete());
    }

    @Test
    void getInsights_pricingServiceFails_returnsStockWithoutPriceAndDoesNotThrow() {
        // Arrange
        when(pricingClient.fetchMarketPrice(laptop))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Pricing API is down")));
        when(warehouseClient.fetchStock(1L)).thenReturn(CompletableFuture.completedFuture(7));

        // Act
        ItemInsights insights = service.getInsights(1L);

        // Assert: failure is handled by exceptionally(), the caller still gets a result
        assertNull(insights.marketPrice());
        assertEquals(7, insights.stock());
        assertFalse(insights.complete());
    }

    @Test
    void getInsights_warehouseServiceFails_returnsPriceWithoutStock() {
        // Arrange
        when(pricingClient.fetchMarketPrice(laptop)).thenReturn(CompletableFuture.completedFuture(new BigDecimal("1050.00")));
        when(warehouseClient.fetchStock(1L))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Warehouse API is down")));

        // Act
        ItemInsights insights = service.getInsights(1L);

        // Assert: failure is handled by handle()
        assertEquals(new BigDecimal("1050.00"), insights.marketPrice());
        assertNull(insights.stock());
        assertFalse(insights.complete());
    }

    @Test
    void getInsights_sourceNeverResponds_stopsWaitingAfterTimeout() {
        // Arrange: a future that never completes simulates a hanging external service
        when(pricingClient.fetchMarketPrice(laptop)).thenReturn(new CompletableFuture<>());
        when(warehouseClient.fetchStock(1L)).thenReturn(CompletableFuture.completedFuture(7));

        // Act
        long start = System.nanoTime();
        ItemInsights insights = service.getInsights(1L);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Assert: we waited roughly the timeout, not forever
        assertTrue(elapsedMs >= TIMEOUT_MS, "should wait for the timeout, waited " + elapsedMs + " ms");
        assertTrue(elapsedMs < 2_000, "should stop waiting soon after the timeout, waited " + elapsedMs + " ms");
        assertNull(insights.marketPrice());
        assertEquals(7, insights.stock());
    }
}
