package com.orderflow.events;

import java.time.Instant;
import java.util.List;

public record OrderPlacedEvent(
        Long orderId,
        String customerEmail,
        List<OrderItemEvent> items,
        Instant placedAt
) {
}
