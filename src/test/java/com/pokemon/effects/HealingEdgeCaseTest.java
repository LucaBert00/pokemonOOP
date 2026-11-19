package com.pokemon.effects;

import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;
import com.pokemon.battle.Element;
import com.pokemon.numbers.Exactly;
import com.pokemon.targets.Attacker;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HealingEdgeCaseTest
{
    private static Battler battler(String name, int maxHp)
    {
        return new Battler(name, List.of(Element.NORMAL), maxHp, 50, 60, 60, 60, 60, 60, List.of());
    }

    private static String captureStdout(Runnable action)
    {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try
        {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            action.run();
        }
        finally
        {
            System.setOut(originalOut);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void zeroDamageDrainHealsZeroAndPrintsNothing()
    {
        Battler attacker = battler("Oddish", 100);
        Battler defender = battler("Gengar", 100);
        attacker.takeDamage(50); // 50/100 HP

        Battle battle = new Battle(attacker, defender, 42L);
        battle.setLastDamageDealt(0);

        String output = captureStdout(() -> new Drain(new Attacker(), new Exactly(0.5)).apply(battle));

        assertEquals(50, attacker.getCurrentHp(), "Zero-damage Drain must heal 0 HP");
        assertFalse(output.contains("absorbed"), "Zero-damage Drain must print no absorption message");
        assertEquals("", output.trim());
    }

    @Test
    void positiveDrainCapsAtMissingHpAndReportsActualGain()
    {
        Battler attacker = battler("Venusaur", 100);
        Battler defender = battler("Blastoise", 100);
        attacker.takeDamage(5); // 95/100 HP -> missing 5 HP

        Battle battle = new Battle(attacker, defender, 42L);
        battle.setLastDamageDealt(100); // 50% requested = 50 HP, capped to 5 HP

        String output = captureStdout(() -> new Drain(new Attacker(), new Exactly(0.5)).apply(battle));

        assertEquals(100, attacker.getCurrentHp(), "Drain must cap healing at missing HP");
        assertTrue(output.contains("Venusaur absorbed 5 HP!"), "Drain must report actual HP gained (5)");
        assertFalse(output.contains("50 HP"), "Drain must not report un-capped requested HP (50)");

        // Already at full HP: subsequent positive Drain heals 0 and prints nothing
        String fullOutput = captureStdout(() -> new Drain(new Attacker(), new Exactly(0.5)).apply(battle));
        assertEquals(100, attacker.getCurrentHp());
        assertEquals("", fullOutput.trim());
    }

    @Test
    void restoreHpAtFullHpStaysUnchangedAndPrintsAlreadyFullMessage()
    {
        Battler squirtle = battler("Squirtle", 100);
        Battler opponent = battler("Charmander", 100);
        Battle battle = new Battle(squirtle, opponent, 42L);

        String output = captureStdout(() -> new RestoreHP(new Attacker(), new Exactly(50)).apply(battle));

        assertEquals(100, squirtle.getCurrentHp(), "Full-HP battler must remain at max HP");
        assertTrue(output.contains("Squirtle's HP is already full!"), "Must print already-full message");
        assertFalse(output.contains("recovered"), "Must not print a fake recovery message");
    }

    @Test
    void restoreHpOnDamagedBattlerRestoresOnlyMissingAmountAndReportsActualGain()
    {
        Battler squirtle = battler("Squirtle", 100);
        Battler opponent = battler("Charmander", 100);
        squirtle.takeDamage(10); // 90/100 HP -> missing 10 HP
        Battle battle = new Battle(squirtle, opponent, 42L);

        String output = captureStdout(() -> new RestoreHP(new Attacker(), new Exactly(50)).apply(battle));

        assertEquals(100, squirtle.getCurrentHp(), "RestoreHP must clamp at max HP");
        assertTrue(output.contains("Squirtle recovered 10 HP!"), "RestoreHP must report actual HP restored (10)");
        assertFalse(output.contains("50 HP"), "RestoreHP must not report requested HP (50)");
    }
}
