package com.pokemon.numbers;

import com.pokemon.api.ICondition;
import com.pokemon.api.INumber;
import com.pokemon.battle.Battle;
import java.util.Objects;

/**
 * Composite {@link INumber} that evaluates {@code whenTrue} when
 * {@code condition} holds on the {@link Battle}, and {@code whenFalse} otherwise.
 */
public class ConditionalNumber implements INumber
{
    private final ICondition<Battle> condition;
    private final INumber whenTrue;
    private final INumber whenFalse;

    public ConditionalNumber(ICondition<Battle> condition, INumber whenTrue, INumber whenFalse)
    {
        this.condition = Objects.requireNonNull(condition, "condition must not be null");
        this.whenTrue = Objects.requireNonNull(whenTrue, "whenTrue must not be null");
        this.whenFalse = Objects.requireNonNull(whenFalse, "whenFalse must not be null");
    }

    @Override
    public double evaluate(Battle battle)
    {
        return condition.check(battle)
            ? whenTrue.evaluate(battle)
            : whenFalse.evaluate(battle);
    }
}
