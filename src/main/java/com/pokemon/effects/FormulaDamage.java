package com.pokemon.effects;

import java.util.Objects;

import com.pokemon.api.IEffect;
import com.pokemon.api.INumber;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;
import com.pokemon.battle.Element;
import com.pokemon.data.TypeChart;

public class FormulaDamage implements IEffect {
    private final INumber power;
    private final MoveCategory category;

    public FormulaDamage(INumber power) {
        this(power, MoveCategory.PHYSICAL);
    }

    public FormulaDamage(INumber power, MoveCategory category) {
        this.power = Objects.requireNonNull(power, "power must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
    }

    public MoveCategory getCategory() {
        return category;
    }

    @Override
    public void apply(Battle battle) {
        Battler attacker = battle.getAttacker();
        Battler defender = battle.getDefender();

        double basePower = power.evaluate(battle);
        int level = attacker.getLevel();
        int attackStat = category.attackStat(attacker);
        int defenseStat = category.defenseStat(defender);

        double damage = (((2.0 * level / 5.0 + 2.0) * basePower * attackStat / defenseStat) / 50.0) + 2.0;

        Element moveElement = battle.getCurrentMoveElement();
        if (moveElement != null) {
            if (attacker.hasElement(moveElement)) {
                damage *= 1.5;
            }
            double effectiveness = TypeChart.getEffectiveness(moveElement, defender.getElements());
            if (effectiveness == 0.0) {
                System.out.println("  It has no effect on " + defender.getName() + "!");
                battle.setLastDamageDealt(0);
                return;
            }
            if (effectiveness > 1.0) {
                System.out.println("  It's super effective!");
            } else if (effectiveness < 1.0) {
                System.out.println("  It's not very effective...");
            }
            damage *= effectiveness;
        }

        double randomFactor = 0.85 + battle.nextDouble() * 0.15;
        damage *= randomFactor;

        int finalDamage = (int) Math.max(1.0, damage);
        defender.takeDamage(finalDamage);
        battle.setLastDamageDealt(finalDamage);
        System.out.printf("  %s dealt %d damage to %s!%n", attacker.getName(), finalDamage, defender.getName());
    }
}
