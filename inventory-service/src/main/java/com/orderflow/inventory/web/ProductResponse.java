package com.orderflow.inventory.web;

import com.orderflow.inventory.domain.Product;
import java.math.BigDecimal;

public record ProductResponse(Long id, String name, int stock, BigDecimal price) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getStock(), product.getPrice());
    }
}
