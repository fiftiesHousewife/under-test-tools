package com.example.orders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class OrderApiTest {
    @Autowired
    private OrderService orders;

    @Test
    public void placesAnOrder() {
        assertEquals("customer-1", orders.place("customer-1", "item-1", 1).getCustomer());
    }
}
