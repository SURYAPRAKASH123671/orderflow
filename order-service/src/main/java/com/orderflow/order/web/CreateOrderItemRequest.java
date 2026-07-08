package com.orderflow.order.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderItemRequest(
        @NotNull Long productId,
        @Min(1) int quantity
) {
}
