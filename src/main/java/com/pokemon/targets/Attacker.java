package com.pokemon.targets;

import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;

/** Resolves to the currently attacking Battler. */

public class Attacker implements ITarget
{
    @Override
    public Battler resolve(Battle battle)
    {
        return battle.getAttacker();
    }
}
