package com.pokemon.battle;

/**
 * Mutually exclusive major status conditions a {@link Battler} can hold.
 * {@link #NONE} represents the absence of any major status (never {@code null}).
 */
public enum MajorStatus
{
    NONE,
    PARALYZED,
    BURNED,
    FROZEN,
    POISONED,
    BADLY_POISONED,
    ASLEEP;

    public boolean isActive()
    {
        return this != NONE;
    }
}
