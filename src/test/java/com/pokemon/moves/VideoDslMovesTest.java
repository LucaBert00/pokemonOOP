package com.pokemon.moves;

import com.pokemon.api.IMove;
import com.pokemon.attempts.Attempt;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;
import com.pokemon.battle.Element;
import com.pokemon.battle.MajorStatus;
import com.pokemon.conditions.For;
import com.pokemon.conditions.IsBadlyPoisoned;
import com.pokemon.conditions.IsPoisoned;
import com.pokemon.conditions.Leq;
import com.pokemon.conditions.Or;
import com.pokemon.conditions.Probability;
import com.pokemon.data.MoveRegistry;
import com.pokemon.effects.DirectDamage;
import com.pokemon.effects.SetHP;
import com.pokemon.numbers.ConditionalNumber;
import com.pokemon.numbers.CurrentHP;
import com.pokemon.numbers.Exactly;
import com.pokemon.numbers.Level;
import com.pokemon.numbers.Max;
import com.pokemon.numbers.MaxHP;
import com.pokemon.numbers.Product;
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

    @Test
    void defaultAccuracyAttemptAlwaysHitsAcrossSeededBattles()
    {
        for (long seed = 0L; seed < 100L; seed++)
        {
            Battler atk = battler("Attacker", Element.NORMAL, 100, 100, 50, 80);
            Battler def = battler("Defender", Element.NORMAL, 100, 100, 50, 70);
            Battle battle = new Battle(atk, def, seed);

            int[] onHitCount = {0};
            new Attempt("Test", b -> onHitCount[0]++).execute(battle);

            assertTrue(battle.isLastAttemptHit(),
                "Expected default-accuracy Attempt to hit for seed " + seed);
            assertEquals(1, onHitCount[0],
                "Expected onHit to run once for seed " + seed);
        }
    }

    @Test
    void defaultAccuracyFullOverloadMatchesExplicitCertainAccuracy()
    {
        Battle explicitBattle = new Battle(
            battler("Attacker", Element.NORMAL, 100, 100, 50, 80),
            battler("Defender", Element.NORMAL, 100, 100, 50, 70),
            42L
        );
        Battle defaultBattle = new Battle(
            battler("Attacker", Element.NORMAL, 100, 100, 50, 80),
            battler("Defender", Element.NORMAL, 100, 100, 50, 70),
            42L
        );

        int[] explicitCounts = new int[3];
        String explicitOut = captureStdout(() -> new Attempt(
            "Test",
            new Probability<>(1.0),
            b -> explicitCounts[0]++,
            b -> explicitCounts[1]++,
            b -> explicitCounts[2]++
        ).execute(explicitBattle));

        int[] defaultCounts = new int[3];
        String defaultOut = captureStdout(() -> new Attempt(
            "Test",
            b -> defaultCounts[0]++,
            b -> defaultCounts[1]++,
            b -> defaultCounts[2]++
        ).execute(defaultBattle));

        assertArrayEquals(explicitCounts, defaultCounts);
        assertEquals(1, defaultCounts[0]);
        assertEquals(0, defaultCounts[1]);
        assertEquals(1, defaultCounts[2]);
        assertEquals(explicitOut, defaultOut);
        assertTrue(explicitBattle.isLastAttemptHit());
        assertTrue(defaultBattle.isLastAttemptHit());
        assertEquals(explicitBattle.nextDouble(), defaultBattle.nextDouble());
    }

    @Test
    void maxEvaluatesGreaterOperandAcrossLeftGreaterRightGreaterAndEqualValues()
    {
        Battler attacker = battler("Attacker", Element.NORMAL, 120, 45, 50, 80);
        Battler defender = battler("Defender", Element.NORMAL, 120, 30, 35, 70);
        Battle battle = new Battle(attacker, defender, 42L);

        assertEquals(42.0, new Max(new Exactly(42), new Exactly(17)).evaluate(battle));
        assertEquals(35.0, new Max(new Exactly(12), new Exactly(35)).evaluate(battle));
        assertEquals(20.0, new Max(new Exactly(20), new Exactly(20)).evaluate(battle));

        // Dynamic battler-derived operands: CurrentHP(ATK)=45 vs Level(DEF)=35
        assertEquals(45.0, new Max(new CurrentHP(new Attacker()), new com.pokemon.numbers.Level(new Defender())).evaluate(battle));
    }

    @Test
    void conditionalNumberEvaluatesTrueBranchWhenConditionHoldsAndFalseBranchOtherwise()
    {
        Battler attacker = battler("Attacker", Element.NORMAL, 120, 120, 50, 80);
        Battler healthyDefender = battler("Healthy", Element.NORMAL, 200, 101, 50, 70);
        Battler weakenedDefender = battler("Weakened", Element.NORMAL, 200, 100, 50, 70);

        ConditionalNumber brinePower = new ConditionalNumber(
            new Leq(new CurrentHP(new Defender()), new Product(new MaxHP(new Defender()), new Exactly(0.5))),
            new Exactly(130),
            new Exactly(65)
        );

        assertEquals(65.0, brinePower.evaluate(new Battle(attacker, healthyDefender, 42L)));
        assertEquals(130.0, brinePower.evaluate(new Battle(attacker, weakenedDefender, 42L)));
    }

    @Test
    void psywaveFloorsDamageAtOneEvenWhenScaledProductIsZero()
    {
        IMove psywave = MoveRegistry.get("Psywave");
        // Level 0 causes Level(ATK) * Between(0.5, 1.5) == 0.0; Max(1, ...) must floor to 1
        Battler zeroLevelAttacker = battler("Alakazam", Element.PSYCHIC, 150, 150, 0, 120);
        Battler defender = battler("Chansey", Element.NORMAL, 500, 500, 50, 50);
        Battle battle = new Battle(zeroLevelAttacker, defender, 42L);

        String out = captureStdout(() -> psywave.execute(battle));
        assertEquals(499, defender.getCurrentHp(), "Psywave must deal at least 1 damage via Max floor");
        assertEquals(1.0, battle.getLastDamageDealt());
        assertTrue(out.contains("took 1 direct damage!"));
    }

    @Test
    void brineDealsBasePowerAboveHalfHpAndDoublesPowerAtOrBelowHalfHp()
    {
        IMove brine = MoveRegistry.get("Brine");

        // Above 50% HP (101/200): base power 65 -> 29 damage on seed 42L
        Battler atkAbove = battler("Dewgong", Element.NORMAL, 200, 200, 50, 80);
        Battler defAbove = battler("Target", Element.NORMAL, 200, 101, 50, 70);
        Battle battleAbove = new Battle(atkAbove, defAbove, 42L);
        brine.execute(battleAbove);
        int dmgAbove = 101 - defAbove.getCurrentHp();

        // At exact 50% HP boundary (100/200): doubled power 130 -> 56 damage on seed 42L
        Battler atkAtHalf = battler("Dewgong", Element.NORMAL, 200, 200, 50, 80);
        Battler defAtHalf = battler("Target", Element.NORMAL, 200, 100, 50, 70);
        Battle battleAtHalf = new Battle(atkAtHalf, defAtHalf, 42L);
        brine.execute(battleAtHalf);
        int dmgAtHalf = 100 - defAtHalf.getCurrentHp();

        // Below 50% HP (60/200): doubled power 130 -> 56 damage on seed 42L
        Battler atkBelow = battler("Dewgong", Element.NORMAL, 200, 200, 50, 80);
        Battler defBelow = battler("Target", Element.NORMAL, 200, 60, 50, 70);
        Battle battleBelow = new Battle(atkBelow, defBelow, 42L);
        brine.execute(battleBelow);
        int dmgBelow = 60 - defBelow.getCurrentHp();

        assertEquals(29, dmgAbove);
        assertEquals(56, dmgAtHalf);
        assertEquals(56, dmgBelow);
    }

    @Test
    void dynamicProbabilityClampsNegativeToZeroAndAboveOneToOne()
    {
        Probability<Battle> neverHits = new Probability<>(new Exactly(-0.25));
        Probability<Battle> alwaysHits = new Probability<>(new Exactly(1.75));

        for (long seed = 0L; seed < 50L; seed++)
        {
            Battle battle = new Battle(
                battler("Attacker", Element.NORMAL, 100, 100, 50, 80),
                battler("Defender", Element.NORMAL, 100, 100, 50, 70),
                seed
            );
            assertFalse(neverHits.check(battle),
                "Expected negative dynamic probability to clamp to 0.0 for seed " + seed);
            assertTrue(alwaysHits.check(battle),
                "Expected >1.0 dynamic probability to clamp to 1.0 for seed " + seed);
        }
    }

    @Test
    void guillotineFailsPreconditionWhenAttackerLevelIsLowerThanDefender()
    {
        IMove guillotine = MoveRegistry.get("Guillotine");
        Battler lowerAtk = battler("Onix", Element.ROCK, 200, 200, 40, 70);
        Battler higherDef = battler("Snorlax", Element.NORMAL, 250, 250, 50, 30);
        Battle battle = new Battle(lowerAtk, higherDef, 4110L);

        String out = captureStdout(() -> guillotine.execute(battle));

        assertTrue(out.contains("  But it failed!"),
            "Expected precondition failure message when attacker level < defender level");
        assertEquals(250, higherDef.getCurrentHp(),
            "Defender HP must remain unchanged when Guillotine precondition fails");
        assertFalse(higherDef.isFainted());
    }

    @Test
    void guillotineUsesThirtyPercentAccuracyAtEqualLevels()
    {
        IMove guillotine = MoveRegistry.get("Guillotine");

        // Seed 4110L: first nextDouble() < 0.30 -> hits and KOs equal-level defender
        Battler atkHit = battler("Onix", Element.ROCK, 200, 200, 50, 70);
        Battler defHit = battler("Snorlax", Element.NORMAL, 250, 250, 50, 30);
        guillotine.execute(new Battle(atkHit, defHit, 4110L));
        assertEquals(0, defHit.getCurrentHp());
        assertTrue(defHit.isFainted());

        // Seed 6439L: first nextDouble() ~0.301 >= 0.30 -> misses equal-level defender
        Battler atkMiss = battler("Onix", Element.ROCK, 200, 200, 50, 70);
        Battler defMiss = battler("Snorlax", Element.NORMAL, 250, 250, 50, 30);
        String missOut = captureStdout(() -> guillotine.execute(new Battle(atkMiss, defMiss, 6439L)));
        assertEquals(250, defMiss.getCurrentHp());
        assertFalse(defMiss.isFainted());
        assertTrue(missOut.contains("missed"));
    }

    @Test
    void guillotineScalesAccuracyWithLevelAdvantage()
    {
        IMove guillotine = MoveRegistry.get("Guillotine");

        // Lv.70 vs Lv.50 -> accuracy = (70 - 50 + 30) / 100 = 0.50
        // Seed 6439L (~0.301) missed at 0.30, so hitting here proves scaled 0.50 threshold
        Battler higherAtk = battler("Onix", Element.ROCK, 200, 200, 70, 70);
        Battler lowerDef = battler("Snorlax", Element.NORMAL, 250, 250, 50, 30);
        guillotine.execute(new Battle(higherAtk, lowerDef, 6439L));

        assertEquals(0, lowerDef.getCurrentHp());
        assertTrue(lowerDef.isFainted());
    }

    @Test
    void venoshockPowerEvaluatesTo65NormallyAndDoublesTo130AgainstEitherPoisonStatus()
    {
        // Ensures Venoshock is registered in MoveRegistry
        assertNotNull(MoveRegistry.get("Venoshock"));

        ConditionalNumber venoshockPower = new ConditionalNumber(
            new For(new Defender(), new Or<>(new IsPoisoned(), new IsBadlyPoisoned())),
            new Exactly(130),
            new Exactly(65)
        );

        Battler attacker = battler("Muk", Element.POISON, 200, 200, 50, 70);

        Battler cleanDef = battler("Target", Element.NORMAL, 200, 200, 50, 60);
        assertEquals(65.0, venoshockPower.evaluate(new Battle(attacker, cleanDef, 42L)));

        Battler poisonedDef = battler("Target", Element.NORMAL, 200, 200, 50, 60);
        poisonedDef.applyMajorStatus(MajorStatus.POISONED);
        assertEquals(130.0, venoshockPower.evaluate(new Battle(attacker, poisonedDef, 42L)));

        Battler badlyPoisonedDef = battler("Target", Element.NORMAL, 200, 200, 50, 60);
        badlyPoisonedDef.applyMajorStatus(MajorStatus.BADLY_POISONED);
        assertEquals(130.0, venoshockPower.evaluate(new Battle(attacker, badlyPoisonedDef, 42L)));

        Battler burnedDef = battler("Target", Element.NORMAL, 200, 200, 50, 60);
        burnedDef.applyMajorStatus(MajorStatus.BURNED);
        assertEquals(65.0, venoshockPower.evaluate(new Battle(attacker, burnedDef, 42L)));

        Battler paralyzedDef = battler("Target", Element.NORMAL, 200, 200, 50, 60);
        paralyzedDef.applyMajorStatus(MajorStatus.PARALYZED);
        assertEquals(65.0, venoshockPower.evaluate(new Battle(attacker, paralyzedDef, 42L)));
    }

    @Test
    void venoshockExecuteDoublesDamageAgainstPoisonedAndBadlyPoisonedDefenders()
    {
        IMove venoshock = MoveRegistry.get("Venoshock");

        // Unstatused defender: base power 65 -> 29 damage on seed 42L
        Battler atkClean = battler("User", Element.NORMAL, 200, 200, 50, 80);
        Battler defClean = battler("Target", Element.NORMAL, 200, 200, 50, 70);
        venoshock.execute(new Battle(atkClean, defClean, 42L));
        int cleanDamage = 200 - defClean.getCurrentHp();

        // POISONED defender: doubled power 130 -> 56 damage on seed 42L
        Battler atkPoisoned = battler("User", Element.NORMAL, 200, 200, 50, 80);
        Battler defPoisoned = battler("Target", Element.NORMAL, 200, 200, 50, 70);
        defPoisoned.applyMajorStatus(MajorStatus.POISONED);
        venoshock.execute(new Battle(atkPoisoned, defPoisoned, 42L));
        int poisonedDamage = 200 - defPoisoned.getCurrentHp();

        // BADLY_POISONED defender: doubled power 130 -> 56 damage on seed 42L
        Battler atkToxic = battler("User", Element.NORMAL, 200, 200, 50, 80);
        Battler defToxic = battler("Target", Element.NORMAL, 200, 200, 50, 70);
        defToxic.applyMajorStatus(MajorStatus.BADLY_POISONED);
        venoshock.execute(new Battle(atkToxic, defToxic, 42L));
        int badlyPoisonedDamage = 200 - defToxic.getCurrentHp();

        // BURNED defender: remains at base power 65 -> 29 damage on seed 42L
        Battler atkBurned = battler("User", Element.NORMAL, 200, 200, 50, 80);
        Battler defBurned = battler("Target", Element.NORMAL, 200, 200, 50, 70);
        defBurned.applyMajorStatus(MajorStatus.BURNED);
        venoshock.execute(new Battle(atkBurned, defBurned, 42L));
        int burnedDamage = 200 - defBurned.getCurrentHp();

        assertEquals(29, cleanDamage);
        assertEquals(56, poisonedDamage);
        assertEquals(56, badlyPoisonedDamage);
        assertEquals(29, burnedDamage);
    }
}
