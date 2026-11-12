package com.pokemon.conditions;

import com.pokemon.api.ICondition;
import com.pokemon.api.INumber;
import com.pokemon.battle.Battle;

/** ICondition<Battle>: true if number.evaluate(battle) >= threshold */

public class Geq implements ICondition<Battle>
{
    private final INumber number;
    private final double  threshold;

    public Geq(INumber number, double threshold)
    {
        this.number = number;
        this.threshold = threshold;
    }

    @Override
    public boolean check(Battle battle)
    {
        return number.evaluate(battle) >= threshold;
    }
}
