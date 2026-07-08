package com.orderflow.events;

public record OrderItemEvent(Long productId, int quantity) {
}
