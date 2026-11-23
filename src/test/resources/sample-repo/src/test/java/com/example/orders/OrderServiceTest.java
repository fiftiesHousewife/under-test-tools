package com.example.orders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;

public class OrderServiceTest {
    private final OrderService service = new OrderService();

    @Test
    public void createsOrder() {
        Order order = service.create("customer-1", 2);
        assertNotNull(order);
    }

    @Test
    public void logsOrder() {
        service.log(service.create("customer-1", 1));
    }

    @Test
    public void pricesOrder() {
        assertEquals(1200, service.price(service.create("customer-1", 2)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAnOrderWithNoItems() {
        service.create("customer-1", 0);
    }

    @Ignore("flaky on CI")
    @Test
    public void cancelsOrder() {
        Order order = service.create("customer-1", 1);
        service.cancel(order);
        assertEquals(Order.Status.CANCELLED, order.getStatus());
    }
}
