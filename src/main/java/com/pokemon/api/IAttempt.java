package com.pokemon.api;
import com.pokemon.battle.Battle;

/** The mechanical act of trying. A Move contains one IAttempt. */
public interface IAttempt
{
    void execute(Battle battle);
}
