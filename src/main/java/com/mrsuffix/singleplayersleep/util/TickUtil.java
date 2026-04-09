package com.mrsuffix.singleplayersleep.util;

public final class TickUtil {
    
    public static final long TICKS_PER_SECOND = 20L;
    public static final long TICKS_PER_MINUTE = 1200L;
    public static final long TICKS_PER_5_MINUTES = 6000L;
    public static final long TICKS_PER_HOUR = 72000L;
    public static final long WORLD_SAVE_DELAY_TICKS = 5L;
    
    private TickUtil() {
    }
    
    public static long secondsToTicks(long seconds) {
        return seconds * TICKS_PER_SECOND;
    }
    
    public static long ticksToSeconds(long ticks) {
        return ticks / TICKS_PER_SECOND;
    }
}
