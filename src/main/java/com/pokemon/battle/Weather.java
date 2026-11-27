package com.pokemon.battle;

import java.util.Objects;

/**
 * Battle-wide weather condition.
 * Each constant carries its own announcement text so {@code SetWeather} stays branch-free.
 */
public enum Weather
{
    NONE("The weather returned to normal!"),
    SUN("The sunlight turned harsh!"),
    RAIN("It started to rain!"),
    SANDSTORM("A sandstorm kicked up!"),
    HAIL("It started to hail!");

    private final String announcement;

    Weather(String announcement)
    {
        this.announcement = Objects.requireNonNull(announcement, "announcement must not be null");
    }

    public String announcement()
    {
        return announcement;
    }
}
