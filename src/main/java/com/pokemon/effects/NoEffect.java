package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.battle.Battle;

/**
 * Null Object Pattern — does nothing.
 * Used as the default for optional effect slots (onMiss, after) to
 * eliminate null checks throughout the system.
 */

public class NoEffect implements IEffect
{
    @Override
    public void apply(Battle battle)
    {
        /* intentionally empty */
    }
}
