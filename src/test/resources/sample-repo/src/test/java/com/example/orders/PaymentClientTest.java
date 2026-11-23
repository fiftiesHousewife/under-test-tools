package com.example.orders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PaymentClientTest {
    private final PaymentClient client = new PaymentClient("http://localhost:8089");

    @Test
    public void chargesTheCard() {
        assertEquals(PaymentClient.Result.OK, client.charge("card-1", 1000));
    }
}
