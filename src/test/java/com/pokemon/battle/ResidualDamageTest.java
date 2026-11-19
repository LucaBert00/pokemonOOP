package com.pokemon.battle;

import com.pokemon.BattleEngine;
import com.pokemon.api.IMove;
import com.pokemon.effects.BadlyPoison;
import com.pokemon.effects.Burn;
import com.pokemon.effects.DirectDamage;
import com.pokemon.effects.Poison;
import com.pokemon.moves.Move;
import com.pokemon.numbers.Exactly;
import com.pokemon.targets.Defender;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResidualDamageTest
{
    private static IMove idleMove()
    {
        return new Move("Idle", Element.NORMAL, battle -> {});
    }

    private static IMove knockoutMove()
    {
        return new Move("Finisher", Element.NORMAL,
            battle -> new DirectDamage(new Defender(), new Exactly(9999)).apply(battle));
    }

    private static Battler battler(String name, Element element, int maxHp, int speed, List<IMove> moves)
    {
        return new Battler(name, List.of(element), maxHp, 50, 60, 60, 60, 60, speed, moves);
    }

    private static String runEngine(Battler player, Battler cpu, String scriptedChoices, long seed)
    {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try
        {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            Scanner scanner = new Scanner(
                new ByteArrayInputStream(scriptedChoices.getBytes(StandardCharsets.UTF_8)),
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

    private static int countOccurrences(String text, String token)
    {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) != -1)
        {
            count++;
            index += token.length();
        }
        return count;
    }

    @Test
    void burnedBattlerAt160MaxHpLoses10HpPerRoundAndFaintsOn16thTick()
    {
        Battler burnedPlayer = battler("Charmeleon", Element.FIRE, 160, 80, List.of(idleMove()));
        Battler healthyCpu = battler("Blastoise", Element.WATER, 500, 70, List.of(idleMove()));

        new Burn(new Defender()).apply(new Battle(healthyCpu, burnedPlayer, 42L));
        assertTrue(burnedPlayer.isBurned());

        String output = runEngine(burnedPlayer, healthyCpu, "1\n".repeat(20), 42L);

        assertTrue(burnedPlayer.isFainted());
        assertEquals(0, burnedPlayer.getCurrentHp());
        assertEquals(500, healthyCpu.getCurrentHp());
        assertEquals(16, countOccurrences(output, "─── Turn "), "Expected exactly 16 rounds for 160 HP at 10 HP/round");
        assertEquals(16, countOccurrences(output, "Charmeleon is hurt by its burn! (-10 HP)"));
    }

    @Test
    void poisonedBattlerAt160MaxHpLoses10HpPerRoundAndFaintsOn16thTick()
    {
        Battler poisonedPlayer = battler("Ivysaur", Element.GRASS, 160, 80, List.of(idleMove()));
        Battler healthyCpu = battler("Venusaur", Element.GRASS, 500, 70, List.of(idleMove()));

        new Poison(new Defender()).apply(new Battle(healthyCpu, poisonedPlayer, 42L));
        assertTrue(poisonedPlayer.isPoisoned());

        String output = runEngine(poisonedPlayer, healthyCpu, "1\n".repeat(20), 42L);

        assertTrue(poisonedPlayer.isFainted());
        assertEquals(0, poisonedPlayer.getCurrentHp());
        assertEquals(500, healthyCpu.getCurrentHp());
        assertEquals(16, countOccurrences(output, "─── Turn "), "Expected 16 rounds at 10 HP/round (1/16), not 8 rounds at 20 HP");
        assertEquals(16, countOccurrences(output, "Ivysaur is hurt by poison! (-10 HP)"));
    }

    @Test
    void badlyPoisonedBattlerAt160MaxHpRamps10Then20Then30AcrossConsecutiveRounds()
    {
        Battler toxicPlayer = battler("Snorlax", Element.NORMAL, 160, 80, List.of(idleMove(), knockoutMove()));
        Battler healthyCpu = battler("Blissey", Element.NORMAL, 500, 70, List.of(idleMove()));

        new BadlyPoison(new Defender()).apply(new Battle(healthyCpu, toxicPlayer, 42L));
        assertTrue(toxicPlayer.isBadlyPoisoned());
        assertEquals(1, toxicPlayer.getToxicCounter());

        // Rounds 1-3 use slot 1 (idle: 10 + 20 + 30 = 60 residual damage); Round 4 uses slot 2 to end the battle cleanly
        String output = runEngine(toxicPlayer, healthyCpu, "1\n1\n1\n2\n", 42L);

        assertTrue(healthyCpu.isFainted());
        assertEquals(100, toxicPlayer.getCurrentHp(), "160 - 10 - 20 - 30 must leave 100 HP after 3 rounds");
        assertEquals(4, toxicPlayer.getToxicCounter());

        int tick1 = output.indexOf("Snorlax is badly hurt by poison! (-10 HP)");
        int tick2 = output.indexOf("Snorlax is badly hurt by poison! (-20 HP)");
        int tick3 = output.indexOf("Snorlax is badly hurt by poison! (-30 HP)");
        assertTrue(tick1 >= 0, "Expected round 1 Toxic tick of -10 HP");
        assertTrue(tick2 > tick1, "Expected round 2 Toxic tick of -20 HP after round 1 tick");
        assertTrue(tick3 > tick2, "Expected round 3 Toxic tick of -30 HP after round 2 tick");

        toxicPlayer.clearMajorStatus();
        assertEquals(0, toxicPlayer.getToxicCounter());
    }

    @Test
    void fullyParalyzedBattlerSkippedHalfTurnDoesNotSuppressOpponentBurnTick()
    {
        // Seed 1L: pickCpuMove with 1 move consumes 1 LCG step (s1), then Probability<>(0.25).check(battle)
        // draws nextDouble() from s2/s3 = 0.10047... < 0.25, triggering full paralysis on Round 1.
        Battler paralyzedPlayer = battler("Pikachu", Element.ELECTRIC, 200, 120, List.of(idleMove(), knockoutMove()));
        Battler burnedCpu = battler("Arcanine", Element.FIRE, 160, 80, List.of(idleMove()));

        new com.pokemon.effects.Paralysis(new Defender()).apply(new Battle(burnedCpu, paralyzedPlayer, 1L));
        new Burn(new Defender()).apply(new Battle(paralyzedPlayer, burnedCpu, 1L));

        assertTrue(paralyzedPlayer.isParalyzed());
        assertTrue(burnedCpu.isBurned());
        // Paralyzed speed 120 / 2 = 60 < Arcanine speed 80 -> Arcanine acts first, Pikachu acts second
        assertEquals(60, paralyzedPlayer.getEffectiveSpeed());

        // Round 1: Pikachu is fully paralyzed (skipped half-turn), Arcanine still takes 10 HP burn at end of round.
        // Round 2: Pikachu selects slot 2 (knockoutMove) to end the battle if not paralyzed.
        String output = runEngine(paralyzedPlayer, burnedCpu, "1\n2\n2\n2\n2\n", 1L);

        assertTrue(output.contains("Pikachu is paralyzed and can't move!"), "Expected full paralysis skip on Round 1");
        assertTrue(output.contains("Arcanine is hurt by its burn! (-10 HP)"), "Skipped half-turn must not suppress opponent burn tick");
    }
}
