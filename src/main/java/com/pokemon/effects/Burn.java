package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;
import com.pokemon.battle.MajorStatus;

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
        Battler targetBattler = target.resolve(battle);
        if (!targetBattler.applyMajorStatus(MajorStatus.BURNED))
        {
            System.out.println("  " + targetBattler.getName() + " is already affected by a major status!");
            return;
        }
        System.out.println("  " + targetBattler.getName() + " was burned!");
    }
}
