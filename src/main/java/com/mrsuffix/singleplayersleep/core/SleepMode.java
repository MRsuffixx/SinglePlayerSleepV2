package com.mrsuffix.singleplayersleep.core;

import java.util.Locale;
import java.util.logging.Logger;

public enum SleepMode {
    SINGLE,
    PERCENTAGE;

    public static SleepMode fromConfig(String value, Logger logger) {
        if (value == null) {
            if (logger != null) {
                logger.warning("sleep.mode is missing; defaulting to 'single'.");
            }
            return SINGLE;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "single":
                return SINGLE;
            case "percentage":
                return PERCENTAGE;
            default:
                if (logger != null) {
                    logger.warning("Unknown sleep.mode '" + value + "'; defaulting to 'single'.");
                }
                return SINGLE;
        }
    }

    public boolean isSingle() {
        return this == SINGLE;
    }

    public boolean isPercentage() {
        return this == PERCENTAGE;
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
