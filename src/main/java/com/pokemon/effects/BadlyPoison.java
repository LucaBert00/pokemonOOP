package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;
import com.pokemon.battle.MajorStatus;

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
        Battler targetBattler = target.resolve(battle);
        if (!targetBattler.applyMajorStatus(MajorStatus.BADLY_POISONED))
        {
            System.out.println("  " + targetBattler.getName() + " is already affected by a major status!");
            return;
        }
        System.out.println("  " + targetBattler.getName() + " was badly poisoned!");
    }
}
