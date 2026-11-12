package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.INumber;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;

/**
 * Reads battle.lastDamageDealt (set by FormulaDamage before this runs)
 * and heals the attacker by that amount × fraction.
 *
 * Usage: Sequence(FormulaDamage(power), Drain(Attacker(), Exactly(0.5)))
 */

public class Drain implements IEffect
{
    private final ITarget target;   // who gets healed (usually Attacker)
    private final INumber fraction; // what fraction of damage dealt to drain

    public Drain(ITarget target, INumber fraction)
    {
        this.target   = target;
        this.fraction = fraction;
    }

    @Override
    public void apply(Battle battle)
    {
        Battler healed = target.resolve(battle);
        int     heal   = (int) Math.max(1, battle.getLastDamageDealt() * fraction.evaluate(battle));
        healed.restoreHp(heal);
        System.out.printf("  %s absorbed %d HP!%n", healed.getName(), heal);
    }
}
