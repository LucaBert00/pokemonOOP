package com.pokemon.attempts;

import com.pokemon.api.*;
import com.pokemon.battle.Battle;
import com.pokemon.conditions.Probability;
import com.pokemon.effects.NoEffect;

/**
 * Leaf attempt. Has its own accuracy roll, animation, and three effect slots:
 *   onHit  — applied when accuracy check passes
 *   onMiss — applied when accuracy check fails   (defaults to NoEffect)
 *   after  — always applied                      (defaults to NoEffect)
 */

public class Attempt implements IAttempt
{
    private final String           animationName;
    private final ICondition<Battle> accuracy;
    private final IEffect          onHit;
    private final IEffect          onMiss;
    private final IEffect          after;

    /** Full constructor. */
    public Attempt(String animationName, ICondition<Battle> accuracy,
                   IEffect onHit, IEffect onMiss, IEffect after)
    {
        this.animationName = animationName;
        this.accuracy      = accuracy;
        this.onHit         = onHit;
        this.onMiss        = onMiss;
        this.after         = after;
    }

    /** Convenience constructor — onMiss and after default to NoEffect (Null Object Pattern). */
    public Attempt(String animationName, ICondition<Battle> accuracy, IEffect onHit)
    {
        this(animationName, accuracy, onHit, new NoEffect(), new NoEffect());
    }

    /** Convenience constructor — accuracy defaults to 1.0. */
    public Attempt(String animationName, IEffect onHit)
    {
        this(animationName, new Probability<>(1.0), onHit, new NoEffect(), new NoEffect());
    }

    /** Full constructor with default accuracy — onMiss and after are explicit. */
    public Attempt(String animationName, IEffect onHit, IEffect onMiss, IEffect after)
    {
        this(animationName, new Probability<>(1.0), onHit, onMiss, after);
    }

    @Override
    public void execute(Battle battle)
    {
        System.out.println("  *" + animationName + "*");

        if (accuracy.check(battle))
        {
            battle.setLastAttemptHit(true);
            onHit.apply(battle);
        }
        else
        {
            battle.setLastAttemptHit(false);
            System.out.println("  The attack missed!");
            onMiss.apply(battle);
        }
        after.apply(battle);
    }
}
