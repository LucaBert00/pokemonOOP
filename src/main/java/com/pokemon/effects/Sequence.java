package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.battle.Battle;
import java.util.Arrays;
import java.util.List;

/**
 * Applies ALL effects in order, unconditionally.
 * Unlike Cascade (attempts), a failing earlier effect does NOT stop the rest.
 *
 * Example: Body Slam onHit = Sequence(FormulaDamage(85), Condition(Prob(0.3), Paralysis))
 */

public class Sequence implements IEffect
{
    private final List<IEffect> effects;

    public Sequence(IEffect... effects)
    {
        this.effects = Arrays.asList(effects);
    }

    @Override
    public void apply(Battle battle)
    {
        for (IEffect effect : effects)
        {
            effect.apply(battle);
        }
    }
}
