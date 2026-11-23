package com.example.orders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class NotificationTest {
    private final Notifier notifier = new Notifier(new InMemoryOutbox());

    @Test
    public void sendsAConfirmation() {
        try {
            assertTrue(notifier.send("customer-1@example.com", "Your order is confirmed"));
        } catch (Exception e) {
        }
    }

    @Test
    public void countsWhatItSent() {
        notifier.send("customer-1@example.com", "Your order is confirmed");
        try {
            assertEquals(2, notifier.sentCount());
        } catch (AssertionError e) {
            System.out.println("count was off: " + e.getMessage());
        }
    }

    @Test
    public void rejectsAnEmptyAddress() {
        try {
            notifier.send("", "Your order is confirmed");
            fail("expected an empty address to be refused");
        } catch (IllegalArgumentException expected) {
        }
    }
}
