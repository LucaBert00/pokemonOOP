package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Weather;
import java.util.Objects;

/**
 * Sets the active {@link Weather} on the battle and prints its announcement.
 */
public class SetWeather implements IEffect
{
    private final Weather weather;

    public SetWeather(Weather weather)
    {
        this.weather = Objects.requireNonNull(weather, "weather must not be null");
    }

    @Override
    public void apply(Battle battle)
    {
        battle.setWeather(weather);
        System.out.println("  " + weather.announcement());
    }
}
