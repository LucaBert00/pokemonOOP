package com.pokemon.api;

/** Generic predicate. T can be Battle, Battler, or any context type. */
public interface ICondition<T>
{
    boolean check(T input);
}
