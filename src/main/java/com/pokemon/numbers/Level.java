package com.pokemon.numbers;

import com.pokemon.api.INumber;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;

/** The level of the specified target. */

public class Level implements INumber
{
    private final ITarget target;

    public Level(ITarget target)
    {
        this.target = target;
    }

    @Override
    public double evaluate(Battle battle)
    {
        return target.resolve(battle).getLevel();
    }
}
