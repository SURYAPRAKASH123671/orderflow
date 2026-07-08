package com.orderflow.inventory.events;

import com.orderflow.events.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("!local")
public class InventoryKafkaTopicsConfig {

    @Bean
    NewTopic inventoryLowStockTopic() {
        return TopicBuilder.name(KafkaTopics.INVENTORY_LOW_STOCK).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic ordersPlacedDlqTopic() {
        return TopicBuilder.name(KafkaTopics.ORDERS_PLACED + ".DLQ").partitions(3).replicas(1).build();
    }
}
