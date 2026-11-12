package com.pokemon.conditions;

import com.pokemon.api.ICondition;

/** Passes if EITHER sub-condition passes. */

public class Or<T> implements ICondition<T>
{
    private final ICondition<T> left, right;

    public Or(ICondition<T> left, ICondition<T> right)
    {
        this.left = left; this.right = right;
    }

    @Override
    public boolean check(T input)
    {
        return left.check(input) || right.check(input);
    }
}
