package com.example.orders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:shipping-test-context.xml")
public class ShippingClientTest {
    @Autowired
    private ShippingClient shipping;

    @Test
    public void quotesNextDayDelivery() {
        assertEquals(495, shipping.quote("item-1", "next-day"));
    }
}
