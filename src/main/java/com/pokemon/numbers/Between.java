package com.pokemon.numbers;

import com.pokemon.api.INumber;
import com.pokemon.battle.Battle;

/**
 * Uniform random value in the closed interval {@code [min, max]} (both bounds inclusive),
 * drawing randomness from {@link Battle#getRandom()}.
 */
public class Between implements INumber
{
    private final double min, max;

    public Between(double min, double max)
    {
        if (min > max)
        {
            throw new IllegalArgumentException("min (" + min + ") must be <= max (" + max + ")");
        }
        this.min = min;
        this.max = max;
    }

    @Override
    public double evaluate(Battle battle)
    {
        double unit = (battle.getRandom().nextLong() >>> 11) / (double) ((1L << 53) - 1);
        return min + unit * (max - min);
    }
}