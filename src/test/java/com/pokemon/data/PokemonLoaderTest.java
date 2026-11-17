package com.pokemon.data;

import com.pokemon.api.IMove;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PokemonLoaderTest {

    private static Battler findByName(List<Battler> roster, String name) {
        return roster.stream()
                .filter(b -> b.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Species not found in pokemon.csv: " + name));
    }

    @Test
    void pikachuHasExpectedLevel50ScaledStats() {
        Battler pikachu = findByName(PokemonLoader.loadAll(), "Pikachu");

        assertEquals(50, pikachu.getLevel());
        assertEquals(95, pikachu.getMaxHp());
        assertEquals(95, pikachu.getCurrentHp());
        assertEquals(60, pikachu.getAttack());
        assertEquals(45, pikachu.getDefense());
        assertEquals(55, pikachu.getSpAttack());
        assertEquals(55, pikachu.getSpDefense());
        assertEquals(95, pikachu.getSpeed());
    }

    @Test
    void squirtleHasExpectedLevel50ScaledStats() {
        Battler squirtle = findByName(PokemonLoader.loadAll(), "Squirtle");

        assertEquals(50, squirtle.getLevel());
        assertEquals(104, squirtle.getMaxHp());
        assertEquals(104, squirtle.getCurrentHp());
        assertEquals(53, squirtle.getAttack());
        assertEquals(70, squirtle.getDefense());
        assertEquals(55, squirtle.getSpAttack());
        assertEquals(69, squirtle.getSpDefense());
        assertEquals(48, squirtle.getSpeed());
    }

    @Test
    void allLoadedSpeciesInitializeCurrentHpEqualToScaledMaxHp() {
        List<Battler> roster = PokemonLoader.loadAll();
        assertFalse(roster.isEmpty(), "Roster loaded from pokemon.csv should not be empty");

        for (Battler battler : roster) {
            assertEquals(50, battler.getLevel());
            assertTrue(battler.getMaxHp() > 60,
                    () -> battler.getName() + " maxHp should be scaled above base + 60, got " + battler.getMaxHp());
            assertEquals(battler.getMaxHp(), battler.getCurrentHp(),
                    () -> battler.getName() + " currentHp should equal maxHp on load");
        }
    }

    @Test
    void neutralTackleDoesNotOneHitKoEqualLevelOpponent() {
        List<Battler> roster = PokemonLoader.loadAll();
        Battler pikachu = findByName(roster, "Pikachu");
        Battler squirtle = findByName(roster, "Squirtle");

        Battle battle = new Battle(pikachu, squirtle, 42L);
        IMove tackle = MoveRegistry.get("Tackle");
        tackle.execute(battle);

        assertFalse(squirtle.isFainted(), "Scaled Level-50 Squirtle should survive a single neutral Tackle");
        assertTrue(squirtle.getCurrentHp() > 0, "Squirtle should retain positive HP after one Tackle");
    }
}
