package com.pokemon.numbers;

import com.pokemon.api.INumber;
import com.pokemon.battle.Battle;

/** Uniform random value between min and max (inclusive). */

public class Between implements INumber
{
    private final double min, max;

    public Between(double min, double max)
    {
        this.min = min;
        this.max = max;
    }

    @Override
    public double evaluate(Battle battle)
    {
        return min + Math.random() * (max - min);
    }
}
