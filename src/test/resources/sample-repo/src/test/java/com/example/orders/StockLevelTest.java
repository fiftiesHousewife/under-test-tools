package com.example.orders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StockLevelTest {
    @Test
    public void reservesStock() {
    }

    @Test
    public void releasesStock() {
        StockLevel stock = new StockLevel("item-1", 10);
        stock.reserve(3);
        stock.release(3);
        assertEquals(10, stock.available());
    }
}
