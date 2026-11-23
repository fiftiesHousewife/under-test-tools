package com.example.orders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TaxTableTest {
    @Test
    public void chargesUkVatAtTwentyPercent() {
        assertEquals(200, new TaxTable("GB").taxOn(1000));
    }

    @Test
    public void chargesIrishVatAtTwentyThreePercent() {
        assertEquals(230, new TaxTable("IE").taxOn(1000));
    }
}
