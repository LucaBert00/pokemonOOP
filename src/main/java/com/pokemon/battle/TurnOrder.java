package com.pokemon.battle;

import com.pokemon.api.IMove;
import java.util.Objects;
import java.util.Random;

/**
 * Pure per-round turn-order resolver.
 * Orders combatants by:
 *   1. Higher {@link IMove#priority()} first
 *   2. Higher {@link Battler#getEffectiveSpeed()} first
 *   3. Exact speed ties broken via {@link Random#nextBoolean()}
 */
public final class TurnOrder
{
    public record RoundPlan(Battler first, IMove firstMove, Battler second, IMove secondMove)
    {
        public Battle createBattle(Random random)
        {
            return new Battle(first, second, random);
        }
    }

    private TurnOrder()
    {
    }

    public static RoundPlan resolve(Battler a, IMove moveA, Battler b, IMove moveB, Random random)
    {
        Objects.requireNonNull(a, "a must not be null");
        Objects.requireNonNull(moveA, "moveA must not be null");
        Objects.requireNonNull(b, "b must not be null");
        Objects.requireNonNull(moveB, "moveB must not be null");
        Objects.requireNonNull(random, "random must not be null");

        return firstActsBeforeSecond(a, moveA, b, moveB, random)
            ? new RoundPlan(a, moveA, b, moveB)
            : new RoundPlan(b, moveB, a, moveA);
    }

    public static boolean firstActsBeforeSecond(Battler a, IMove moveA, Battler b, IMove moveB, Random random)
    {
        int priorityComparison = Integer.compare(moveA.priority(), moveB.priority());
        if (priorityComparison != 0)
        {
            return priorityComparison > 0;
        }

        int speedComparison = Integer.compare(a.getEffectiveSpeed(), b.getEffectiveSpeed());
        if (speedComparison != 0)
        {
            return speedComparison > 0;
        }

        return random.nextBoolean();
    }
}
