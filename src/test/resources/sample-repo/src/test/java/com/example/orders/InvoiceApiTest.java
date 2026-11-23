package com.example.orders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class InvoiceApiTest extends AbstractIntegrationTest {
    @Autowired
    private InvoiceService invoices;

    @Test
    public void invoicesAnOrder() {
        assertEquals("INV-1", invoices.invoice(1L).getNumber());
    }
}
