package com.pokemon.data;

import com.pokemon.battle.Element;
import java.util.*;

/**
 * The Pokémon type effectiveness chart.
 * Returns a multiplier for (attacking type → defending type).
 * 0.0 = immune, 0.5 = not very effective, 1.0 = normal, 2.0 = super effective
 */

public class TypeChart
{
    // chart[attack][defense] = multiplier
    private static final Map<Element, Map<Element, Double>> chart = new EnumMap<>(Element.class);

    static
    {
        // Helper: set(attacker, multiplier, defender1, defender2, ...)
        // Defaults not listed are 1.0

        // NORMAL
        set(Element.NORMAL, 0.5, Element.ROCK, Element.STEEL);
        set(Element.NORMAL, 0.0, Element.GHOST);

        // FIRE
        set(Element.FIRE, 2.0, Element.GRASS, Element.ICE, Element.BUG, Element.STEEL);
        set(Element.FIRE, 0.5, Element.FIRE, Element.WATER, Element.ROCK, Element.DRAGON);

        // WATER
        set(Element.WATER, 2.0, Element.FIRE, Element.GROUND, Element.ROCK);
        set(Element.WATER, 0.5, Element.WATER, Element.GRASS, Element.DRAGON);

        // ELECTRIC
        set(Element.ELECTRIC, 2.0, Element.WATER, Element.FLYING);
        set(Element.ELECTRIC, 0.5, Element.ELECTRIC, Element.GRASS, Element.DRAGON);
        set(Element.ELECTRIC, 0.0, Element.GROUND);

        // GRASS
        set(Element.GRASS, 2.0, Element.WATER, Element.GROUND, Element.ROCK);
        set(Element.GRASS, 0.5, Element.FIRE, Element.GRASS, Element.POISON, Element.FLYING, Element.BUG, Element.DRAGON, Element.STEEL);

        // ICE
        set(Element.ICE, 2.0, Element.GRASS, Element.GROUND, Element.FLYING, Element.DRAGON);
        set(Element.ICE, 0.5, Element.WATER, Element.ICE);
        set(Element.ICE, 0.5, Element.FIRE, Element.STEEL);

        // FIGHTING
        set(Element.FIGHTING, 2.0, Element.NORMAL, Element.ICE, Element.ROCK, Element.DARK, Element.STEEL);
        set(Element.FIGHTING, 0.5, Element.POISON, Element.FLYING, Element.PSYCHIC, Element.BUG, Element.FAIRY);
        set(Element.FIGHTING, 0.0, Element.GHOST);

        // POISON
        set(Element.POISON, 2.0, Element.GRASS, Element.FAIRY);
        set(Element.POISON, 0.5, Element.POISON, Element.GROUND, Element.ROCK, Element.GHOST);
        set(Element.POISON, 0.0, Element.STEEL);

        // GROUND
        set(Element.GROUND, 2.0, Element.FIRE, Element.ELECTRIC, Element.POISON, Element.ROCK, Element.STEEL);
        set(Element.GROUND, 0.5, Element.GRASS, Element.BUG);
        set(Element.GROUND, 0.0, Element.FLYING);

        // FLYING
        set(Element.FLYING, 2.0, Element.GRASS, Element.FIGHTING, Element.BUG);
        set(Element.FLYING, 0.5, Element.ELECTRIC, Element.ROCK, Element.STEEL);

        // PSYCHIC
        set(Element.PSYCHIC, 2.0, Element.FIGHTING, Element.POISON);
        set(Element.PSYCHIC, 0.5, Element.PSYCHIC, Element.STEEL);
        set(Element.PSYCHIC, 0.0, Element.DARK);

        // BUG
        set(Element.BUG, 2.0, Element.GRASS, Element.PSYCHIC, Element.DARK);
        set(Element.BUG, 0.5, Element.FIRE, Element.FIGHTING, Element.FLYING, Element.GHOST, Element.STEEL, Element.FAIRY);

        // ROCK
        set(Element.ROCK, 2.0, Element.FIRE, Element.ICE, Element.FLYING, Element.BUG);
        set(Element.ROCK, 0.5, Element.FIGHTING, Element.GROUND, Element.STEEL);

        // GHOST
        set(Element.GHOST, 2.0, Element.PSYCHIC, Element.GHOST);
        set(Element.GHOST, 0.5, Element.DARK);
        set(Element.GHOST, 0.0, Element.NORMAL);

        // DRAGON
        set(Element.DRAGON, 2.0, Element.DRAGON);
        set(Element.DRAGON, 0.5, Element.STEEL);
        set(Element.DRAGON, 0.0, Element.FAIRY);

        // DARK
        set(Element.DARK, 2.0, Element.PSYCHIC, Element.GHOST);
        set(Element.DARK, 0.5, Element.FIGHTING, Element.DARK, Element.FAIRY);

        // STEEL
        set(Element.STEEL, 2.0, Element.ICE, Element.ROCK, Element.FAIRY);
        set(Element.STEEL, 0.5, Element.FIRE, Element.WATER, Element.ELECTRIC, Element.STEEL);

        // FAIRY
        set(Element.FAIRY, 2.0, Element.FIGHTING, Element.DRAGON, Element.DARK);
        set(Element.FAIRY, 0.5, Element.FIRE, Element.POISON, Element.STEEL);
    }

    private static void set(Element attacker, double multiplier, Element... defenders)
    {
        chart.computeIfAbsent(attacker, k -> new EnumMap<>(Element.class));

        for (Element d : defenders)
        {
            chart.get(attacker).put(d, multiplier);
        }
    }


    /**
     * Returns the combined effectiveness multiplier for one attack type
     * against a list of defense types (for dual-type defenders, types multiply).
     */

    public static double getEffectiveness(Element attackType, List<Element> defenseTypes)
    {
        if (attackType == null || defenseTypes == null || defenseTypes.isEmpty())
        {
            return 1.0;
        }

        double multiplier = 1.0;

        for (Element def : defenseTypes)
        {
            multiplier *= chart
                .getOrDefault(attackType, Collections.emptyMap())
                .getOrDefault(def, 1.0);
        }

        return multiplier;
    }
}
