package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;

/** Applies the paralysis status condition to the target. */

public class Paralysis implements IEffect
{
    private final ITarget target;

    public Paralysis(ITarget target)
    {
        this.target = target;
    }

    @Override
    public void apply(Battle battle)
    {
        Battler t = target.resolve(battle);
        if (!t.isParalyzed())
        {
            t.setParalyzed(true);
            System.out.println("  " + t.getName() + " is paralyzed! It may be unable to move!");
        }
        else
        {
            System.out.println("  " + t.getName() + " is already paralyzed!");
        }
    }
}
