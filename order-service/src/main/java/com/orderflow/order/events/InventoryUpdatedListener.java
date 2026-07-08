package com.orderflow.order.events;

import com.orderflow.events.InventoryUpdatedEvent;
import com.orderflow.events.KafkaTopics;
import com.orderflow.order.service.OrderService;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
public class InventoryUpdatedListener {

    private final OrderService orderService;

    public InventoryUpdatedListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_UPDATED, groupId = "order-service")
    public void onInventoryUpdated(InventoryUpdatedEvent event) {
        orderService.applyInventoryUpdate(event);
    }
}
