package rsnrdk.websec.async;

import java.math.BigDecimal;

/**
 * Item enriched with data from two external services. A null field means that source was unavailable.
 */
public record ItemInsights(
        Long itemId,
        String name,
        BigDecimal marketPrice,
        Integer stock,
        boolean complete
) {
}
