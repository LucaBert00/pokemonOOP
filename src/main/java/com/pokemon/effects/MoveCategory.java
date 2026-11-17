package com.pokemon.effects;

import com.pokemon.battle.Battler;

/**
 * Classifies whether a formula-based damaging move scales off physical
 * (Attack / Defense) or special (Sp. Attack / Sp. Defense) combat stats.
 */
public enum MoveCategory {
    PHYSICAL {
        @Override
        public int attackStat(Battler attacker) {
            return attacker.getAttack();
        }

        @Override
        public int defenseStat(Battler defender) {
            return defender.getDefense();
        }
    },
    SPECIAL {
        @Override
        public int attackStat(Battler attacker) {
            return attacker.getSpAttack();
        }

        @Override
        public int defenseStat(Battler defender) {
            return defender.getSpDefense();
        }
    };

    public abstract int attackStat(Battler attacker);

    public abstract int defenseStat(Battler defender);
}
