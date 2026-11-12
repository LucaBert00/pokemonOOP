package com.pokemon.conditions;

import com.pokemon.api.ICondition;

/** Inverts the result of the wrapped condition. */

public class Not<T> implements ICondition<T>
{
    private final ICondition<T> inner;

    public Not(ICondition<T> inner)
    {
        this.inner = inner;
    }

    @Override
    public boolean check(T input)
    {
        return !inner.check(input);
    }
}
