package com.example.orders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:reports-context.xml")
public class ReportClientTest {
    @Autowired
    private ReportClient reports;

    @Test
    public void fetchesTheDailyReport() {
        assertEquals(1, reports.daily().size());
    }
}
