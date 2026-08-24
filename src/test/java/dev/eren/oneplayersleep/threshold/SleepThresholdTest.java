package dev.eren.oneplayersleep.threshold;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SleepThresholdTest {
    @Test
    void absoluteThresholdIsClampedToEligiblePopulation() {
        SleepThreshold threshold = SleepThreshold.parse(5, PercentageRounding.CEIL);

        assertEquals(1, threshold.requiredPlayers(1));
        assertEquals(3, threshold.requiredPlayers(3));
        assertEquals(5, threshold.requiredPlayers(8));
        assertTrue(threshold.isClamped(3));
    }

    @Test
    void percentageDefaultsToSafeCeiling() {
        SleepThreshold threshold = SleepThreshold.parse("50%", PercentageRounding.CEIL);

        assertEquals(2, threshold.requiredPlayers(3));
        assertEquals(2, threshold.requiredPlayers(4));
    }

    @Test
    void acceptsPrefixPercentageNotation() {
        SleepThreshold threshold = SleepThreshold.parse("%30", PercentageRounding.CEIL);
        assertEquals("30%", threshold.asConfigValue());
        assertEquals(1, threshold.requiredPlayers(3));
    }

    @Test
    void differentRoundingPoliciesAreApplied() {
        assertEquals(1, SleepThreshold.parse("50%", PercentageRounding.FLOOR).requiredPlayers(3));
        assertEquals(2, SleepThreshold.parse("50%", PercentageRounding.NEAREST).requiredPlayers(3));
    }

    @Test
    void rejectsUnsafeValues() {
        assertThrows(IllegalArgumentException.class, () -> SleepThreshold.parse(0, PercentageRounding.CEIL));
        assertThrows(IllegalArgumentException.class, () -> SleepThreshold.parse("101%", PercentageRounding.CEIL));
        assertThrows(IllegalArgumentException.class, () -> SleepThreshold.parse("1.5", PercentageRounding.CEIL));
    }
}
