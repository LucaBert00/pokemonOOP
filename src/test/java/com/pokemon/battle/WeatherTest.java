package com.pokemon.battle;

import com.pokemon.BattleEngine;
import com.pokemon.api.IMove;
import com.pokemon.conditions.HasWeather;
import com.pokemon.data.MoveRegistry;
import com.pokemon.data.PokemonLoader;
import com.pokemon.effects.SetWeather;
import com.pokemon.moves.Move;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeatherTest
{
    private static String captureStdout(Runnable action)
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

    private static Battler battler(String name, Element element, int maxHp, int currentHp, int level, int speed)
    {
        Battler b = new Battler(name, List.of(element), maxHp, level, 80, 80, 80, 80, speed, List.of());
        if (currentHp < maxHp)
        {
            b.takeDamage(maxHp - currentHp);
        }
        return b;
    }

    @Test
    void setWeatherStoresActiveWeatherAndPrintsExactAnnouncement()
    {
        Battle battle = new Battle(
            battler("Ninetales", Element.FIRE, 200, 200, 50, 100),
            battler("Snorlax", Element.NORMAL, 200, 200, 50, 30),
            42L
        );
        assertEquals(Weather.NONE, battle.getWeather());

        String sunOutput = captureStdout(() -> new SetWeather(Weather.SUN).apply(battle));
        assertEquals(Weather.SUN, battle.getWeather());
        assertTrue(sunOutput.contains("  The sunlight turned harsh!"),
            "Expected harsh sunlight announcement, got:\n" + sunOutput);

        String normalOutput = captureStdout(() -> new SetWeather(Weather.NONE).apply(battle));
        assertEquals(Weather.NONE, battle.getWeather());
        assertTrue(normalOutput.contains("  The weather returned to normal!"),
            "Expected normal weather announcement, got:\n" + normalOutput);
    }

    @Test
    void hasWeatherMatchesOnlyTheActiveWeather()
    {
        for (Weather active : Weather.values())
        {
            Battle battle = new Battle(
                battler("Attacker", Element.NORMAL, 100, 100, 50, 50),
                battler("Defender", Element.NORMAL, 100, 100, 50, 50),
                42L
            );
            battle.setWeather(active);

            for (Weather probe : Weather.values())
            {
                boolean expected = (probe == active);
                assertEquals(expected, new HasWeather(probe).check(battle),
                    () -> "Expected HasWeather(" + probe + ") == " + expected + " when active weather is " + active);
            }
        }
    }

    @Test
    void sunnyDayRegisteredMoveSetsSunWeather()
    {
        Battle battle = new Battle(
            battler("Ninetales", Element.FIRE, 200, 200, 50, 100),
            battler("Snorlax", Element.NORMAL, 200, 200, 50, 30),
            42L
        );
        IMove sunnyDay = MoveRegistry.get("SunnyDay");

        String output = captureStdout(() -> sunnyDay.execute(battle));

        assertEquals(Weather.SUN, battle.getWeather());
        assertTrue(output.contains("*Sunny Day*"),
            "Expected move animation line '*Sunny Day*', got:\n" + output);
        assertTrue(output.contains("  The sunlight turned harsh!"),
            "Expected harsh sunlight announcement, got:\n" + output);
    }

    @Test
    void solarBeamDealsDoubleDamageInSunUnderFixedSeed()
    {
        IMove solarBeam = MoveRegistry.get("SolarBeam");

        Battler attacker1 = battler("Ninetales", Element.FIRE, 1000, 1000, 50, 100);
        Battler defender1 = battler("Snorlax", Element.NORMAL, 1000, 1000, 50, 30);
        Battle normalBattle = new Battle(attacker1, defender1, 42L);
        solarBeam.execute(normalBattle);
        int normalDamage = 1000 - defender1.getCurrentHp();

        Battler attacker2 = battler("Ninetales", Element.FIRE, 1000, 1000, 50, 100);
        Battler defender2 = battler("Snorlax", Element.NORMAL, 1000, 1000, 50, 30);
        Battle sunBattle = new Battle(attacker2, defender2, 42L);
        sunBattle.setWeather(Weather.SUN);
        solarBeam.execute(sunBattle);
        int sunDamage = 1000 - defender2.getCurrentHp();

        // FormulaDamage adds a flat +2.0 after the power term, so doubling base power
        // from 60 to 120 at Lv.50 (80 SpA vs 80 SpD, seed 42L) yields 27 and 52 damage.
        assertEquals(27, normalDamage);
        assertEquals(52, sunDamage);
    }

    @Test
    void battleEngineCarriesWeatherAcrossRoundsAndEmpowersSolarBeam()
    {
        IMove sunnyDay = MoveRegistry.get("SunnyDay");
        IMove solarBeam = MoveRegistry.get("SolarBeam");
        IMove idle = new Move("Idle", Element.NORMAL, battle -> {});

        Battler ninetales = new Battler(
            "Ninetales", List.of(Element.FIRE), 200, 50, 76, 75, 80, 100, 100,
            List.of(sunnyDay, solarBeam)
        );
        // 40 HP is above 60-BP Solar Beam max (28) and below 120-BP Sun-boosted Solar Beam min (46)
        Battler snorlax = new Battler(
            "Snorlax", List.of(Element.NORMAL), 40, 50, 110, 65, 65, 80, 30,
            List.of(idle)
        );

        String scriptedMoves = "1\n2\n";
        Scanner scanner = new Scanner(
            new ByteArrayInputStream(scriptedMoves.getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8
        );
        String output = captureStdout(() -> new BattleEngine(ninetales, snorlax, scanner, 42L).run());

        assertTrue(snorlax.isFainted(), "Sun-boosted Solar Beam in Round 2 must KO 40-HP Snorlax");
        assertTrue(output.contains("  The sunlight turned harsh!"),
            "Expected Sunny Day announcement in Round 1, got:\n" + output);
        assertTrue(output.contains("  Weather: SUN"),
            "Expected BattleEngine status to display active weather across rounds, got:\n" + output);
    }

    @Test
    void weatherMovesAreRegisteredAndAssignedToNinetales()
    {
        IMove sunnyDay = MoveRegistry.get("SunnyDay");
        IMove solarBeam = MoveRegistry.get("SolarBeam");
        assertNotNull(sunnyDay);
        assertNotNull(solarBeam);

        Battler ninetales = PokemonLoader.loadAll().stream()
            .filter(b -> "Ninetales".equals(b.getName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Ninetales not found in PokemonLoader.loadAll()"));

        assertTrue(ninetales.getMoves().contains(sunnyDay),
            "Ninetales must have SunnyDay assigned in pokemon.csv");
        assertTrue(ninetales.getMoves().contains(solarBeam),
            "Ninetales must have SolarBeam assigned in pokemon.csv");
    }
}
