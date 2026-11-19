package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.INumber;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;

/** Heals the target by the specified amount. */

public class RestoreHP implements IEffect
{
    private final ITarget target;
    private final INumber amount;

    public RestoreHP(ITarget target, INumber amount)
    {
        this.target = target;
        this.amount = amount;
    }

    @Override
    public void apply(Battle battle)
    {
        Battler beneficiary = target.resolve(battle);
        int requested = (int) amount.evaluate(battle);
        int missingHp = beneficiary.getMaxHp() - beneficiary.getCurrentHp();
        int actual = Math.min(missingHp, requested);
        if (actual <= 0)
        {
            System.out.println("  " + beneficiary.getName() + "'s HP is already full!");
            return;
        }
        beneficiary.restoreHp(actual);
        System.out.println("  " + beneficiary.getName() + " recovered " + actual + " HP!");
    }
}
