package com.pokemon.conditions;

import com.pokemon.api.ICondition;
import com.pokemon.battle.Battler;

/** True if the Battler is currently under the paralysis status condition. */

public class IsParalyzed implements ICondition<Battler>
{
    @Override
    public boolean check(Battler battler)
    {
        return battler.isParalyzed();
    }
}
