package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.INumber;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;

/**
 * Inflicts an exact amount of HP damage, bypassing the formula.
 * Ignores type matchups and stats.
 */

public class DirectDamage implements IEffect
{
    private final ITarget target;
    private final INumber amount;

    public DirectDamage(ITarget target, INumber amount)
    {
        this.target = target;
        this.amount = amount;
    }

    @Override
    public void apply(Battle battle)
    {
        Battler t = target.resolve(battle);
        int dmg   = (int) Math.max(1, amount.evaluate(battle));
        t.takeDamage(dmg);
        battle.setLastDamageDealt(dmg);
        System.out.printf("  %s took %d direct damage!%n", t.getName(), dmg);
    }
}
