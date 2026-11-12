package com.pokemon.conditions;

import com.pokemon.api.ICondition;
import com.pokemon.battle.Battler;
import com.pokemon.battle.Element;

/** True if the Battler has the specified element type. */

public class HasElement implements ICondition<Battler>
{
    private final Element element;

    public HasElement(Element element)
    {
        this.element = element;
    }

    @Override
    public boolean check(Battler battler)
    {
        return battler.hasElement(element);
    }
}
