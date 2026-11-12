package com.pokemon.api;
import com.pokemon.battle.Battle;

/** Something that happens in battle — damage, healing, status, etc. */
public interface IEffect
{
    void apply(Battle battle);
}
