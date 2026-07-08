package com.orderflow.notification.events;

import com.orderflow.events.InventoryLowStockEvent;
import com.orderflow.events.InventoryUpdatedEvent;
import com.orderflow.events.KafkaTopics;
import com.orderflow.events.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventListeners {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListeners.class);

    @KafkaListener(topics = KafkaTopics.ORDERS_PLACED, groupId = "notification-service-orders")
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Order received: orderId={}, customerEmail={}, items={}",
                event.orderId(), event.customerEmail(), event.items().size());
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_UPDATED, groupId = "notification-service-inventory")
    public void onInventoryUpdated(InventoryUpdatedEvent event) {
        if (event.successful()) {
            log.info("Order confirmed notification prepared: orderId={}", event.orderId());
        } else {
            log.warn("Order failed notification prepared: orderId={}, reason={}",
                    event.orderId(), event.reason());
        }
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_LOW_STOCK, groupId = "notification-service-low-stock")
    public void onLowStock(InventoryLowStockEvent event) {
        log.warn("Low-stock alert: productId={}, productName={}, remainingStock={}, threshold={}",
                event.productId(), event.productName(), event.remainingStock(), event.threshold());
    }
}
