package com.mrsuffix.singleplayersleep.managers;

import com.mrsuffix.singleplayersleep.core.SleepRule;

import java.util.List;

public record WorldSettings(boolean enabled,
                            double sleepPercentage,
                            List<SleepRule> dynamicRules,
                            WeatherSettings weatherSettings) {

    public record WeatherSettings(boolean changeWeather,
                                  boolean clearRain,
                                  boolean clearThunder) {
    }
}
