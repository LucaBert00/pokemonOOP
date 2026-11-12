package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;

/** Causes the target to faint immediately (sets HP to 0). */

public class Faint implements IEffect
{
    private final ITarget target;

    public Faint(ITarget target)
    {
        this.target = target;
    }

    @Override
    public void apply(Battle battle)
    {
        Battler t = target.resolve(battle);
        t.takeDamage(t.getCurrentHp()); // drain all remaining HP
        System.out.println("  " + t.getName() + " fainted!");
    }
}
