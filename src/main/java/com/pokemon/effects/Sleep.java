package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;

/** Applies the sleep status condition to the target. */

public class Sleep implements IEffect
{
    private final ITarget target;

    public Sleep(ITarget target)
    {
        this.target = target;
    }

    @Override
    public void apply(Battle battle)
    {
        Battler t = target.resolve(battle);
        if (!t.isAsleep())
        {
            t.setAsleep(true);
            System.out.println("  " + t.getName() + " fell asleep!");
        }
        else
        {
            System.out.println("  " + t.getName() + " is already asleep!");
        }
    }
}