package com.pokemon.conditions;

import com.pokemon.api.ICondition;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;

/**
 * The Lifting / Adapter combinator.
 * Promotes ICondition<Battler> → ICondition<Battle> by resolving the target first.
 *
 * Example: For(new Defender(), new IsParalyzed())
 *   → "Is the defender in this battle paralyzed?"
 *   without needing a dedicated DefenderIsParalyzed class.
 */

public class For implements ICondition<Battle>
{
    private final ITarget             target;
    private final ICondition<Battler> condition;

    public For(ITarget target, ICondition<Battler> condition)
    {
        this.target    = target;
        this.condition = condition;
    }

    @Override
    public boolean check(Battle battle)
    {
        Battler battler = target.resolve(battle);
        return condition.check(battler);
    }
}
