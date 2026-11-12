package com.pokemon.battle;

public enum Element
{
    NORMAL, FIRE, WATER, ELECTRIC, GRASS, ICE, FIGHTING,
    POISON, GROUND, FLYING, PSYCHIC, BUG, ROCK, GHOST,
    DRAGON, DARK, STEEL, FAIRY;

    public String display()
    {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
