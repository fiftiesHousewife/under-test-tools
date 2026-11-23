package com.example.orders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class OrderLookupTest {
    private final OrderService orders = new OrderService(new InMemoryOrders());

    @Test
    public void findsNoOrderForAStranger() {
        assertNull(orders.latestFor("stranger"));
    }

    @Test
    public void looksUpBothWays() {
        testLookupByCustomer();
        testLookupByNumber();
    }

    public void testLookupByCustomer() {
        assertEquals(1, orders.allFor("customer-1").size());
    }

    public void testLookupByNumber() {
        assertEquals("customer-1", orders.find(1).getCustomer());
    }

    @Test
    public void cancelsWithoutError() {
        Throwable result = null;
        try {
            orders.cancel(1);
        } catch (Throwable t) {
            result = t;
        }
        assertNull(result);
    }
}
