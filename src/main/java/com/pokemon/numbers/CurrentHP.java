package com.pokemon.numbers;

import com.pokemon.api.INumber;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;

/** The current (live) HP of the specified target. */

public class CurrentHP implements INumber
{
    private final ITarget target;

    public CurrentHP(ITarget target)
    {
        this.target = target;
    }

    @Override
    public double evaluate(Battle battle)
    {
        return target.resolve(battle).getCurrentHp();
    }
}
