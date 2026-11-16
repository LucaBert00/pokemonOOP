package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.INumber;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;

/** Inflicts direct crash damage and reports the attacker's crash. */
public class CrashDamage implements IEffect
{
    private final ITarget target;
    private final INumber amount;

    public CrashDamage(ITarget target, INumber amount)
    {
        this.target = target;
        this.amount = amount;
    }

    @Override
    public void apply(Battle battle)
    {
        Battler t = target.resolve(battle);
        int dmg = (int) Math.max(1, amount.evaluate(battle));
        t.takeDamage(dmg);
        battle.setLastDamageDealt(dmg);
        System.out.printf("  %s kept going and crashed!%n", t.getName());
    }
}
