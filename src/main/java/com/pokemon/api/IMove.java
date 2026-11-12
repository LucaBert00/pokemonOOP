package com.pokemon.api;
import com.pokemon.battle.Battle;

/** Root of the move hierarchy. Execute mutates battle state. */
public interface IMove
{
    void execute(Battle battle);
}
