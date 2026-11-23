package com.example.orders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class OrderStoreJpaTest {
    @Autowired
    private OrderStore store;

    @Test
    public void savesAnOrder() {
        assertEquals(1, store.save(new Order(new Customer("customer-1"))).getId());
    }
}
