package com.pokemon.conditions;

import com.pokemon.api.ICondition;
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
    private final Random fallbackRandom;

    public Probability(double chance)
    {
        this(chance, new Random());
    }

    public Probability(double chance, Random fallbackRandom)
    {
        this.chance = chance;
        this.fallbackRandom = Objects.requireNonNull(fallbackRandom, "fallbackRandom must not be null");
    }

    @Override
    public boolean check(T input)
    {
        Random rng = (input instanceof Battle battle) ? battle.getRandom() : fallbackRandom;
        return rng.nextDouble() < chance;
    }
}