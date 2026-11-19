package com.pokemon.effects;

import com.pokemon.api.IEffect;
import com.pokemon.api.INumber;
import com.pokemon.api.ITarget;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;
import java.util.List;
import java.util.Objects;

/**
 * Leaf effect that sets one or more targets' current HP to the floored
 * value of an {@link INumber} expression, clamped to {@code [0, maxHp]}.
 */
public class SetHP implements IEffect
{
    private final List<ITarget> targets;
    private final INumber targetHp;

    public SetHP(ITarget target, INumber targetHp)
    {
        this(targetHp, Objects.requireNonNull(target, "target must not be null"));
    }

    public SetHP(INumber targetHp, ITarget... targets)
    {
        this.targetHp = Objects.requireNonNull(targetHp, "targetHp must not be null");
        Objects.requireNonNull(targets, "targets must not be null");
        if (targets.length == 0)
        {
            throw new IllegalArgumentException("At least one target is required");
        }
        this.targets = List.of(targets);
    }

    @Override
    public void apply(Battle battle)
    {
        int raw = (int) Math.floor(targetHp.evaluate(battle));
        for (ITarget target : targets)
        {
            Battler battler = target.resolve(battle);
            int clamped = Math.max(0, Math.min(battler.getMaxHp(), raw));
            battler.setCurrentHp(clamped);
            System.out.printf("  %s's HP became %d!%n", battler.getName(), clamped);
        }
    }
}
