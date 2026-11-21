package com.pokemon.conditions;

import com.pokemon.api.ICondition;
import com.pokemon.api.INumber;
import com.pokemon.battle.Battle;
import java.util.Objects;
import java.util.Random;

/**
 * Returns true with the given probability (0.0 - 1.0).
 * Generic so it can be used as ICondition&lt;Battle&gt;, ICondition&lt;Battler&gt;, etc.
 * When evaluated against a {@link Battle}, draws from {@link Battle#getRandom()};
 * otherwise uses a fallback {@link Random} instance.
 */
public class Probability<T> implements ICondition<T>
{
    private final double chance;
    private final INumber chanceNumber;
    private final Random fallbackRandom;

    public Probability(double chance)
    {
        this(chance, new Random());
    }

    public Probability(double chance, Random fallbackRandom)
    {
        this.chance = chance;
        this.chanceNumber = null;
        this.fallbackRandom = Objects.requireNonNull(fallbackRandom, "fallbackRandom must not be null");
    }

    public Probability(INumber chanceNumber)
    {
        this(chanceNumber, new Random());
    }

    public Probability(INumber chanceNumber, Random fallbackRandom)
    {
        this.chance = 0.0;
        this.chanceNumber = Objects.requireNonNull(chanceNumber, "chanceNumber must not be null");
        this.fallbackRandom = Objects.requireNonNull(fallbackRandom, "fallbackRandom must not be null");
    }

    @Override
    public boolean check(T input)
    {
        if (chanceNumber != null)
        {
            if (!(input instanceof Battle battle))
            {
                throw new IllegalArgumentException("Dynamic Probability(INumber) requires a Battle input");
            }
            double effectiveChance = Math.clamp(chanceNumber.evaluate(battle), 0.0, 1.0);
            return battle.getRandom().nextDouble() < effectiveChance;
        }
        Random rng = (input instanceof Battle battle) ? battle.getRandom() : fallbackRandom;
        return rng.nextDouble() < chance;
    }
}
