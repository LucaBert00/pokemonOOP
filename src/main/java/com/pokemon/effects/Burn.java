package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;

/** Applies the burn status condition to the target. */

public class Burn implements IEffect
{
    private final ITarget target;

    public Burn(ITarget target)
    {
        this.target = target;
    }

    @Override
    public void apply(Battle battle)
    {
        Battler t = target.resolve(battle);
        if (!t.isBurned())
        {
            t.setBurned(true);
            System.out.println("  " + t.getName() + " was burned!");
        }
        else
        {
            System.out.println("  " + t.getName() + " is already burned!");
        }
    }
}