package com.pokemon.battle;

import com.pokemon.BattleEngine;
import com.pokemon.api.IMove;
import com.pokemon.data.MoveRegistry;
import com.pokemon.effects.Burn;
import com.pokemon.effects.Paralysis;
import com.pokemon.moves.Move;
import com.pokemon.targets.Defender;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deterministic JUnit 5 regression coverage for the 5 Fase 1 edge cases
 * previously unobservable during manual terminal verification (FINDINGS.md).
 */
class Fase1EdgeCaseTest
{
    private static String captureStdOut(Runnable action)
    {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try
        {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            action.run();
        }
        finally
        {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static long findSeedWithFirstDoubleAtLeast(double threshold)
    {
        for (long seed = 1L; seed < 10_000L; seed++)
        {
            Random r = new Random(seed);
            r.nextInt(1); // CPU move pick in BattleEngine
            if (r.nextDouble() >= threshold)
            {
                return seed;
            }
        }
        throw new IllegalStateException("No seed found for threshold >= " + threshold);
    }

    @Test
    void thunderWaveOnGroundAndParalyzedDefenderPrintsImmunityFirst()
    {
        IMove thunderWave = MoveRegistry.get("ThunderWave");
        Battler attacker = new Battler("Pikachu", List.of(Element.ELECTRIC), 150, 50, 80, 60, 80, 60, 90, List.of(thunderWave));
        Battler defender = new Battler("Sandslash", List.of(Element.GROUND), 150, 50, 100, 110, 45, 55, 65, List.of(MoveRegistry.get("Tackle")));

        // Pre-apply paralysis so both outer immunity (Ground) and inner precondition (already paralyzed) apply
        new Paralysis(new Defender()).apply(new Battle(attacker, defender, 42L));
        assertTrue(defender.isParalyzed());

        Battle battle = new Battle(attacker, defender, 42L);
        String output = captureStdOut(() -> thunderWave.execute(battle));

        assertTrue(output.contains("It doesn't affect Sandslash!"),
            "Outer WithApplicability must print type immunity first, got:\n" + output);
        assertFalse(output.contains("already"),
            "Inner WithPrecondition failure message must not be reached, got:\n" + output);
        assertFalse(output.contains("But it failed!"),
            "Inner WithPrecondition default failure must not be reached, got:\n" + output);
    }

    @Test
    void willOWispOnFireAndBurnedDefenderPrintsImmunityFirst()
    {
        IMove willOWisp = MoveRegistry.get("WillOWisp");
        Battler attacker = new Battler("Gengar", List.of(Element.GHOST, Element.POISON), 150, 50, 65, 60, 130, 75, 110, List.of(willOWisp));
        Battler defender = new Battler("Charizard", List.of(Element.FIRE, Element.FLYING), 150, 50, 84, 78, 109, 85, 100, List.of(MoveRegistry.get("Tackle")));

        // Pre-apply burn so both outer immunity (Fire) and inner precondition (already burned) apply
        new Burn(new Defender()).apply(new Battle(attacker, defender, 42L));
        assertTrue(defender.isBurned());

        Battle battle = new Battle(attacker, defender, 42L);
        String output = captureStdOut(() -> willOWisp.execute(battle));

        assertTrue(output.contains("It doesn't affect Charizard!"),
            "Outer WithApplicability must print Fire immunity first, got:\n" + output);
        assertFalse(output.contains("already"),
            "Inner WithPrecondition failure message must not be reached, got:\n" + output);
        assertFalse(output.contains("But it failed!"),
            "Inner WithPrecondition default failure must not be reached, got:\n" + output);
    }

    @Test
    void furyAttackStopsImmediatelyWhenDefenderFaints()
    {
        IMove furyAttack = MoveRegistry.get("FuryAttack");
        Battler attacker = new Battler("Raticate", List.of(Element.NORMAL), 150, 50, 95, 60, 50, 70, 97, List.of(furyAttack));
        Battler defender = new Battler("Caterpie", List.of(Element.BUG), 1, 50, 30, 35, 20, 20, 45, List.of(MoveRegistry.get("Tackle")));

        // Seed 42L: Weighted rolls 3 hits (0.72756), first accuracy roll passes (< 0.85), KOing 1-HP defender on hit 1
        Battle battle = new Battle(attacker, defender, 42L);
        String output = captureStdOut(() -> furyAttack.execute(battle));

        assertTrue(defender.isFainted(), "Defender with 1 HP must faint on hit 1");
        assertEquals(0, defender.getCurrentHp());
        assertTrue(output.contains("It hit 1 time!"),
            "Combo must stop on faint and report actual applied hits (1), got:\n" + output);
    }

    @Test
    void bodySlamTriggersParalysisOnDeterministicSeed()
    {
        IMove bodySlam = MoveRegistry.get("BodySlam");
        Battler attacker = new Battler("Snorlax", List.of(Element.NORMAL), 220, 50, 110, 65, 65, 110, 30, List.of(bodySlam));
        Battler defender = new Battler("Blastoise", List.of(Element.WATER), 200, 50, 83, 100, 85, 105, 78, List.of(MoveRegistry.get("Tackle")));

        // Seed 1L: 1st roll 0.7309 (< 1.0 hit), 2nd roll 0.4100 (damage factor), 3rd roll 0.2077 (< 0.30 paralysis)
        Battle battle = new Battle(attacker, defender, 1L);
        String output = captureStdOut(() -> bodySlam.execute(battle));

        int damageDealt = defender.getMaxHp() - defender.getCurrentHp();
        assertTrue(damageDealt > 0, "Body Slam must deal positive physical damage");
        assertFalse(defender.isFainted());
        assertEquals(MajorStatus.PARALYZED, defender.getMajorStatus(),
            "30% paralysis secondary effect must trigger on seed 1L");
        assertTrue(output.contains("Blastoise was paralyzed!"),
            "Expected paralysis message in output, got:\n" + output);
    }

    @Test
    void jumpKickMissFaintsAttackerOnCrashDamage()
    {
        IMove jumpKick = MoveRegistry.get("JumpKick");
        IMove splashNoOp = new Move("Wait", Element.NORMAL, battle -> { });

        // Attacker starts at 40 / 100 HP (<= 50% max HP) so 50% crash damage (50 HP) faints it
        Battler attacker = new Battler("Machop", List.of(Element.FIGHTING), 100, 50, 80, 50, 35, 35, 35, List.of(jumpKick));
        attacker.takeDamage(60);
        assertEquals(40, attacker.getCurrentHp());

        // Burn attacker beforehand to verify self-faint terminates safely without residual burn ticks
        new Burn(new Defender()).apply(new Battle(attacker, attacker, 42L));
        assertTrue(attacker.isBurned());

        Battler defender = new Battler("Slowpoke", List.of(Element.WATER), 200, 50, 65, 65, 40, 40, 15, List.of(splashNoOp));

        // Find first deterministic seed where Jump Kick's 95% accuracy roll misses (>= 0.95)
        long missSeed = findSeedWithFirstDoubleAtLeast(0.95);
        Scanner scanner = new Scanner(new ByteArrayInputStream("1\n".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);

        String output = captureStdOut(() -> new BattleEngine(attacker, defender, scanner, missSeed).run());

        assertTrue(attacker.isFainted(), "Attacker must faint from 50% max HP crash damage");
        assertEquals(0, attacker.getCurrentHp());
        assertEquals(200, defender.getCurrentHp(), "Defender must take 0 damage on Jump Kick miss");
        assertTrue(output.contains("crashed"),
            "Expected crash damage message on Jump Kick miss, got:\n" + output);
        assertFalse(output.contains("Machop is hurt by its burn!"),
            "Fainted attacker must not take end-of-round residual burn damage, got:\n" + output);
    }
}
