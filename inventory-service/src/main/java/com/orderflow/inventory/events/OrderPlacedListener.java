package com.orderflow.inventory.events;

import com.orderflow.events.KafkaTopics;
import com.orderflow.events.OrderPlacedEvent;
import com.orderflow.inventory.service.InventoryService;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
public class OrderPlacedListener {

    private final InventoryService inventoryService;

    public OrderPlacedListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = KafkaTopics.ORDERS_PLACED, groupId = "inventory-service")
    public void onOrderPlaced(OrderPlacedEvent event) {
        inventoryService.reserveStockForOrder(event);
    }
}
