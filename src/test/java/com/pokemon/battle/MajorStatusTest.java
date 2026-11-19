package com.pokemon.battle;

import com.pokemon.BattleEngine;
import com.pokemon.api.IMove;
import com.pokemon.data.MoveRegistry;
import com.pokemon.effects.Burn;
import com.pokemon.effects.Freeze;
import com.pokemon.effects.Paralysis;
import com.pokemon.effects.Poison;
import com.pokemon.effects.Sleep;
import com.pokemon.targets.Defender;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MajorStatusTest
{
    private static Battler battlerWithMoves(String name, Element element, int speed, List<IMove> moves)
    {
        return new Battler(name, List.of(element), 500, 50, 60, 60, 60, 60, speed, moves);
    }

    @Test
    void burnedOrParalyzedBattlerCannotReceiveSecondMajorStatus()
    {
        Battler attacker = battlerWithMoves("Attacker", Element.NORMAL, 80, List.of(MoveRegistry.get("Tackle")));
        Battler burnedDefender = battlerWithMoves("BurnedTarget", Element.NORMAL, 50, List.of(MoveRegistry.get("Tackle")));
        Battler paralyzedDefender = battlerWithMoves("ParaTarget", Element.NORMAL, 50, List.of(MoveRegistry.get("Tackle")));

        // Seed 42L: standard deterministic seed; ThunderWave (90% acc) and Spore (100% acc) both hit on initial rolls.
        Battle burnBattle = new Battle(attacker, burnedDefender, 42L);
        new Burn(new Defender()).apply(burnBattle);
        assertEquals(MajorStatus.BURNED, burnedDefender.getMajorStatus());

        // Direct effect attempts and status moves must not overwrite BURNED
        new Paralysis(new Defender()).apply(burnBattle);
        new Sleep(new Defender()).apply(burnBattle);
        MoveRegistry.get("ThunderWave").execute(burnBattle);
        MoveRegistry.get("Spore").execute(burnBattle);

        assertEquals(MajorStatus.BURNED, burnedDefender.getMajorStatus());
        assertTrue(burnedDefender.isBurned());
        assertFalse(burnedDefender.isParalyzed());
        assertFalse(burnedDefender.isAsleep());

        // Paralyzed defender also rejects a second status (Burn / Poison / WillOWisp / Toxic)
        Battle paraBattle = new Battle(attacker, paralyzedDefender, 42L);
        new Paralysis(new Defender()).apply(paraBattle);
        assertEquals(MajorStatus.PARALYZED, paralyzedDefender.getMajorStatus());

        new Burn(new Defender()).apply(paraBattle);
        new Poison(new Defender()).apply(paraBattle);
        MoveRegistry.get("WillOWisp").execute(paraBattle);
        MoveRegistry.get("Toxic").execute(paraBattle);

        assertEquals(MajorStatus.PARALYZED, paralyzedDefender.getMajorStatus());
        assertTrue(paralyzedDefender.isParalyzed());
        assertFalse(paralyzedDefender.isBurned());
        assertFalse(paralyzedDefender.isPoisoned());
    }

    @Test
    void sleepingBattlerLosesExactRolledTurnsThenWakesAndActs()
    {
        // Seed 2L:
        // - CPU move pick: nextInt(1)
        // - Spore accuracy check: nextDouble() (always hits at 1.00)
        // - Sleep duration roll: nextInt(3) + 1 = 1 + 1 = 2 turns
        // Defender must lose exactly 2 turns ("is fast asleep!"), then wake up ("woke up!") and act on turn 3.
        long seed = 2L;
        Battler player = battlerWithMoves("SleeperUser", Element.GRASS, 100,
            List.of(MoveRegistry.get("Spore"), MoveRegistry.get("Tackle")));
        Battler cpu = battlerWithMoves("Victim", Element.NORMAL, 50,
            List.of(MoveRegistry.get("Tackle")));

        // Round 1: pick [1] Spore; later rounds: pick [2] Tackle so the battle terminates.
        String input = "1\n" + "2\n".repeat(40);
        String output = runBattleEngine(player, cpu, seed, input);

        int asleepOccurrences = countOccurrences(output, "Victim is fast asleep!");
        assertEquals(2, asleepOccurrences, "Expected Victim to lose exactly 2 rolled sleep turns.\nOutput:\n" + output);

        int firstAsleepIdx = output.indexOf("Victim is fast asleep!");
        int wokeUpIdx = output.indexOf("Victim woke up!");
        int moveLineIdx = output.indexOf("Victim used Tackle!", wokeUpIdx);

        assertTrue(firstAsleepIdx >= 0, "Expected 'fast asleep' message in output");
        assertTrue(wokeUpIdx > firstAsleepIdx, "Expected 'woke up!' after 'fast asleep!'");
        assertTrue(moveLineIdx > wokeUpIdx, "Expected Victim's move line after 'woke up!'");
        assertEquals(MajorStatus.NONE, cpu.getMajorStatus());
        assertFalse(cpu.isAsleep());
        assertTrue(player.getCurrentHp() < player.getMaxHp(), "Victim should deal Tackle damage after waking up");
    }

    @Test
    void frozenBattlerThawsOnKnownLowRollSeedAndActs()
    {
        // Seed 42L:
        // - CPU move pick: nextInt(1)
        // - thaw roll: nextDouble() = 0.05466... (< 0.20 thaw threshold),
        // so a FROZEN attacker thaws immediately at the start of its first turn and executes its move.
        long seed = 42L;
        Battler player = battlerWithMoves("FrozenFighter", Element.NORMAL, 100,
            List.of(MoveRegistry.get("Tackle")));
        Battler cpu = battlerWithMoves("Opponent", Element.NORMAL, 50,
            List.of(MoveRegistry.get("SwordsDance")));

        new Freeze(new Defender()).apply(new Battle(cpu, player, seed));
        assertTrue(player.isFrozen());
        assertEquals(" [FRZ]", statusTag(player));

        // All rounds: player picks Tackle [1]
        String output = runBattleEngine(player, cpu, seed, "1\n".repeat(40));

        int thawedIdx = output.indexOf("FrozenFighter thawed out!");
        int moveIdx = output.indexOf("FrozenFighter used Tackle!", thawedIdx);

        assertTrue(thawedIdx >= 0, "Expected 'thawed out!' on seed 42L.\nOutput:\n" + output);
        assertTrue(moveIdx > thawedIdx, "Expected FrozenFighter to execute Tackle after thawing");
        assertEquals(MajorStatus.NONE, player.getMajorStatus());
        assertFalse(player.isFrozen());
        assertTrue(cpu.getCurrentHp() < cpu.getMaxHp(), "Opponent should take Tackle damage after thaw");
    }

    /** Mirrors BattleEngine's private status tag rendering for test assertions. */
    private static String statusTag(Battler b)
    {
        if (b.isParalyzed())     { return " [PAR]"; }
        if (b.isBurned())        { return " [BRN]"; }
        if (b.isFrozen())        { return " [FRZ]"; }
        if (b.isPoisoned())      { return " [PSN]"; }
        if (b.isBadlyPoisoned()) { return " [TOX]"; }
        if (b.isAsleep())        { return " [SLP]"; }
        return "";
    }

    private static String runBattleEngine(Battler player, Battler cpu, long seed, String scriptedInput)
    {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try
        {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            Scanner scanner = new Scanner(
                new ByteArrayInputStream(scriptedInput.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8
            );
            new BattleEngine(player, cpu, scanner, seed).run();
        }
        finally
        {
            System.setOut(originalOut);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static int countOccurrences(String text, String sub)
    {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1)
        {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
