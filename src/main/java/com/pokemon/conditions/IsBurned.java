package com.pokemon.conditions;

import com.pokemon.api.ICondition;
import com.pokemon.battle.Battler;

/** True if the Battler is currently under the burn status condition. */

public class IsBurned implements ICondition<Battler>
{
    @Override
    public boolean check(Battler battler)
    {
        return battler.isBurned();
    }
}