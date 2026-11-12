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

    public Move(String name, Element element, IAttempt attempt)
    {
        this.name    = name;
        this.element = element;
        this.attempt = attempt;
    }

    @Override
    public void execute(Battle battle)
    {
        System.out.println(battle.getAttacker().getName() + " used " + name + "!");
        battle.setCurrentMoveElement(element);
        attempt.execute(battle);
    }

    public String  getName()    { return name; }
    public Element getElement() { return element; }
}
