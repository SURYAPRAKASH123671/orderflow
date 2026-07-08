package com.orderflow.order.service;

import com.orderflow.order.domain.CustomerOrder;
import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.repository.CustomerOrderRepository;
import com.orderflow.order.web.CreateOrderRequest;
import com.orderflow.events.InventoryUpdatedEvent;
import com.orderflow.events.KafkaTopics;
import com.orderflow.events.OrderItemEvent;
import com.orderflow.events.OrderPlacedEvent;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.kafka.core.KafkaTemplate;

@Service
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectProvider<LocalInventoryClient> localInventoryClient;
    private final boolean localEvents;

    public OrderService(
            CustomerOrderRepository orderRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectProvider<LocalInventoryClient> localInventoryClient,
            @Value("${order.events.local:false}") boolean localEvents
    ) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.localInventoryClient = localInventoryClient;
        this.localEvents = localEvents;
    }

    @Transactional
    public CustomerOrder placeOrder(CreateOrderRequest request) {
        CustomerOrder order = new CustomerOrder(request.customerEmail());
        request.items().forEach(item -> order.addItem(item.productId(), item.quantity()));
        CustomerOrder savedOrder = orderRepository.save(order);

        if (localEvents) {
            try {
                LocalInventoryClient inventoryClient = localInventoryClient.getObject();
                request.items().forEach(item -> inventoryClient.decrementStock(item.productId(), item.quantity()));
                savedOrder.confirm();
            } catch (RuntimeException ex) {
                savedOrder.fail();
            }
            return savedOrder;
        }

        OrderPlacedEvent event = new OrderPlacedEvent(
                savedOrder.getId(),
                savedOrder.getCustomerEmail(),
                request.items().stream()
                        .map(item -> new OrderItemEvent(item.productId(), item.quantity()))
                        .toList(),
                Instant.now()
        );
        kafkaTemplate.send(KafkaTopics.ORDERS_PLACED, String.valueOf(savedOrder.getId()), event);

        return savedOrder;
    }

    @Transactional(readOnly = true)
    public CustomerOrder getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    @Transactional
    public void applyInventoryUpdate(InventoryUpdatedEvent event) {
        CustomerOrder order = getOrder(event.orderId());
        if (event.successful()) {
            order.confirm();
        } else {
            order.fail();
        }
    }
}
