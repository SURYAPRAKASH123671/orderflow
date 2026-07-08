package com.orderflow.inventory.service;

import com.orderflow.inventory.domain.Product;
import com.orderflow.inventory.repository.ProductRepository;
import com.orderflow.events.InventoryLowStockEvent;
import com.orderflow.events.InventoryUpdatedEvent;
import com.orderflow.events.KafkaTopics;
import com.orderflow.events.OrderPlacedEvent;
import com.orderflow.events.ProductStockEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CacheManager cacheManager;
    private final int lowStockThreshold;
    private final boolean publishEvents;

    public InventoryService(
            ProductRepository productRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            CacheManager cacheManager,
            @Value("${inventory.low-stock-threshold:5}") int lowStockThreshold,
            @Value("${inventory.events.publish:true}") boolean publishEvents
    ) {
        this.productRepository = productRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.cacheManager = cacheManager;
        this.lowStockThreshold = lowStockThreshold;
        this.publishEvents = publishEvents;
    }

    @Cacheable(cacheNames = "products", key = "#p0")
    @Transactional(readOnly = true)
    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }

    @CacheEvict(cacheNames = "products", key = "#p0")
    @Transactional
    public Product decrementStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        product.decrement(quantity);
        publishLowStockIfNeeded(product);
        return product;
    }

    @Transactional
    public void reserveStockForOrder(OrderPlacedEvent event) {
        List<ProductStockEvent> updatedProducts = new ArrayList<>();

        try {
            for (var item : event.items()) {
                Product product = productRepository.findById(item.productId())
                        .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.productId()));
                if (!product.hasStock(item.quantity())) {
                    throw new IllegalStateException("Insufficient stock for product: " + item.productId());
                }
            }
            for (var item : event.items()) {
                Product product = decrementStock(item.productId(), item.quantity());
                updatedProducts.add(new ProductStockEvent(product.getId(), product.getStock()));
                evictProductCache(product.getId());
            }
            publishInventoryUpdated(event.orderId(), true, "Stock reserved", updatedProducts);
        } catch (RuntimeException ex) {
            publishInventoryUpdated(event.orderId(), false, ex.getMessage(), updatedProducts);
        }
    }

    private void publishInventoryUpdated(
            Long orderId,
            boolean successful,
            String reason,
            List<ProductStockEvent> products
    ) {
        if (!publishEvents) {
            return;
        }
        InventoryUpdatedEvent event = new InventoryUpdatedEvent(
                orderId,
                successful,
                reason,
                products,
                Instant.now()
        );
        kafkaTemplate.send(KafkaTopics.INVENTORY_UPDATED, String.valueOf(orderId), event);
    }

    private void publishLowStockIfNeeded(Product product) {
        if (!publishEvents) {
            return;
        }
        if (product.getStock() <= lowStockThreshold) {
            InventoryLowStockEvent event = new InventoryLowStockEvent(
                    product.getId(),
                    product.getName(),
                    product.getStock(),
                    lowStockThreshold,
                    Instant.now()
            );
            kafkaTemplate.send(KafkaTopics.INVENTORY_LOW_STOCK, String.valueOf(product.getId()), event);
        }
    }

    private void evictProductCache(Long productId) {
        var cache = cacheManager.getCache("products");
        if (cache != null) {
            cache.evict(productId);
        }
    }
}
