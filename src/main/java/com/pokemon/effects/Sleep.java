package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;
import com.pokemon.battle.MajorStatus;

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
        Battler targetBattler = target.resolve(battle);
        if (!targetBattler.applyMajorStatus(MajorStatus.ASLEEP))
        {
            System.out.println("  " + targetBattler.getName() + " is already affected by a major status!");
            return;
        }
        int turns = battle.getRandom().nextInt(3) + 1;
        targetBattler.setSleepTurnsRemaining(turns);
        System.out.println("  " + targetBattler.getName() + " fell asleep!");
    }
}
