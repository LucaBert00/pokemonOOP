package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;

/** Applies the freeze status condition to the target. */

public class Freeze implements IEffect
{
    private final ITarget target;

    public Freeze(ITarget target)
    {
        this.target = target;
    }

    @Override
    public void apply(Battle battle)
    {
        Battler t = target.resolve(battle);
        if (!t.isFrozen())
        {
            t.setFrozen(true);
            System.out.println("  " + t.getName() + " was frozen solid!");
        }
        else
        {
            System.out.println("  " + t.getName() + " is already frozen!");
        }
    }
}