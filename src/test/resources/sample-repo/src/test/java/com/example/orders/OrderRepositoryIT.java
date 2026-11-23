package com.example.orders;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.Test;

public class OrderRepositoryIT {
    private static final String URL = "jdbc:oracle:thin:@orders-uat.internal:1521/ORD";

    @Test
    public void findsAnOrder() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "orders", System.getenv("UAT_PASSWORD"))) {
            assertEquals("customer-1", new OrderRepository(connection).find(1).getCustomer());
        }
    }

    @Test
    public void countsOrders() throws Exception {
        try (Connection connection = DriverManager.getConnection(URL, "orders", System.getenv("UAT_PASSWORD"))) {
            assertEquals(42, new OrderRepository(connection).count());
        }
    }
}
