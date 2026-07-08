package com.orderflow.events;

public record ProductStockEvent(Long productId, int remainingStock) {
}
