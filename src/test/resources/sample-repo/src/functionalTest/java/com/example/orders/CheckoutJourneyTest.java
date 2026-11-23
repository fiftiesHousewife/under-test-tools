package com.example.orders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CheckoutJourneyTest {
    @Test
    public void checksOutABasketOfTwoItems() {
        Basket basket = new Basket();
        basket.add("item-1", 2);
        assertEquals(Basket.Status.PAID, basket.checkOut());
    }

    @Test
    public void refusesAnEmptyBasket() {
        assertEquals(Basket.Status.REFUSED, new Basket().checkOut());
    }
}
