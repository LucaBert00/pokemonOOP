package com.pokemon.conditions;

import com.pokemon.api.ICondition;
import com.pokemon.battle.Battler;

/** True if the Battler is currently under the sleep status condition. */

public class IsAsleep implements ICondition<Battler>
{
    @Override
    public boolean check(Battler battler)
    {
        return battler.isAsleep();
    }
}