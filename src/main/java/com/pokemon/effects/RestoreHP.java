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
        Battler t    = target.resolve(battle);
        int     heal = (int) Math.max(1, amount.evaluate(battle));
        t.restoreHp(heal);
        System.out.printf("  %s recovered %d HP!%n", t.getName(), heal);
    }
}
