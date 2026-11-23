package com.example.orders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:discount-rules-context.xml")
public class DiscountRulesContextTest {
    @Autowired
    private DiscountRules rules;

    @Test
    public void takesTenPercentOffLargeOrders() {
        assertEquals(900, rules.apply(1000));
    }
}
