package com.pokemon.numbers;

import com.pokemon.api.INumber;
import com.pokemon.battle.Battle;

/** A constant number. Exactly(40) always returns 40.0. */

public class Exactly implements INumber
{
    private final double value;

    public Exactly(double value)
    {
        this.value = value;
    }

    @Override
    public double evaluate(Battle battle)
    {
        return value;
    }
}
