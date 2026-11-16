package com.pokemon.attempts;

import com.pokemon.api.*;
import com.pokemon.battle.Battle;

/**
 * Multi-hit move with a single accuracy roll.
 * Hit count is determined lazily by an INumber (usually Weighted).
 * Applies the same effect every applied hit and reports the applied count.
 *
 * Example: Fury Attack — 85% accuracy, 2-5 hits, FormulaDamage(15) each.
 */

public class Combo implements IAttempt
{
    private final String             animationName;
    private final ICondition<Battle> accuracy;
    private final INumber            hits;
    private final IEffect            every;

    public Combo(String animationName, ICondition<Battle> accuracy,
                 INumber hits, IEffect every)
    {
        this.animationName = animationName;
        this.accuracy      = accuracy;
        this.hits          = hits;
        this.every         = every;
    }

    @Override
    public void execute(Battle battle)
    {
        boolean hit = accuracy.check(battle);
        battle.setLastAttemptHit(hit);

        if (hit)
        {
            int hitCount = (int) Math.max(1, hits.evaluate(battle));
            int appliedHits = 0;

            for (int i = 0; i < hitCount; i++)
            {
                if (battle.getDefender().isFainted())
                {
                    break;
                }

                System.out.println("  *" + animationName + "*");
                every.apply(battle);
                appliedHits++;
            }

            String times = appliedHits == 1 ? "time" : "times";
            System.out.println("  It hit " + appliedHits + " " + times + "!");
        }
        else
        {
            System.out.println("  The attack missed!");
        }
    }
}
