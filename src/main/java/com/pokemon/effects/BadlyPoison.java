package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;

/** Applies the badly poisoned (Toxic) status condition to the target. */

public class BadlyPoison implements IEffect
{
    private final ITarget target;

    public BadlyPoison(ITarget target)
    {
        this.target = target;
    }

    @Override
    public void apply(Battle battle)
    {
        Battler t = target.resolve(battle);
        if (!t.isBadlyPoisoned())
        {
            t.setBadlyPoisoned(true);
            System.out.println("  " + t.getName() + " was badly poisoned!");
        }
        else
        {
            System.out.println("  " + t.getName() + " is already badly poisoned!");
        }
    }
}