package com.example.orders;

import static org.junit.Assert.assertEquals;

final class OrderChecks {
    private OrderChecks() {
    }

    static void checkTotals(Order order, int expectedGross) {
        assertEquals(expectedGross, order.gross());
        assertEquals(order.net() + order.tax(), order.gross());
    }

    static String describe(Order order) {
        return order.getCustomer() + ": " + order.gross();
    }
}
