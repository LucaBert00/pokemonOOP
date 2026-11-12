package com.pokemon.numbers;

import com.pokemon.api.INumber;
import com.pokemon.battle.Battle;

/** Returns a.evaluate(battle) + b.evaluate(battle). */

public class Sum implements INumber
{
    private final INumber a, b;

    public Sum(INumber a, INumber b)
    {
        this.a = a; this.b = b;
    }

    @Override
    public double evaluate(Battle battle)
    {
        return a.evaluate(battle) + b.evaluate(battle);
    }
}
