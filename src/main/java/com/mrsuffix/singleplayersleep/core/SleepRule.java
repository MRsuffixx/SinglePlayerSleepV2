package com.mrsuffix.singleplayersleep.core;

import java.util.Optional;

public record SleepRule(int minPlayers, int maxPlayers, double percentage) {

    public boolean matches(long playerCount) {
        return playerCount >= minPlayers && playerCount <= maxPlayers;
    }

    public static Optional<SleepRule> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.replace(" ", "");
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        String[] parts = trimmed.split(":");
        if (parts.length != 2) {
            return Optional.empty();
        }
        String rangePart = parts[0];
        String percentPart = parts[1];
        try {
            double percentage = Double.parseDouble(percentPart);
            if (percentage <= 0) {
                return Optional.empty();
            }
            int min;
            int max;
            if (rangePart.endsWith("+")) {
                String minPart = rangePart.substring(0, rangePart.length() - 1);
                min = parseInt(minPart);
                max = Integer.MAX_VALUE;
            } else if (rangePart.contains("-")) {
                String[] rangeParts = rangePart.split("-", 2);
                min = parseInt(rangeParts[0]);
                if (rangeParts[1].isEmpty() || rangeParts[1].equals("+")) {
                    max = Integer.MAX_VALUE;
                } else {
                    max = parseInt(rangeParts[1]);
                }
            } else {
                min = parseInt(rangePart);
                max = min;
            }
            if (min <= 0 || max < min) {
                return Optional.empty();
            }
            return Optional.of(new SleepRule(min, max, percentage));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static int parseInt(String value) {
        return Integer.parseInt(value.replaceAll("[^0-9]", ""));
    }
}
