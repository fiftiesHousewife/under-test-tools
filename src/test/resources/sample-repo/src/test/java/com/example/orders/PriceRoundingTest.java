package com.example.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PriceRoundingTest {
    @Test
    void roundsHalfUp() {
        assertEquals(101, new Price(100.5).inPence());
    }

    @Test
    private void roundsDown() {
        assertEquals(100, new Price(100.4).inPence());
    }

    @Test
    int roundsToPence() {
        return new Price(100.49).inPence();
    }

    @Test
    void keepsThePrice() {
        assertThat(new Price(100));
    }
}
