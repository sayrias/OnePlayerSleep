package dev.eren.oneplayersleep.sleep;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeSkipReasonCompatTest {
    @Test
    void readsLegacySkipReasonAccessor() {
        assertTrue(TimeSkipReasonCompat.isNightSkip(new LegacyEvent(LegacyReason.NIGHT_SKIP)));
        assertFalse(TimeSkipReasonCompat.isNightSkip(new LegacyEvent(LegacyReason.COMMAND)));
    }

    @Test
    void readsReasonAccessorFallback() {
        assertTrue(TimeSkipReasonCompat.isNightSkip(new AlternateEvent(AlternateReason.NIGHT_SKIP)));
    }

    @Test
    void unknownEventsFailOpenWithoutBlockingTimeChanges() {
        assertFalse(TimeSkipReasonCompat.isNightSkip(new Object()));
        assertFalse(TimeSkipReasonCompat.isNightSkip(null));
    }

    public enum LegacyReason {
        NIGHT_SKIP,
        COMMAND
    }

    public enum AlternateReason {
        NIGHT_SKIP
    }

    public static final class LegacyEvent {
        private final LegacyReason reason;

        LegacyEvent(LegacyReason reason) {
            this.reason = reason;
        }

        public LegacyReason getSkipReason() {
            return reason;
        }
    }

    public static final class AlternateEvent {
        private final AlternateReason reason;

        AlternateEvent(AlternateReason reason) {
            this.reason = reason;
        }

        public AlternateReason getReason() {
            return reason;
        }
    }
}
