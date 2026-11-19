package com.pokemon.conditions;

import com.pokemon.api.ICondition;
import com.pokemon.api.INumber;
import com.pokemon.battle.Battle;
import com.pokemon.numbers.Exactly;
import java.util.Objects;

/**
 * Condition that holds when {@code left >= right}.
 */
public class Geq implements ICondition<Battle>
{
    private final INumber left;
    private final INumber right;

    public Geq(INumber left, INumber right)
    {
        this.left = Objects.requireNonNull(left, "left must not be null");
        this.right = Objects.requireNonNull(right, "right must not be null");
    }

    public Geq(INumber number, double threshold)
    {
        this(number, new Exactly(threshold));
    }

    @Override
    public boolean check(Battle battle)
    {
        return left.evaluate(battle) >= right.evaluate(battle);
    }
}
