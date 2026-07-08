package com.orderflow.inventory.web;

import jakarta.validation.constraints.Min;

public record DecrementStockRequest(@Min(1) int quantity) {
}
