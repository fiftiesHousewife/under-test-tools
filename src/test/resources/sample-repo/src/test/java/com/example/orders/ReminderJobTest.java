package com.example.orders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReminderJobTest {
    private final ReminderJob job = new ReminderJob();

    @Test
    public void sendsAReminderAfterADay() throws Exception {
        job.start();
        Thread.sleep(5000);
        assertEquals(1, job.sent());
    }

    @Test
    public void stopsAfterThreeReminders() throws Exception {
        job.start();
        Thread.sleep(15000);
        assertEquals(3, job.sent());
    }

    @Test
    public void startsWithNoReminders() {
        assertEquals(0, job.sent());
    }
}
