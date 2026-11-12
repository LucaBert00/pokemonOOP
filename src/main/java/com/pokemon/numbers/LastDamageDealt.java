package com.pokemon.numbers;

import com.pokemon.api.INumber;
import com.pokemon.battle.Battle;

/**
 * Returns the last damage dealt in this battle (stored on Battle).
 * Useful for: Counter, Destiny Bond, Bide, etc.
 */

public class LastDamageDealt implements INumber
{
    @Override public double evaluate(Battle battle)
    {
        return battle.getLastDamageDealt();
    }
}
