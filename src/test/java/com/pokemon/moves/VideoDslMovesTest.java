package com.pokemon.moves;

import com.pokemon.api.IMove;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;
import com.pokemon.battle.Element;
import com.pokemon.data.MoveRegistry;
import com.pokemon.effects.DirectDamage;
import com.pokemon.effects.SetHP;
import com.pokemon.numbers.Exactly;
import com.pokemon.targets.Attacker;
import com.pokemon.targets.Defender;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VideoDslMovesTest
{
    private static Battler battler(String name, Element element, int maxHp, int currentHp, int level, int speed)
    {
        Battler b = new Battler(
            name,
            List.of(element),
            maxHp,
            level,
            80,
            80,
            80,
            80,
            speed,
            List.of(MoveRegistry.get("Tackle"))
        );
        b.setCurrentHp(currentHp);
        return b;
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
    void setHpClampsToZeroAndMaxHpAndPrintsMessage()
    {
        Battler attacker = battler("Attacker", Element.NORMAL, 120, 50, 50, 80);
        Battler defender = battler("Defender", Element.NORMAL, 120, 120, 50, 70);
        Battle battle = new Battle(attacker, defender, 42L);

        String highOut = captureStdout(() -> new SetHP(new Attacker(), new Exactly(999)).apply(battle));
        assertEquals(120, attacker.getCurrentHp());
        assertTrue(highOut.contains("Attacker's HP became 120!"));

        String lowOut = captureStdout(() -> new SetHP(new Attacker(), new Exactly(-15)).apply(battle));
        assertEquals(0, attacker.getCurrentHp());
        assertTrue(attacker.isFainted());
        assertTrue(lowOut.contains("Attacker's HP became 0!"));
    }

    @Test
    void painSplitAveragesEvenAndOddHpSumsWithFlooring()
    {
        IMove painSplit = MoveRegistry.get("PainSplit");

        // Even sum: (20 + 80) / 2 = 50
        Battler evenAttacker = battler("Attacker", Element.NORMAL, 100, 20, 50, 80);
        Battler evenDefender = battler("Defender", Element.NORMAL, 100, 80, 50, 70);
        Battle evenBattle = new Battle(evenAttacker, evenDefender, 42L);

        String evenOut = captureStdout(() -> painSplit.execute(evenBattle));
        assertEquals(50, evenAttacker.getCurrentHp());
        assertEquals(50, evenDefender.getCurrentHp());
        assertTrue(evenOut.contains("Attacker's HP became 50!"));
        assertTrue(evenOut.contains("Defender's HP became 50!"));

        // Odd sum: floor((21 + 80) / 2) = floor(50.5) = 50
        Battler oddAttacker = battler("Attacker", Element.NORMAL, 100, 21, 50, 80);
        Battler oddDefender = battler("Defender", Element.NORMAL, 100, 80, 50, 70);
        Battle oddBattle = new Battle(oddAttacker, oddDefender, 42L);

        String oddOut = captureStdout(() -> painSplit.execute(oddBattle));
        assertEquals(50, oddAttacker.getCurrentHp());
        assertEquals(50, oddDefender.getCurrentHp());
        assertTrue(oddOut.contains("Attacker's HP became 50!"));
        assertTrue(oddOut.contains("Defender's HP became 50!"));
    }

    @Test
    void selfDestructInflictsDamageAndFaintsAttacker()
    {
        IMove selfDestruct = MoveRegistry.get("SelfDestruct");
        Battler attacker = battler("Voltorb", Element.ELECTRIC, 150, 150, 50, 100);
        Battler defender = battler("Snorlax", Element.NORMAL, 500, 500, 50, 30);
        Battle battle = new Battle(attacker, defender, 42L);

        String out = captureStdout(() -> selfDestruct.execute(battle));
        assertTrue(defender.getCurrentHp() < 500, "SelfDestruct must damage defender");
        assertTrue(battle.getLastDamageDealt() > 0);
        assertEquals(0, attacker.getCurrentHp());
        assertTrue(attacker.isFainted());
        assertTrue(out.contains("Voltorb"));
    }

    @Test
    void counterHasNegativePriorityFailsAtZeroDamageAndDoublesLastDamageDealt()
    {
        IMove counter = MoveRegistry.get("Counter");
        assertEquals(-5, counter.priority(), "Counter must have priority -5");

        // Fails when lastDamageDealt == 0
        Battler counterUser = battler("Machamp", Element.FIGHTING, 200, 200, 50, 55);
        Battler opponent = battler("Snorlax", Element.NORMAL, 250, 250, 50, 30);
        Battle freshBattle = new Battle(counterUser, opponent, 42L);

        String failOut = captureStdout(() -> counter.execute(freshBattle));
        assertEquals(250, opponent.getCurrentHp());
        assertTrue(failOut.contains("But it failed!"));

        // Deals 2x damage (50) when opponent dealt 25 damage earlier in the round
        Battle activeRound = new Battle(opponent, counterUser, 42L);
        new DirectDamage(new Defender(), new Exactly(25)).apply(activeRound);
        assertEquals(25.0, activeRound.getLastDamageDealt());

        activeRound.flipTurn();
        String hitOut = captureStdout(() -> counter.execute(activeRound));
        assertEquals(200, opponent.getCurrentHp(), "Counter must deal 2 * 25 = 50 damage");
        assertTrue(hitOut.contains("50"));
    }

    @Test
    void psywaveDealsLevelScaledDamageWithinHalfToOnePointFiveTimesLevel()
    {
        IMove psywave = MoveRegistry.get("Psywave");
        int level = 50;
        Battler attacker = battler("Alakazam", Element.PSYCHIC, 150, 150, level, 120);
        Battler defender = battler("Chansey", Element.NORMAL, 500, 500, 50, 50);
        Battle battle = new Battle(attacker, defender, 42L);

        String out = captureStdout(() -> psywave.execute(battle));
        int damageDealt = 500 - defender.getCurrentHp();
        assertTrue(damageDealt >= 25 && damageDealt <= 75,
            "Expected Psywave damage in [25, 75], got " + damageDealt);
        assertEquals(damageDealt, (int) battle.getLastDamageDealt());
        assertTrue(out.contains("took " + damageDealt + " direct damage!"));
    }

    @Test
    void endeavorFailsWhenAttackerHpGeqDefenderAndEqualizesWhenLower()
    {
        IMove endeavor = MoveRegistry.get("Endeavor");

        // Fails when attacker HP == defender HP
        Battler equalAtk = battler("Rattata", Element.NORMAL, 150, 80, 50, 90);
        Battler equalDef = battler("Blissey", Element.NORMAL, 300, 80, 50, 55);
        Battle equalBattle = new Battle(equalAtk, equalDef, 42L);

        String equalOut = captureStdout(() -> endeavor.execute(equalBattle));
        assertEquals(80, equalDef.getCurrentHp());
        assertTrue(equalOut.contains("But it failed!"));

        // Fails when attacker HP > defender HP
        Battler higherAtk = battler("Rattata", Element.NORMAL, 150, 95, 50, 90);
        Battler lowerDef = battler("Blissey", Element.NORMAL, 300, 80, 50, 55);
        Battle higherBattle = new Battle(higherAtk, lowerDef, 42L);

        String higherOut = captureStdout(() -> endeavor.execute(higherBattle));
        assertEquals(80, lowerDef.getCurrentHp());
        assertTrue(higherOut.contains("But it failed!"));

        // Succeeds when attacker HP < defender HP
        Battler hurtAtk = battler("Rattata", Element.NORMAL, 150, 30, 50, 90);
        Battler healthyDef = battler("Blissey", Element.NORMAL, 300, 220, 50, 55);
        Battle hurtBattle = new Battle(hurtAtk, healthyDef, 42L);

        String hurtOut = captureStdout(() -> endeavor.execute(hurtBattle));
        assertEquals(30, healthyDef.getCurrentHp());
        assertEquals(30, hurtAtk.getCurrentHp());
        assertTrue(hurtOut.contains("Blissey's HP became 30!"));
    }
}
