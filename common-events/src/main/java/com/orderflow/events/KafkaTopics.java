package com.orderflow.events;

public final class KafkaTopics {

    public static final String ORDERS_PLACED = "orders.placed";
    public static final String INVENTORY_UPDATED = "inventory.updated";
    public static final String INVENTORY_LOW_STOCK = "inventory.low-stock";

    private KafkaTopics() {
    }
}
