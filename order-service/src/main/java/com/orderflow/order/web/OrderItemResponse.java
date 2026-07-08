package com.orderflow.order.web;

import com.orderflow.order.domain.OrderItem;

public record OrderItemResponse(Long id, Long productId, int quantity) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.getId(), item.getProductId(), item.getQuantity());
    }
}
