package com.orderflow.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orderflow.events.InventoryUpdatedEvent;
import com.orderflow.events.KafkaTopics;
import com.orderflow.events.OrderItemEvent;
import com.orderflow.events.OrderPlacedEvent;
import com.orderflow.inventory.domain.Product;
import com.orderflow.inventory.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.core.KafkaTemplate;

class InventoryServiceTests {

    private final ProductRepository productRepository = org.mockito.Mockito.mock(ProductRepository.class);
    private final KafkaTemplate<String, Object> kafkaTemplate = org.mockito.Mockito.mock(KafkaTemplate.class);
    private final CacheManager cacheManager = org.mockito.Mockito.mock(CacheManager.class);
    private final InventoryService inventoryService = new InventoryService(
            productRepository,
            kafkaTemplate,
            cacheManager,
            5,
            true
    );

    @Test
    void reserveStockForOrderDecrementsStockAndPublishesSuccessEvent() {
        Product product = new Product("Keyboard", 10, BigDecimal.valueOf(2499));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        OrderPlacedEvent event = new OrderPlacedEvent(
                99L,
                "customer@example.com",
                List.of(new OrderItemEvent(1L, 3)),
                Instant.now()
        );

        inventoryService.reserveStockForOrder(event);

        assertThat(product.getStock()).isEqualTo(7);
        ArgumentCaptor<InventoryUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(InventoryUpdatedEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.INVENTORY_UPDATED), eq("99"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().successful()).isTrue();
        assertThat(eventCaptor.getValue().products()).hasSize(1);
    }

    @Test
    void reserveStockForOrderPublishesFailureWhenStockIsInsufficient() {
        Product product = new Product("Keyboard", 1, BigDecimal.valueOf(2499));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        OrderPlacedEvent event = new OrderPlacedEvent(
                100L,
                "customer@example.com",
                List.of(new OrderItemEvent(1L, 3)),
                Instant.now()
        );

        inventoryService.reserveStockForOrder(event);

        assertThat(product.getStock()).isEqualTo(1);
        ArgumentCaptor<InventoryUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(InventoryUpdatedEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.INVENTORY_UPDATED), eq("100"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().successful()).isFalse();
    }
}
