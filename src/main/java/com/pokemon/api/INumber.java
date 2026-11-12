package com.pokemon.api;
import com.pokemon.battle.Battle;

/** A lazily-evaluated number. May depend on battle state at execute time. */
public interface INumber
{
    double evaluate(Battle battle);
}
