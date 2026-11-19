package com.pokemon.moves;

import com.pokemon.api.IAttempt;
import com.pokemon.api.IMove;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Element;

/**
 * A named, typed move that delegates its mechanics to an IAttempt.
 * Sets battle.currentMoveElement so FormulaDamage can apply STAB + type chart.
 */

public class Move implements IMove
{
    private final String   name;
    private final Element  element;
    private final IAttempt attempt;
    private final int      priority;

    public Move(String name, Element element, IAttempt attempt)
    {
        this(name, element, attempt, 0);
    }

    public Move(String name, Element element, IAttempt attempt, int priority)
    {
        this.name     = name;
        this.element  = element;
        this.attempt  = attempt;
        this.priority = priority;
    }

    @Override
    public int priority()
    {
        return priority;
    }

    @Override
    public void execute(Battle battle)
    {
        battle.setCurrentMoveElement(element);
        attempt.execute(battle);
    }

    public String  getName()    { return name; }
    public Element getElement() { return element; }
}
