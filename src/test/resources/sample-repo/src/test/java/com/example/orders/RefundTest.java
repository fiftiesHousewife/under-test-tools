package com.example.orders;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class RefundTest {
    private final RefundService refunds = new RefundService();

    @Test
    public void refundsInFull() {
        assertEquals(1000, refunds.refund(1000, 1.0));
    }

    @Test
    public void refundsPartly() {
        assertEquals(333, refunds.refund(1000, 0.333));
    }

    @Test
    public void refusesARefundAboveThePrice() {
        assertEquals(0, refunds.refund(1000, 1.5));
    }
}
