package com.pokemon.conditions;

import com.pokemon.api.ICondition;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Weather;
import java.util.Objects;

/**
 * Battle-level condition that holds when {@code battle.getWeather() == weather}.
 */
public class HasWeather implements ICondition<Battle>
{
    private final Weather weather;

    public HasWeather(Weather weather)
    {
        this.weather = Objects.requireNonNull(weather, "weather must not be null");
    }

    @Override
    public boolean check(Battle battle)
    {
        return battle.getWeather() == weather;
    }
}
