package com.pokemon.moves;

import com.pokemon.api.ICondition;
import com.pokemon.api.IMove;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;

/**
 * Decorator that checks a target-level condition BEFORE delegating.
 * Failure message: "It doesn't affect [name]!"
 *
 * Example: Thunder Wave — defender must not be Ground-type.
 */

public class WithApplicability implements IMove
{
    private final ICondition<Battler> condition;
    private final IMove               wrapped;

    public WithApplicability(ICondition<Battler> condition, IMove wrapped)
    {
        this.condition = condition;
        this.wrapped   = wrapped;
    }

    @Override
    public void execute(Battle battle)
    {
        Battler target = battle.getDefender();

        if (condition.check(target))
        {
            wrapped.execute(battle);
        }
        else
        {
            System.out.println("  It doesn't affect " + target.getName() + "!");
        }
    }
}
