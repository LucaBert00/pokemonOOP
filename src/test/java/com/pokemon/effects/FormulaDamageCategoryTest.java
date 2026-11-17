package com.pokemon.effects;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.pokemon.api.IMove;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;
import com.pokemon.battle.Element;
import com.pokemon.data.MoveRegistry;
import com.pokemon.numbers.Exactly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormulaDamageCategoryTest {

    /**
     * Attacker with low physical Attack (20) and high Sp. Attack (160).
     */
    private static Battler specialAttacker() {
        return new Battler(
                "SpecAttacker",
                List.of(Element.NORMAL),
                500,
                50,
                20,
                50,
                160,
                50,
                60,
                List.of(MoveRegistry.get("Tackle"))
        );
    }

    /**
     * Defender with high physical Defense (160) and low Sp. Defense (20).
     */
    private static Battler physicalWallDefender() {
        return new Battler(
                "PhysWall",
                List.of(Element.NORMAL),
                500,
                50,
                50,
                160,
                50,
                20,
                40,
                List.of(MoveRegistry.get("Tackle"))
        );
    }

    @Test
    void physicalAndSpecialCategoriesRouteToDistinctStatPairs() {
        Battler attacker = specialAttacker();
        Battler physDefender = physicalWallDefender();
        Battler specDefender = physicalWallDefender();

        Battle physicalBattle = new Battle(attacker, physDefender, 42L);
        physicalBattle.setCurrentMoveElement(Element.ELECTRIC);
        new FormulaDamage(new Exactly(80), MoveCategory.PHYSICAL).apply(physicalBattle);

        Battle specialBattle = new Battle(attacker, specDefender, 42L);
        specialBattle.setCurrentMoveElement(Element.ELECTRIC);
        new FormulaDamage(new Exactly(80), MoveCategory.SPECIAL).apply(specialBattle);

        // Seed 42L first roll: randomFactor = 0.85 + 0.7275636800328681 * 0.15 = 0.959134552
        // Physical (Atk 20 / Def 160): (((22 * 80 * 20 / 160) / 50) + 2) * 0.959134552 = 6.138 -> 6
        // Special  (SpA 160 / SpD 20): (((22 * 80 * 160 / 20) / 50) + 2) * 0.959134552 = 272.01 -> 272
        int physicalDamage = 500 - physDefender.getCurrentHp();
        int specialDamage = 500 - specDefender.getCurrentHp();

        assertEquals(6, physicalDamage);
        assertEquals(272, specialDamage);
    }

    @Test
    void singleArgConstructorDefaultsToPhysicalCategory() {
        Battler attacker = specialAttacker();
        Battler defaultDefender = physicalWallDefender();
        Battler explicitPhysicalDefender = physicalWallDefender();

        Battle defaultBattle = new Battle(attacker, defaultDefender, 42L);
        defaultBattle.setCurrentMoveElement(Element.FIRE);
        new FormulaDamage(new Exactly(80)).apply(defaultBattle);

        Battle explicitBattle = new Battle(attacker, explicitPhysicalDefender, 42L);
        explicitBattle.setCurrentMoveElement(Element.FIRE);
        new FormulaDamage(new Exactly(80), MoveCategory.PHYSICAL).apply(explicitBattle);

        assertEquals(explicitPhysicalDefender.getCurrentHp(), defaultDefender.getCurrentHp());
    }

    @Test
    void moveRegistryRoutesRegisteredSpecialAndPhysicalMovesToExpectedStats() {
        List<String> specialMoves = List.of(
                "Ember",
                "Flamethrower",
                "WaterGun",
                "Surf",
                "Thunderbolt",
                "Thunder",
                "MegaDrain",
                "IceBeam",
                "SludgeBomb"
        );
        List<String> physicalMoves = List.of(
                "Tackle",
                "BodySlam",
                "TripleKick",
                "JumpKick",
                "FuryAttack",
                "VineWhip",
                "Scratch",
                "QuickAttack",
                "RockThrow",
                "KarateChop",
                "CrossChop"
        );

        // Seed 514L produces an initial roll of ~0.6851 (< 0.70), so every registered move hits.
        for (String key : specialMoves) {
            Battler attacker = specialAttacker();
            Battler defender = physicalWallDefender();
            IMove move = MoveRegistry.get(key);
            move.execute(new Battle(attacker, defender, 514L));

            int damage = 500 - defender.getCurrentHp();
            assertTrue(damage >= 120, key + " should route through SpAtk/SpDef (>= 120 dmg), got " + damage);
        }

        for (String key : physicalMoves) {
            Battler attacker = specialAttacker();
            Battler defender = physicalWallDefender();
            IMove move = MoveRegistry.get(key);
            move.execute(new Battle(attacker, defender, 514L));

            int damage = 500 - defender.getCurrentHp();
            assertTrue(damage > 0 && damage <= 15, key + " should route through Atk/Def (1..15 dmg), got " + damage);
        }
    }

    @Test
    void constructorRejectsNullCategory() {
        assertThrows(NullPointerException.class, () -> new FormulaDamage(new Exactly(40), null));
    }
}
