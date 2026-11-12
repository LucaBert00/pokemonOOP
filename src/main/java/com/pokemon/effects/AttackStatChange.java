package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.INumber;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;

/** Changes the Attack stat of the target by the delta specified by INumber. */

public class AttackStatChange implements IEffect
{
    private final ITarget target;
    private final INumber delta;

    public AttackStatChange(ITarget target, INumber delta)
    {
        this.target = target;
        this.delta  = delta;
    }

    @Override
    public void apply(Battle battle)
    {
        Battler t   = target.resolve(battle);
        int     chg = (int) delta.evaluate(battle);
        t.modifyAttack(chg);

        if (chg > 0)
        {
            System.out.println("  " + t.getName() + "'s Attack rose!");
        }
        else
        {
            System.out.println("  " + t.getName() + "'s Attack fell!");
        }
    }
}
