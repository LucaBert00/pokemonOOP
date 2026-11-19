package com.pokemon.moves;

import com.pokemon.api.ICondition;
import com.pokemon.api.IMove;
import com.pokemon.battle.Battle;

/**
 * Decorator that checks a battle-level condition BEFORE delegating.
 * Failure message defaults to "But it failed!".
 *
 * Example: Thunder Wave — defender must not already be paralyzed.
 */

public class WithPrecondition implements IMove
{
    private final ICondition<Battle> condition;
    private final IMove              wrapped;
    private final String             failureMessage;

    public WithPrecondition(ICondition<Battle> condition, IMove wrapped)
    {
        this(condition, wrapped, "  But it failed!");
    }

    public WithPrecondition(ICondition<Battle> condition, IMove wrapped, String failureMessage)
    {
        this.condition      = condition;
        this.wrapped        = wrapped;
        this.failureMessage = failureMessage;
    }

    public IMove getWrapped() { return wrapped; }

    @Override
    public void execute(Battle battle)
    {
        if (condition.check(battle))
        {
            wrapped.execute(battle);
        }
        else
        {
            System.out.println(failureMessage);
        }
    }
}
