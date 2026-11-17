package com.pokemon.numbers;

import com.pokemon.api.INumber;
import com.pokemon.battle.Battle;
import java.util.ArrayList;
import java.util.List;

/**
 * Weighted random distribution. Used by Combo for hit-count rolls.
 *
 * Example for Fury Attack:
 *   new Weighted(new double[][]{{2, 0.375}, {3, 0.375}, {4, 0.125}, {5, 0.125}})
 */

public class Weighted implements INumber
{
    /** Each entry is {value, weight}. Weights need not sum to 1. */
    private final List<double[]> entries;

    public Weighted(double[]... entries)
    {
        this.entries = new ArrayList<>();

        for (double[] e : entries)
        {
            this.entries.add(e);
        }
    }

    @Override
    public double evaluate(Battle battle)
    {
        double total      = entries.stream().mapToDouble(e -> e[1]).sum();
        double r          = battle.nextDouble() * total;
        double cumulative = 0;
        for (double[] entry : entries)
        {
            cumulative += entry[1];
            if (r < cumulative)
            {
                return entry[0];
            }
        }
        return entries.get(entries.size() - 1)[0];
    }
}
