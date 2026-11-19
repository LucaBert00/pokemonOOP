package com.pokemon.battle;

import com.pokemon.BattleEngine;
import com.pokemon.api.IMove;
import com.pokemon.conditions.For;
import com.pokemon.conditions.HasElement;
import com.pokemon.conditions.HasMajorStatus;
import com.pokemon.conditions.Not;
import com.pokemon.data.MoveRegistry;
import com.pokemon.effects.Paralysis;
import com.pokemon.moves.WithApplicability;
import com.pokemon.moves.WithPrecondition;
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

class TurnOrderTest
{
    private static Battler battler(String name, int speed, List<IMove> moves)
    {
        return new Battler(name, List.of(Element.NORMAL), 500, 50, 60, 60, 60, 60, speed, moves);
    }

    @Test
    void slowerBattlerWithQuickAttackActsBeforeFasterBattlerWithZeroPriorityMove()
    {
        IMove quickAttack = MoveRegistry.get("QuickAttack");
        IMove tackle      = MoveRegistry.get("Tackle");

        assertEquals(1, quickAttack.priority(), "QuickAttack must have priority +1");
        assertEquals(0, tackle.priority(), "Tackle must default to priority 0");

        Battler slowQuickUser = battler("SlowPikachu", 60, List.of(quickAttack));
        Battler fastOpponent  = battler("FastGengar", 120, List.of(tackle));

        // Seed 42L: priority comparison resolves order deterministically before RNG tie-break
        Random rng = new Random(42L);
        TurnOrder.RoundPlan plan = TurnOrder.resolve(slowQuickUser, quickAttack, fastOpponent, tackle, rng);
        Battle roundBattle = plan.createBattle(rng);

        assertSame(slowQuickUser, plan.first());
        assertSame(fastOpponent, plan.second());
        assertSame(slowQuickUser, roundBattle.getAttacker());
        assertSame(fastOpponent, roundBattle.getDefender());
    }

    @Test
    void paralysisHalvesEffectiveSpeedAndReversesSubsequentRoundOrderAndDecoratorsDelegatePriority()
    {
        IMove quickAttack = MoveRegistry.get("QuickAttack");
        IMove tackle      = MoveRegistry.get("Tackle");

        // Verify WithPrecondition and WithApplicability delegate priority() to the wrapped IMove
        IMove withPrecondition = new WithPrecondition(
            new Not<>(new For(new Defender(), new HasMajorStatus())),
            quickAttack,
            "Failed!"
        );
        IMove withApplicability = new WithApplicability(
            new HasElement(Element.NORMAL),
            withPrecondition
        );
        assertEquals(1, withPrecondition.priority());
        assertEquals(1, withApplicability.priority());

        // Initially FastAce (speed 110) outspeeds SteadyMon (speed 80)
        Battler fastAce   = battler("FastAce", 110, List.of(tackle));
        Battler steadyMon = battler("SteadyMon", 80, List.of(tackle));

        // Seed 42L: standard deterministic seed; no tie occurs before or after paralysis
        Random rng = new Random(42L);
        TurnOrder.RoundPlan round1 = TurnOrder.resolve(fastAce, tackle, steadyMon, tackle, rng);
        assertSame(fastAce, round1.first());
        assertEquals(110, fastAce.getEffectiveSpeed());

        // Inflict Paralysis on FastAce -> effective speed drops from 110 to 110 / 2 = 55 (< 80)
        new Paralysis(new Defender()).apply(new Battle(steadyMon, fastAce, 42L));
        assertTrue(fastAce.isParalyzed());
        assertEquals(55, fastAce.getEffectiveSpeed());
        assertEquals(80, steadyMon.getEffectiveSpeed());

        // Next round's order must reverse so SteadyMon acts first
        TurnOrder.RoundPlan round2 = TurnOrder.resolve(fastAce, tackle, steadyMon, tackle, rng);
        Battle round2Battle = round2.createBattle(rng);
        assertSame(steadyMon, round2.first());
        assertSame(fastAce, round2.second());
        assertSame(steadyMon, round2Battle.getAttacker());
        assertSame(fastAce, round2Battle.getDefender());
    }

    @Test
    void battleFlipTurnSwapsAttackerAndDefenderAndSecondCallRestoresThem()
    {
        Battler first  = battler("First", 90, List.of(MoveRegistry.get("Tackle")));
        Battler second = battler("Second", 70, List.of(MoveRegistry.get("Tackle")));

        Battle battle = new Battle(first, second, 42L);
        assertSame(first, battle.getAttacker());
        assertSame(second, battle.getDefender());

        battle.flipTurn();
        assertSame(second, battle.getAttacker());
        assertSame(first, battle.getDefender());

        battle.flipTurn();
        assertSame(first, battle.getAttacker());
        assertSame(second, battle.getDefender());
    }

    @Test
    void exactSpeedTiesBreakDeterministicallyWithSharedSeededRandom()
    {
        IMove tackle = MoveRegistry.get("Tackle");
        Battler a = battler("A", 80, List.of(tackle));
        Battler b = battler("B", 80, List.of(tackle));

        // Seed 1L: new Random(1L).nextBoolean() == true (first roll >= 0.5), so candidate A wins tie
        TurnOrder.RoundPlan planSeed1 = TurnOrder.resolve(a, tackle, b, tackle, new Random(1L));
        assertSame(a, planSeed1.first());

        // Seed 8096L: new Random(8096L).nextBoolean() == false (first roll ~0.093 < 0.5), so candidate B wins tie
        TurnOrder.RoundPlan planSeed8096 = TurnOrder.resolve(a, tackle, b, tackle, new Random(8096L));
        assertSame(b, planSeed8096.first());
    }

    @Test
    void faintingOnFirstHalfTurnEndsRoundImmediatelyWithoutSecondMoverActing()
    {
        IMove quickAttack = MoveRegistry.get("QuickAttack");
        IMove tackle      = MoveRegistry.get("Tackle");

        // Slower player with 1-HP faster opponent; QuickAttack KO prevents opponent from acting
        // Gengar's Poison type keeps Normal damage neutral (Normal is immune to Ghost).
        Battler player = new Battler("Pikachu", List.of(Element.ELECTRIC), 200, 50, 90, 60, 60, 60, 60, List.of(quickAttack));
        Battler fragileCpu = new Battler("FragileGengar", List.of(Element.POISON), 1, 50, 120, 30, 120, 30, 120, List.of(tackle));

        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try
        {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            Scanner scanner = new Scanner(new ByteArrayInputStream("1\n".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
            new BattleEngine(player, fragileCpu, scanner, 42L).run();
        }
        finally
        {
            System.setOut(originalOut);
        }

        assertTrue(fragileCpu.isFainted());
        assertEquals(player.getMaxHp(), player.getCurrentHp(), "Fainted second mover must not execute its move");
    }
}
