package com.example.orders;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ArchiveJobTest {
    private ArchiveJob job;

    @Before
    public void startTheContext() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("archive-context.xml");
        job = context.getBean(ArchiveJob.class);
    }

    @Test
    public void archivesOldOrders() {
        assertEquals(3, job.archiveOlderThan(30));
    }
}
