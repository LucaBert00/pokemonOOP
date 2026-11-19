package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;
import com.pokemon.battle.MajorStatus;

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
        Battler targetBattler = target.resolve(battle);
        if (!targetBattler.applyMajorStatus(MajorStatus.FROZEN))
        {
            System.out.println("  " + targetBattler.getName() + " is already affected by a major status!");
            return;
        }
        System.out.println("  " + targetBattler.getName() + " was frozen solid!");
    }
}
