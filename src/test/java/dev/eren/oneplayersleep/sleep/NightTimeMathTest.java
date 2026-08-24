package dev.eren.oneplayersleep.sleep;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NightTimeMathTest {
    @Test
    void earlySleepAdvancesMostOfTheNight() {
        assertEquals(11000L, NightTimeMath.ticksUntil(13000L, 0L));
    }

    @Test
    void lateSleepAdvancesOnlyTheRemainingNight() {
        assertEquals(1000L, NightTimeMath.ticksUntil(23000L, 0L));
    }

    @Test
    void respectsConfiguredMorningTime() {
        assertEquals(2000L, NightTimeMath.ticksUntil(23000L, 1000L));
    }
}
