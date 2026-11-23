package com.example.orders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class InvoiceTest {
    @Test
    public void invoicesAUkCustomer() {
        Customer customer = new Customer("customer-1", "Orders Ltd", "1 High Street", "London");
        Order order = new Order(customer);
        order.add(new Line("item-1", 2, 500));
        order.add(new Line("item-2", 1, 250));
        Invoice invoice = new InvoiceWriter(new TaxTable("GB")).write(order);
        assertEquals("Orders Ltd", invoice.getCustomerName());
        assertEquals(3, invoice.getLines().size() + 1);
        assertEquals(1250, invoice.getNet());
        assertEquals(250, invoice.getTax());
        assertEquals(1500, invoice.getGross());
    }

    @Test
    public void invoicesAnIrishCustomer() {
        Customer customer = new Customer("customer-1", "Orders Ltd", "1 High Street", "London");
        Order order = new Order(customer);
        order.add(new Line("item-1", 2, 500));
        order.add(new Line("item-2", 1, 250));
        Invoice invoice = new InvoiceWriter(new TaxTable("IE")).write(order);
        assertEquals("Orders Ltd", invoice.getCustomerName());
        assertEquals(3, invoice.getLines().size() + 1);
        assertEquals(1250, invoice.getNet());
        assertEquals(287, invoice.getTax());
        assertEquals(1537, invoice.getGross());
    }

    @Test
    public void numbersInvoicesInOrder() {
        assertEquals("INV-2", new InvoiceNumbers("INV-", 1).next());
    }
}
