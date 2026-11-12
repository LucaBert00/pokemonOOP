package com.pokemon.numbers;

import com.pokemon.api.INumber;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;

/** The maximum HP of the specified target. */
public class MaxHP implements INumber
{
    private final ITarget target;

    public MaxHP(ITarget target)
    {
        this.target = target;
    }

    @Override
    public double evaluate(Battle battle)
    {
        return target.resolve(battle).getMaxHp();
    }
}
