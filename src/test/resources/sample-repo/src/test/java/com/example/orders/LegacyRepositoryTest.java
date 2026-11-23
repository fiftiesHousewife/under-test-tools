package com.example.orders;

import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

public class LegacyRepositoryTest extends AbstractDependencyInjectionSpringContextTests {
    private OrderRepository repository;

    public void setRepository(OrderRepository repository) {
        this.repository = repository;
    }

    protected String[] getConfigLocations() {
        return new String[] {"classpath:legacy-context.xml"};
    }

    public void testFindsTheFirstOrder() {
        assertEquals("customer-1", repository.find(1).getCustomer());
    }
}
