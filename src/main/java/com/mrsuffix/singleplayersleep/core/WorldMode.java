package com.mrsuffix.singleplayersleep.core;

import java.util.Locale;
import java.util.logging.Logger;

public enum WorldMode {
    WHITELIST,
    BLACKLIST;

    public static WorldMode fromConfig(String value, Logger logger) {
        if (value == null) {
            if (logger != null) {
                logger.warning("worlds.mode is missing; defaulting to 'whitelist'.");
            }
            return WHITELIST;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "whitelist":
                return WHITELIST;
            case "blacklist":
                return BLACKLIST;
            default:
                if (logger != null) {
                    logger.warning("Unknown worlds.mode '" + value + "'; defaulting to 'whitelist'.");
                }
                return WHITELIST;
        }
    }

    public boolean isWhitelist() {
        return this == WHITELIST;
    }

    public boolean isBlacklist() {
        return this == BLACKLIST;
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
