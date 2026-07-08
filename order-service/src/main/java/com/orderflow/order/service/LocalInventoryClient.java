package com.orderflow.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "order.events.local", havingValue = "true")
public class LocalInventoryClient {

    private final RestClient restClient;

    public LocalInventoryClient(
            RestClient.Builder restClientBuilder,
            @Value("${inventory-service.base-url:http://localhost:8082}") String inventoryBaseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(inventoryBaseUrl).build();
    }

    public void decrementStock(Long productId, int quantity) {
        restClient.patch()
                .uri("/api/inventory/{productId}/decrement", productId)
                .body(new DecrementStockRequest(quantity))
                .retrieve()
                .toBodilessEntity();
    }

    private record DecrementStockRequest(int quantity) {
    }
}
