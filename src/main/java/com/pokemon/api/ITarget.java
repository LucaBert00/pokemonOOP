package com.pokemon.api;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;

/** Resolves one Battler from a Battle (attacker or defender side). */
public interface ITarget
{
    Battler resolve(Battle battle);
}
