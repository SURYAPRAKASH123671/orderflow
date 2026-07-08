package com.orderflow.order.events;

import com.orderflow.events.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("!local")
public class OrderKafkaTopicsConfig {

    @Bean
    NewTopic ordersPlacedTopic() {
        return TopicBuilder.name(KafkaTopics.ORDERS_PLACED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic inventoryUpdatedTopic() {
        return TopicBuilder.name(KafkaTopics.INVENTORY_UPDATED).partitions(3).replicas(1).build();
    }
}
