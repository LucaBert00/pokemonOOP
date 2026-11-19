package com.pokemon.conditions;

import com.pokemon.api.ICondition;
import com.pokemon.battle.Battler;

/**
 * Evaluates to {@code true} when the target {@link Battler} currently has
 * any active {@link com.pokemon.battle.MajorStatus}.
 */
public class HasMajorStatus implements ICondition<Battler>
{
    @Override
    public boolean check(Battler battler)
    {
        return battler.hasMajorStatus();
    }
}
