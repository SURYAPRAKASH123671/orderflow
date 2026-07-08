package com.orderflow.events;

import java.time.Instant;
import java.util.List;

public record InventoryUpdatedEvent(
        Long orderId,
        boolean successful,
        String reason,
        List<ProductStockEvent> products,
        Instant updatedAt
) {
}
