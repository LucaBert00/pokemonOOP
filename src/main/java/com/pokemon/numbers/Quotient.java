package com.pokemon.numbers;

import com.pokemon.api.INumber;
import com.pokemon.battle.Battle;

/** Returns a.evaluate(battle) / b.evaluate(battle). */

public class Quotient implements INumber
{
    private final INumber a, b;

    public Quotient(INumber a, INumber b)
    {
        this.a = a; this.b = b;
    }

    @Override
    public double evaluate(Battle battle)
    {
        double denom = b.evaluate(battle);

        if (denom == 0)
        {
            return 0;
        }

        double numerator = a.evaluate(battle);
        return numerator / denom;
    }
}
