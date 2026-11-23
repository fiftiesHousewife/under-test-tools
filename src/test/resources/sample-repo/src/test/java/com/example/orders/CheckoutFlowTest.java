package com.example.orders;

import static org.junit.Assert.assertEquals;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CheckoutFlowTest {
    private static Basket basket = new Basket();

    @Test
    public void step1_addsAnItem() {
        basket.add("item-1");
        assertEquals(1, basket.size());
    }

    @Test
    public void step2_pays() {
        assertEquals(Basket.Status.PAID, basket.pay());
    }

    @Test
    public void step3_ships() {
        assertEquals(Basket.Status.SHIPPED, basket.ship());
    }
}
