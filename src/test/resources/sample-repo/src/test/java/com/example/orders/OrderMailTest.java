package com.example.orders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:orders")
@ActiveProfiles("test")
public class OrderMailTest {
    @Autowired
    private OrderService orders;

    @Test
    public void mailsTheConfirmation() {
        assertEquals(1, orders.confirm("customer-1").sentCount());
    }
}
