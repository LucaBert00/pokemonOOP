package com.pokemon.numbers;

import com.pokemon.api.INumber;
import com.pokemon.battle.Battle;
import java.util.Objects;

/**
 * Composite {@link INumber} that lazily evaluates two child expressions
 * and returns the greater value.
 */
public class Max implements INumber
{
    private final INumber left;
    private final INumber right;

    public Max(INumber left, INumber right)
    {
        this.left = Objects.requireNonNull(left, "left must not be null");
        this.right = Objects.requireNonNull(right, "right must not be null");
    }

    @Override
    public double evaluate(Battle battle)
    {
        return Math.max(left.evaluate(battle), right.evaluate(battle));
    }
}
