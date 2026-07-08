package com.orderflow.events;

import java.time.Instant;

public record InventoryLowStockEvent(
        Long productId,
        String productName,
        int remainingStock,
        int threshold,
        Instant detectedAt
) {
}
