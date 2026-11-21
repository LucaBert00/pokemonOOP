package com.pokemon.conditions;

import com.pokemon.api.ICondition;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;
import com.pokemon.battle.Element;
import com.pokemon.battle.MajorStatus;
import com.pokemon.targets.Defender;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusConditionsTest
{
    private static Battler battlerWithStatus(MajorStatus status)
    {
        Battler battler = new Battler("Subject", List.of(Element.NORMAL), 150, 50, 60, 60, 60, 60, 60, List.of());
        if (status != MajorStatus.NONE)
        {
            assertTrue(battler.applyMajorStatus(status), "Failed to apply " + status);
        }
        return battler;
    }

    private static void assertMatchesOnlyStatus(ICondition<Battler> condition, MajorStatus expectedStatus)
    {
        assertAll(
            java.util.Arrays.stream(MajorStatus.values()).map(status -> () -> {
                Battler subject = battlerWithStatus(status);
                boolean expected = (status == expectedStatus);
                assertEquals(
                    expected,
                    condition.check(subject),
                    () -> condition.getClass().getSimpleName() + " for status " + status
                );
            })
        );
    }

    @Test
    void isParalyzedMatchesOnlyParalyzed()
    {
        assertMatchesOnlyStatus(new IsParalyzed(), MajorStatus.PARALYZED);
    }

    @Test
    void isBurnedMatchesOnlyBurned()
    {
        assertMatchesOnlyStatus(new IsBurned(), MajorStatus.BURNED);
    }

    @Test
    void isAsleepMatchesOnlyAsleep()
    {
        assertMatchesOnlyStatus(new IsAsleep(), MajorStatus.ASLEEP);
    }

    @Test
    void isFrozenMatchesOnlyFrozen()
    {
        assertMatchesOnlyStatus(new IsFrozen(), MajorStatus.FROZEN);
    }

    @Test
    void isPoisonedMatchesOnlyPoisoned()
    {
        assertMatchesOnlyStatus(new IsPoisoned(), MajorStatus.POISONED);
    }

    @Test
    void isBadlyPoisonedMatchesOnlyBadlyPoisoned()
    {
        assertMatchesOnlyStatus(new IsBadlyPoisoned(), MajorStatus.BADLY_POISONED);
    }

    @Test
    void orEvaluatesCompleteFourCaseBooleanTruthTable()
    {
        ICondition<String> alwaysTrue = s -> true;
        ICondition<String> alwaysFalse = s -> false;

        assertAll(
            () -> assertFalse(new Or<>(alwaysFalse, alwaysFalse).check("x"), "(F, F) -> F"),
            () -> assertTrue(new Or<>(alwaysTrue, alwaysFalse).check("x"), "(T, F) -> T"),
            () -> assertTrue(new Or<>(alwaysFalse, alwaysTrue).check("x"), "(F, T) -> T"),
            () -> assertTrue(new Or<>(alwaysTrue, alwaysTrue).check("x"), "(T, T) -> T")
        );
    }

    @Test
    void orShortCircuitsRightOperandWhenLeftOperandIsTrue()
    {
        AtomicInteger rightCalls = new AtomicInteger(0);
        ICondition<String> trackingRight = s -> {
            rightCalls.incrementAndGet();
            return true;
        };

        // When left is true, right must not be evaluated
        assertTrue(new Or<>(s -> true, trackingRight).check("x"));
        assertEquals(0, rightCalls.get(), "Or must short-circuit when left condition is true");

        // When left is false, right must be evaluated once
        assertTrue(new Or<>(s -> false, trackingRight).check("x"));
        assertEquals(1, rightCalls.get(), "Or must evaluate right condition when left condition is false");
    }

    @Test
    void forDefenderOrPoisonedOrBadlyPoisonedMatchesBothPoisonStatesOnly()
    {
        ICondition<Battle> defenderPoisoned = new For(
            new Defender(),
            new Or<>(new IsPoisoned(), new IsBadlyPoisoned())
        );
        Battler attacker = battlerWithStatus(MajorStatus.NONE);

        assertFalse(defenderPoisoned.check(new Battle(attacker, battlerWithStatus(MajorStatus.NONE), 42L)));
        assertTrue(defenderPoisoned.check(new Battle(attacker, battlerWithStatus(MajorStatus.POISONED), 42L)));
        assertTrue(defenderPoisoned.check(new Battle(attacker, battlerWithStatus(MajorStatus.BADLY_POISONED), 42L)));
        assertFalse(defenderPoisoned.check(new Battle(attacker, battlerWithStatus(MajorStatus.BURNED), 42L)));
        assertFalse(defenderPoisoned.check(new Battle(attacker, battlerWithStatus(MajorStatus.PARALYZED), 42L)));
    }
}
