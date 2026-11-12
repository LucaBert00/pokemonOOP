package com.pokemon.conditions;

import com.pokemon.api.ICondition;
import com.pokemon.battle.Battler;

/** True if the Battler is currently under the badly poisoned (Toxic) status condition. */

public class IsBadlyPoisoned implements ICondition<Battler>
{
    @Override
    public boolean check(Battler battler)
    {
        return battler.isBadlyPoisoned();
    }
}