package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.INumber;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;
import com.pokemon.battle.Element;
import com.pokemon.data.TypeChart;

/**
 * Standard Pokémon damage formula (simplified):
 *   damage = (2*level/5 + 2) * power * attack / defense / 50 + 2
 * then multiplied by STAB (1.5x if move type matches attacker type)
 * and type chart effectiveness, and a random factor (85-100%).
 *
 * Always attacker → defender.
 */

public class FormulaDamage implements IEffect
{
    private final INumber power;

    public FormulaDamage(INumber power)
    {
        this.power = power;
    }

    @Override
    public void apply(Battle battle)
    {
        Battler atk = battle.getAttacker();
        Battler def = battle.getDefender();

        double p          = power.evaluate(battle);
        double level      = atk.getLevel();
        double attackStat = atk.getAttack();
        double defenseStat= def.getDefense();

        // Base formula
        double damage = ((2.0 * level / 5.0 + 2.0) * p * attackStat / defenseStat / 50.0) + 2.0;

        // STAB (Same-Type Attack Bonus)
        Element moveElement = battle.getCurrentMoveElement();
        if (moveElement != null && atk.hasElement(moveElement))
        {
            damage *= 1.5;
        }

        // Type chart effectiveness
        double effectiveness = TypeChart.getEffectiveness(moveElement, def.getElements());
        if (effectiveness == 0.0)
        {
            System.out.println("  It has no effect on " + def.getName() + "...");
            battle.setLastDamageDealt(0);
            return;
        }
        damage *= effectiveness;
        if (effectiveness >= 2.0)
        {
            System.out.println("  It's super effective!");
        }
        else if (effectiveness <= 0.5)
        {
            System.out.println("  It's not very effective...");
        }

        // Random factor 85–100%
        damage *= (0.85 + Math.random() * 0.15);

        int finalDamage = (int) Math.max(1, damage);
        def.takeDamage(finalDamage);
        battle.setLastDamageDealt(finalDamage);

        System.out.printf("  %s dealt %d damage to %s!%n", atk.getName(), finalDamage, def.getName());
    }
}
