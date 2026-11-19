package com.pokemon.battle;

import com.pokemon.BattleEngine;
import com.pokemon.api.IMove;
import com.pokemon.attempts.Attempt;
import com.pokemon.conditions.Probability;
import com.pokemon.data.MoveRegistry;
import com.pokemon.data.PokemonLoader;
import com.pokemon.effects.BadlyPoison;
import com.pokemon.effects.Burn;
import com.pokemon.effects.FormulaDamage;
import com.pokemon.effects.MoveCategory;
import com.pokemon.effects.NoEffect;
import com.pokemon.effects.Poison;
import com.pokemon.effects.Sleep;
import com.pokemon.moves.Move;
import com.pokemon.numbers.Exactly;
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
 * End-to-end multi-turn regression suite verifying Fase 2 mechanics (#16-#23):
 * Level-50 stat scaling, Physical/Special splits, move priority & speed ordering,
 * status mutual exclusivity, sleep countdown/wake-up, end-of-round residual ticks
 * (Burn, Poison, Toxic N/16 ramp), and video DSL moves (PainSplit, Counter,
 * SelfDestruct, Endeavor).
 */
class Fase2SimulationTest
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

    private static IMove noopMove(String name)
    {
        return new Move(name, Element.NORMAL, new Attempt(name, new Probability<>(1.0), new NoEffect()));
    }

    @Test
    void level50StatFormulaMatchesPokemonLoaderRoster()
    {
        List<Battler> roster = PokemonLoader.loadAll();
        assertFalse(roster.isEmpty(), "pokemon.csv roster must not be empty");

        // Pikachu base stats in pokemon.csv: HP 35, Atk 55, Def 40, SpA 50, SpD 50, Spe 90
        // Level 50 formulas: HP = floor(2 * base * 50 / 100) + 60 = base + 60;
        //                    Other = floor(2 * base * 50 / 100) + 5 = base + 5
        Battler pikachu = roster.stream()
            .filter(b -> b.getName().equals("Pikachu"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Pikachu missing from pokemon.csv"));

        assertEquals(50, pikachu.getLevel());
        assertEquals(95, pikachu.getMaxHp());
        assertEquals(60, pikachu.getAttack());
        assertEquals(45, pikachu.getDefense());
        assertEquals(55, pikachu.getSpAttack());
        assertEquals(55, pikachu.getSpDefense());
        assertEquals(95, pikachu.getSpeed());
    }

    @Test
    void physicalAndSpecialMovesRouteToRespectiveCombatStats()
    {
        IMove physicalHit = new Move(
            "PhysTest",
            Element.NORMAL,
            new Attempt("PhysTest", new Probability<>(1.0), new FormulaDamage(new Exactly(80), MoveCategory.PHYSICAL))
        );
        IMove specialHit = new Move(
            "SpecTest",
            Element.NORMAL,
            new Attempt("SpecTest", new Probability<>(1.0), new FormulaDamage(new Exactly(80), MoveCategory.SPECIAL))
        );

        // Attacker: high SpAttack (150), low Attack (50)
        // Defender: high Defense (150), low SpDefense (50)
        Battler attacker = new Battler("Alakazam", List.of(Element.PSYCHIC), 300, 50, 50, 60, 150, 85, 120, List.of(physicalHit, specialHit));
        Battler physTarget = new Battler("PhysWall", List.of(Element.NORMAL), 300, 50, 80, 150, 60, 50, 50, List.of(noopMove("Idle")));
        Battler specTarget = new Battler("SpecWall", List.of(Element.NORMAL), 300, 50, 80, 150, 60, 50, 50, List.of(noopMove("Idle")));

        physicalHit.execute(new Battle(attacker, physTarget, 42L));
        specialHit.execute(new Battle(attacker, specTarget, 42L));

        int physicalDamage = 300 - physTarget.getCurrentHp();
        int specialDamage = 300 - specTarget.getCurrentHp();

        assertTrue(specialDamage > physicalDamage * 5,
            "Special move (150 SpA vs 50 SpD) must deal vastly more damage than Physical (50 Atk vs 150 Def): spec="
                + specialDamage + ", phys=" + physicalDamage);
    }

    @Test
    void movePriorityAndSpeedOrderAcrossQuickAttackNormalAndCounter()
    {
        IMove quickAttack = MoveRegistry.get("QuickAttack"); // priority +1
        IMove tackle = MoveRegistry.get("Tackle");           // priority 0
        IMove counter = MoveRegistry.get("Counter");         // priority -5

        Battler slowQuickUser = new Battler("SlowPikachu", List.of(Element.ELECTRIC), 200, 50, 60, 60, 60, 60, 40, List.of(quickAttack));
        Battler fastNormalUser = new Battler("FastGengar", List.of(Element.POISON), 200, 50, 60, 60, 60, 60, 130, List.of(tackle, counter));
        Random rng = new Random(42L);

        // 1. Priority +1 (QuickAttack, speed 40) beats Priority 0 (Tackle, speed 130)
        TurnOrder.RoundPlan plan1 = TurnOrder.resolve(slowQuickUser, quickAttack, fastNormalUser, tackle, rng);
        assertSame(slowQuickUser, plan1.first());
        assertSame(fastNormalUser, plan1.second());

        // 2. Priority 0 (Tackle, speed 40) beats Priority -5 (Counter, speed 130)
        TurnOrder.RoundPlan plan2 = TurnOrder.resolve(slowQuickUser, tackle, fastNormalUser, counter, rng);
        assertSame(slowQuickUser, plan2.first());
        assertSame(fastNormalUser, plan2.second());
    }

    @Test
    void statusMutualExclusivityAndSleepCountdownWakeUp()
    {
        IMove sleepMove = new Move("HypnoTest", Element.PSYCHIC, new Attempt("HypnoTest", new Probability<>(1.0), new Sleep(new Defender())));
        IMove wakeStrike = new Move(
            "WakePunch",
            Element.NORMAL,
            new Attempt("WakePunch", new Probability<>(1.0), new FormulaDamage(new Exactly(50), MoveCategory.PHYSICAL))
        );
        IMove idle = noopMove("Wait");

        Battler sleeper = new Battler("Snorlax", List.of(Element.NORMAL), 300, 50, 110, 65, 65, 110, 90, List.of(wakeStrike, idle));
        Battler controller = new Battler("Gengar", List.of(Element.POISON), 300, 50, 65, 60, 130, 75, 50, List.of(idle));

        // Apply Poison first; subsequent Burn must be rejected due to mutual exclusivity
        new Poison(new Defender()).apply(new Battle(controller, sleeper, 42L));
        assertEquals(MajorStatus.POISONED, sleeper.getMajorStatus());

        new Burn(new Defender()).apply(new Battle(controller, sleeper, 42L));
        assertEquals(MajorStatus.POISONED, sleeper.getMajorStatus(), "Burn must not overwrite existing Poison");
        assertFalse(sleeper.isBurned());

        // Clear status, then inflict Sleep and simulate multi-turn countdown + wake-up in BattleEngine
        sleeper.clearMajorStatus();
        sleepMove.execute(new Battle(controller, sleeper, 42L));
        assertEquals(MajorStatus.ASLEEP, sleeper.getMajorStatus());

        int initialSleepTurns = sleeper.getSleepTurnsRemaining();
        assertTrue(initialSleepTurns >= 1 && initialSleepTurns <= 3);

        // Feed (initialSleepTurns + 1) rounds of move "1" (WakePunch) for sleeper;
        // sleeper skips initialSleepTurns rounds, then wakes up and deals damage on the final round
        StringBuilder script = new StringBuilder();
        for (int i = 0; i <= initialSleepTurns; i++)
        {
            script.append("1\n");
        }
        // Extra safety inputs in case battle continues past wake-up round until we stop or KO
        Battler fragileTarget = new Battler("Target", List.of(Element.NORMAL), 10, 50, 50, 50, 50, 50, 20, List.of(idle));
        Scanner scanner = new Scanner(new ByteArrayInputStream(script.toString().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);

        String output = captureStdOut(() -> new BattleEngine(sleeper, fragileTarget, scanner, 42L).run());

        assertFalse(sleeper.isAsleep(), "Sleeper must wake up after sleep counter expires");
        assertTrue(fragileTarget.isFainted(), "Woken battler must execute WakePunch and KO the 10-HP target");
        assertTrue(output.contains("Snorlax is fast asleep!"));
        assertTrue(output.contains("Snorlax woke up!"));
    }

    @Test
    void endOfRoundResidualDamageAppliesPoisonBurnAndEscalatingToxic()
    {
        IMove idle = noopMove("Wait");

        // 160 max HP -> 1/16 = 10 HP per tick
        Battler burnedMon = new Battler("BurnedMon", List.of(Element.NORMAL), 160, 50, 50, 50, 50, 50, 80, List.of(idle));
        Battler poisonedMon = new Battler("PoisonedMon", List.of(Element.NORMAL), 160, 50, 50, 50, 50, 50, 70, List.of(idle));
        new Burn(new Defender()).apply(new Battle(poisonedMon, burnedMon, 42L));
        new Poison(new Defender()).apply(new Battle(burnedMon, poisonedMon, 42L));

        // Run 2 rounds of Burn vs. Poison: each loses exactly 10 HP per round -> 140 HP after 2 rounds
        // Give both 20 HP total so both faint at the end of round 2 (160 - 140 = 20 -> 10 -> 0)
        burnedMon.takeDamage(140);
        poisonedMon.takeDamage(140);
        Scanner twoRoundScanner = new Scanner(new ByteArrayInputStream("1\n1\n".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        String burnPoisonOut = captureStdOut(() -> new BattleEngine(burnedMon, poisonedMon, twoRoundScanner, 42L).run());

        assertEquals(0, burnedMon.getCurrentHp());
        assertEquals(0, poisonedMon.getCurrentHp());
        assertTrue(burnPoisonOut.contains("BurnedMon is hurt by its burn! (-10 HP)"));
        assertTrue(burnPoisonOut.contains("PoisonedMon is hurt by poison! (-10 HP)"));

        // Toxic escalation: 160 max HP -> 10 (1/16), then 20 (2/16), then 30 (3/16) = 60 total HP over 3 rounds
        Battler toxicVictim = new Battler("ToxicVictim", List.of(Element.NORMAL), 160, 50, 50, 50, 50, 50, 80, List.of(idle));
        Battler healthySponger = new Battler("Sponger", List.of(Element.NORMAL), 500, 50, 50, 50, 50, 50, 70, List.of(idle));
        new BadlyPoison(new Defender()).apply(new Battle(healthySponger, toxicVictim, 42L));
        assertEquals(1, toxicVictim.getToxicCounter());

        // Set victim to 60 HP so 10 + 20 + 30 = 60 HP faints it cleanly on round 3
        toxicVictim.takeDamage(100);
        Scanner toxicScanner = new Scanner(new ByteArrayInputStream("1\n1\n1\n".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        String toxicOut = captureStdOut(() -> new BattleEngine(toxicVictim, healthySponger, toxicScanner, 42L).run());

        assertEquals(0, toxicVictim.getCurrentHp());
        assertTrue(toxicVictim.isFainted());
        assertTrue(toxicOut.contains("ToxicVictim is badly hurt by poison! (-10 HP)"));
        assertTrue(toxicOut.contains("ToxicVictim is badly hurt by poison! (-20 HP)"));
        assertTrue(toxicOut.contains("ToxicVictim is badly hurt by poison! (-30 HP)"));
    }

    @Test
    void videoDslMovesIntegrateAcrossPainSplitCounterEndeavorAndSelfDestruct()
    {
        IMove painSplit = MoveRegistry.get("PainSplit");
        IMove counter = MoveRegistry.get("Counter");
        IMove endeavor = MoveRegistry.get("Endeavor");
        IMove selfDestruct = MoveRegistry.get("SelfDestruct");
        IMove tackle = MoveRegistry.get("Tackle");

        // 1. PainSplit: averages 40 HP and 160 HP -> both become 100 HP
        Battler lowUser = new Battler("Misdreavus", List.of(Element.GHOST), 200, 50, 60, 60, 85, 85, 85, List.of(painSplit));
        lowUser.takeDamage(160); // 40 HP
        Battler highTarget = new Battler("Snorlax", List.of(Element.NORMAL), 200, 50, 110, 65, 65, 110, 30, List.of(tackle));
        highTarget.takeDamage(40); // 160 HP
        painSplit.execute(new Battle(lowUser, highTarget, 42L));
        assertEquals(100, lowUser.getCurrentHp());
        assertEquals(100, highTarget.getCurrentHp());

        // 2. Counter (priority -5): faster user still moves second and doubles incoming physical damage
        Battler fastCounterUser = new Battler("Wobbuffet", List.of(Element.PSYCHIC), 300, 50, 33, 58, 33, 58, 120, List.of(counter));
        Battler slowerAttacker = new Battler("Tauros", List.of(Element.NORMAL), 300, 50, 100, 95, 40, 70, 80, List.of(tackle));
        Scanner counterScanner = new Scanner(new ByteArrayInputStream("1\n".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        // Run 1 round via BattleEngine by letting a 1-round helper or direct round plan execute:
        TurnOrder.RoundPlan counterPlan = TurnOrder.resolve(fastCounterUser, counter, slowerAttacker, tackle, new Random(42L));
        assertSame(slowerAttacker, counterPlan.first(), "Priority -5 Counter must move after Priority 0 Tackle");
        Battle counterBattle = counterPlan.createBattle(new Random(42L));
        counterPlan.firstMove().execute(counterBattle);
        int damageTakenByWobbuffet = 300 - fastCounterUser.getCurrentHp();
        assertTrue(damageTakenByWobbuffet > 0);
        counterBattle.flipTurn();
        counterPlan.secondMove().execute(counterBattle);
        assertEquals(300 - (damageTakenByWobbuffet * 2), slowerAttacker.getCurrentHp(),
            "Counter must reflect exact 2x LastDamageDealt");

        // 3. Endeavor: lowers defender HP to match attacker's lower HP (35 HP)
        Battler endeavorUser = new Battler("Rattata", List.of(Element.NORMAL), 150, 50, 56, 35, 25, 35, 72, List.of(endeavor));
        endeavorUser.takeDamage(115); // 35 HP
        Battler healthyFoe = new Battler("Blissey", List.of(Element.NORMAL), 300, 50, 10, 10, 75, 135, 55, List.of(tackle));
        endeavor.execute(new Battle(endeavorUser, healthyFoe, 42L));
        assertEquals(35, healthyFoe.getCurrentHp());

        // 4. SelfDestruct: deals massive damage to target and faints the user
        Battler bomber = new Battler("Electrode", List.of(Element.ELECTRIC), 150, 50, 100, 70, 80, 80, 140, List.of(selfDestruct));
        Battler sponge = new Battler("Chansey", List.of(Element.NORMAL), 400, 50, 5, 5, 35, 105, 50, List.of(tackle));
        selfDestruct.execute(new Battle(bomber, sponge, 42L));
        assertTrue(bomber.isFainted(), "SelfDestruct must faint the user via Faint(ATK)");
        assertTrue(sponge.getCurrentHp() < 400, "SelfDestruct must deal damage to the defender");
    }
}
