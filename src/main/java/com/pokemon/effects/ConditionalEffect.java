package com.pokemon.effects;

import com.pokemon.api.ICondition;
import com.pokemon.api.IEffect;
import com.pokemon.battle.Battle;

/**
 * If/then/else for effects. Checks a condition and branches.
 *
 * Example: Condition(Probability(0.3), Paralysis(Defender))
 *   → 30% chance to apply paralysis.
 */

public class ConditionalEffect implements IEffect
{
    private final ICondition<Battle> condition;
    private final IEffect            onPass;
    private final IEffect            onFail;

    public ConditionalEffect(ICondition<Battle> condition, IEffect onPass, IEffect onFail)
    {
        this.condition = condition;
        this.onPass    = onPass;
        this.onFail    = onFail;
    }

    /** Convenience: onFail defaults to NoEffect. */
    public ConditionalEffect(ICondition<Battle> condition, IEffect onPass)
    {
        this(condition, onPass, new NoEffect());
    }

    @Override
    public void apply(Battle battle)
    {
        if (condition.check(battle))
        {
            onPass.apply(battle);
        }
        else
        {
            onFail.apply(battle);
        }
    }
}
