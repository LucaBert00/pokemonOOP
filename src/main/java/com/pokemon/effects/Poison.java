package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;

/** Applies the poison status condition to the target. */

public class Poison implements IEffect
{
    private final ITarget target;

    public Poison(ITarget target)
    {
        this.target = target;
    }

    @Override
    public void apply(Battle battle)
    {
        Battler t = target.resolve(battle);
        if (!t.isPoisoned())
        {
            t.setPoisoned(true);
            System.out.println("  " + t.getName() + " was poisoned!");
        }
        else
        {
            System.out.println("  " + t.getName() + " is already poisoned!");
        }
    }
}