package dev.eren.oneplayersleep.sleep;

final class NightTimeMath {
    private static final long DAY_TICKS = 24000L;

    private NightTimeMath() {
    }

    /**
     * Returns only the world time that will really be skipped, rather than assuming a full night.
     */
    static long ticksUntil(long currentTime, long targetTime) {
        long difference = Math.floorMod(targetTime - currentTime, DAY_TICKS);
        return difference == 0L ? DAY_TICKS : difference;
    }
}
