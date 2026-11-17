package com.pokemon;

import com.pokemon.api.IMove;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;
import com.pokemon.battle.Element;
import com.pokemon.conditions.Probability;
import com.pokemon.data.MoveRegistry;
import com.pokemon.data.PokemonLoader;
import com.pokemon.effects.FormulaDamage;
import com.pokemon.numbers.Between;
import com.pokemon.numbers.Exactly;
import com.pokemon.numbers.Weighted;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BattleRandomDeterminismTest
{
    private static Battler sampleBattler(String name, Element element, int hp)
    {
        List<IMove> moves = List.of(
            MoveRegistry.get("Tackle"),
            MoveRegistry.get("BodySlam"),
            MoveRegistry.get("FuryAttack"),
            MoveRegistry.get("QuickAttack")
        );
        return new Battler(name, List.of(element), hp, 50, 70, 60, 70, 60, 80, moves);
    }

    @Test
    void matchingSeedsProduceIdenticalPrimitiveSequences()
    {
        long seed = 42L;
        Battle b1 = new Battle(sampleBattler("A1", Element.NORMAL, 500), sampleBattler("D1", Element.NORMAL, 500), seed);
        Battle b2 = new Battle(sampleBattler("A2", Element.NORMAL, 500), sampleBattler("D2", Element.NORMAL, 500), seed);

        Probability<Battle> prob = new Probability<>(0.50);
        Weighted weighted = new Weighted(
            new double[]{2, 0.375},
            new double[]{3, 0.375},
            new double[]{4, 0.125},
            new double[]{5, 0.125}
        );
        Between between = new Between(0.5, 1.5);
        FormulaDamage formula = new FormulaDamage(new Exactly(40));

        for (int i = 0; i < 50; i++)
        {
            assertEquals(b1.nextDouble(), b2.nextDouble());
            assertEquals(b1.nextInt(100), b2.nextInt(100));
            assertEquals(b1.nextBoolean(), b2.nextBoolean());
            assertEquals(prob.check(b1), prob.check(b2));
            assertEquals(weighted.evaluate(b1), weighted.evaluate(b2));

            double v1 = between.evaluate(b1);
            double v2 = between.evaluate(b2);
            assertEquals(v1, v2);
            assertTrue(v1 >= 0.5 && v1 <= 1.5, "Between value out of [0.5, 1.5]: " + v1);

            formula.apply(b1);
            formula.apply(b2);
            assertEquals(b1.getLastDamageDealt(), b2.getLastDamageDealt());
        }
    }

    @Test
    void betweenHandlesDegenerateAndInvalidBounds()
    {
        Battle battle = new Battle(sampleBattler("A", Element.NORMAL, 100), sampleBattler("D", Element.NORMAL, 100), 7L);
        assertEquals(3.25, new Between(3.25, 3.25).evaluate(battle));
        assertThrows(IllegalArgumentException.class, () -> new Between(5.0, 2.0));
    }

    @Test
    void battleEngineProducesIdenticalOutputStreamsForMatchingSeeds()
    {
        String out1 = runSeededEngineBattle(42L);
        String out2 = runSeededEngineBattle(42L);

        assertFalse(out1.isBlank());
        assertEquals(out1, out2);
    }

    @Test
    void mainCliSeedFlagProducesIdenticalOutputAcrossRuns()
    {
        String scriptedInput = "1\n" + "2\n".repeat(40);
        String run1 = runMainWithArgs(new String[]{"--seed", "42"}, scriptedInput);
        String run2 = runMainWithArgs(new String[]{"--seed", "42"}, scriptedInput);

        assertTrue(run1.contains("You chose: Pikachu!"));
        assertEquals(run1, run2);
    }

    @Test
    void mainParseSeedValidatesArguments()
    {
        assertNull(Main.parseSeed(new String[0]));
        assertEquals(42L, Main.parseSeed(new String[]{"--seed", "42"}));
        assertEquals(-99L, Main.parseSeed(new String[]{"--seed=-99"}));
        assertThrows(IllegalArgumentException.class, () -> Main.parseSeed(new String[]{"--seed"}));
        assertThrows(IllegalArgumentException.class, () -> Main.parseSeed(new String[]{"--seed", "abc"}));
        assertThrows(IllegalArgumentException.class, () -> Main.parseSeed(new String[]{"--unknown"}));
    }

    private static String runSeededEngineBattle(long seed)
    {
        List<Battler> roster = PokemonLoader.loadAll();
        Battler player = roster.get(0); // Pikachu
        Battler cpu = roster.get(2);    // Squirtle
        String movesInput = "2\n".repeat(40);

        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try
        {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            Scanner scanner = new Scanner(new ByteArrayInputStream(movesInput.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
            new BattleEngine(player, cpu, scanner, seed).run();
        }
        finally
        {
            System.setOut(originalOut);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static String runMainWithArgs(String[] args, String scriptedInput)
    {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try
        {
            System.setIn(new ByteArrayInputStream(scriptedInput.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            Main.main(args);
        }
        finally
        {
            System.setIn(originalIn);
            System.setOut(originalOut);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
