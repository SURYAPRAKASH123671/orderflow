package com.orderflow.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.orderflow.events.InventoryUpdatedEvent;
import com.orderflow.events.KafkaTopics;
import com.orderflow.events.OrderPlacedEvent;
import com.orderflow.order.domain.CustomerOrder;
import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.repository.CustomerOrderRepository;
import com.orderflow.order.web.CreateOrderItemRequest;
import com.orderflow.order.web.CreateOrderRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;

class OrderServiceTests {

    private final CustomerOrderRepository orderRepository = org.mockito.Mockito.mock(CustomerOrderRepository.class);
    private final KafkaTemplate<String, Object> kafkaTemplate = org.mockito.Mockito.mock(KafkaTemplate.class);
    private final ObjectProvider<LocalInventoryClient> localInventoryClient = org.mockito.Mockito.mock(ObjectProvider.class);
    private final OrderService orderService = new OrderService(orderRepository, kafkaTemplate, localInventoryClient, false);

    @Test
    void placeOrderSavesPendingOrderAndPublishesOrderPlacedEvent() {
        CreateOrderRequest request = new CreateOrderRequest(
                "customer@example.com",
                List.of(new CreateOrderItemRequest(1L, 2))
        );
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerOrder order = orderService.placeOrder(request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.ORDERS_PLACED), any(), eventCaptor.capture());
        assertThat(eventCaptor.getValue().customerEmail()).isEqualTo("customer@example.com");
        assertThat(eventCaptor.getValue().items()).hasSize(1);
    }

    @Test
    void applyInventoryUpdateConfirmsOrderWhenInventorySucceeds() {
        CustomerOrder order = new CustomerOrder("customer@example.com");
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        orderService.applyInventoryUpdate(new InventoryUpdatedEvent(10L, true, "ok", List.of(), Instant.now()));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
}
