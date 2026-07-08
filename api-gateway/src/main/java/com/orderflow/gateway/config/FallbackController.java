package com.orderflow.gateway.config;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallbackController {

    @GetMapping("/fallback/orders")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> ordersFallback() {
        return Map.of(
                "service", "order-service",
                "message", "Order service is temporarily unavailable",
                "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/fallback/inventory")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> inventoryFallback() {
        return Map.of(
                "service", "inventory-service",
                "message", "Inventory service is temporarily unavailable",
                "timestamp", Instant.now().toString()
        );
    }
}
