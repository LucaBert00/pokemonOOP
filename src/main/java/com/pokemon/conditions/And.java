package com.pokemon.conditions;

import com.pokemon.api.ICondition;

/** Passes only if BOTH sub-conditions pass. */

public class And<T> implements ICondition<T>
{
    private final ICondition<T> left, right;

    public And(ICondition<T> left, ICondition<T> right)
    {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean check(T input)
    {
        return left.check(input) && right.check(input);
    }
}
