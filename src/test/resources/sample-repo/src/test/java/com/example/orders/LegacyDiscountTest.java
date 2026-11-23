package com.example.orders;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

public class LegacyDiscountTest {
    @Test
    public void appliesNoDiscountToSmallOrders() {
        assertEquals(100, new Discounts().apply("customer-1", 100));
    }

    @Ignore
    public void appliesLoyaltyDiscount() {
        assertEquals(950, new Discounts().apply("loyal-customer", 1000));
    }

    // @Test
    public void appliesStaffDiscount() {
        assertEquals(800, new Discounts().apply("staff-1", 1000));
    }

    public void testAppliesSeasonalDiscount() {
        assertEquals(900, new Discounts().apply("customer-1", 1000));
    }
}
