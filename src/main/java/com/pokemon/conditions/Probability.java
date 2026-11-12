package com.pokemon.conditions;

import com.pokemon.api.ICondition;

/**
 * Returns true with the given probability (0.0 – 1.0).
 * Generic so it can be used as ICondition<Battle>, ICondition<Battler>, etc.
 * The input is completely ignored — only the random roll matters.
 */

public class Probability<T> implements ICondition<T>
{
    private final double chance;

    public Probability(double chance)
    {
        this.chance = chance;
    }

    @Override
    public boolean check(T input)
    {
        return Math.random() < chance;
    }
}
