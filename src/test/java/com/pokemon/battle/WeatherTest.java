package com.pokemon.battle;

import com.pokemon.conditions.HasWeather;
import com.pokemon.effects.SetWeather;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
}
