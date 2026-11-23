package com.example.orders;

import org.junit.Test;

public class OrderTotalsTest {
    private final OrderService orders = new OrderService(new InMemoryOrders());

    @Test
    public void addsUpAPlacedOrder() {
        Order order = orders.place("customer-1", "item-1", 2);
        OrderChecks.checkTotals(order, 1200);
    }

    @Test
    public void describesAPlacedOrder() {
        Order order = orders.place("customer-1", "item-1", 2);
        OrderChecks.describe(order);
    }
}
