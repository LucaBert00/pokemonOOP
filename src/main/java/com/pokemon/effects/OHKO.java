package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;

/**
 * One-Hit KO. Distinct from Faint to allow ability interactions
 * and show a different message.
 */

public class OHKO implements IEffect
{
    private final ITarget target;

    public OHKO(ITarget target)
    {
        this.target = target;
    }

    @Override
    public void apply(Battle battle)
    {
        Battler t = target.resolve(battle);
        battle.setLastDamageDealt(t.getCurrentHp());
        t.takeDamage(t.getCurrentHp());
        System.out.println("  It's a One-Hit KO! " + t.getName() + " fainted!");
    }
}
