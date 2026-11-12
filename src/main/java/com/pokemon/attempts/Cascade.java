package com.pokemon.attempts;

import com.pokemon.api.IAttempt;
import com.pokemon.battle.Battle;
import java.util.Arrays;
import java.util.List;

/**
 * Sequential chain of attempts. Stops immediately if any attempt misses.
 * Each attempt has its own accuracy roll, animation, and effect.
 * Does NOT print "Hit X times" — that's Combo's job.
 *
 * Example: Triple Kick — 10/20/30 damage, 90% each, stops on first miss.
 */

public class Cascade implements IAttempt
{
    private final List<IAttempt> attempts;

    public Cascade(IAttempt... attempts)
    {
        this.attempts = Arrays.asList(attempts);
    }

    @Override
    public void execute(Battle battle)
    {
        for (IAttempt attempt : attempts)
        {
            attempt.execute(battle);
            if (!battle.isLastAttemptHit())
            {
                return; // Chain broken — stop here
            }
        }
    }
}
