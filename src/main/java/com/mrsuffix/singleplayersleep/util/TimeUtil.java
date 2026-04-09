package com.mrsuffix.singleplayersleep.util;

import org.bukkit.World;

public final class TimeUtil {

    public static final long SUNSET_TICKS = 12541L;
    public static final long SUNRISE_TICKS = 23458L;
    public static final long DAY_LENGTH = 24000L;

    private TimeUtil() {
    }

    public static boolean isNight(World world) {
        if (world == null) return false;
        long t = world.getTime();
        return t >= SUNSET_TICKS && t <= SUNRISE_TICKS;
    }
}
