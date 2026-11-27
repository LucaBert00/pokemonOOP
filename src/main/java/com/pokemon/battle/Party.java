package com.pokemon.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One side's team: a single active {@link Battler} plus its bench.
 * The game uses parties of size 1 (1v1) or 2 (2v2).
 */
public final class Party
{
    private final List<Battler> members;
    private int activeIndex;

    public Party(Battler member)
    {
        this(List.of(Objects.requireNonNull(member, "member must not be null")));
    }

    public Party(List<Battler> members)
    {
        Objects.requireNonNull(members, "members must not be null");
        if (members.isEmpty())
        {
            throw new IllegalArgumentException("A party must contain at least one Pokémon");
        }
        for (Battler member : members)
        {
            Objects.requireNonNull(member, "party members must not be null");
        }
        this.members = new ArrayList<>(members);
    }

    public List<Battler> getMembers()   { return List.copyOf(members); }
    public Battler      getActive()     { return members.get(activeIndex); }
    public int          getActiveIndex(){ return activeIndex; }

    /** Every member except the active one, in party order. */
    public List<Battler> getBench()
    {
        List<Battler> bench = new ArrayList<>();
        for (int i = 0; i < members.size(); i++)
        {
            if (i != activeIndex)
            {
                bench.add(members.get(i));
            }
        }
        return bench;
    }

    /** Bench members that have not fainted, in party order. */
    public List<Battler> getHealthyBench()
    {
        List<Battler> healthy = new ArrayList<>();
        for (int i = 0; i < members.size(); i++)
        {
            if (i != activeIndex && !members.get(i).isFainted())
            {
                healthy.add(members.get(i));
            }
        }
        return healthy;
    }

    public boolean hasHealthyBench()
    {
        return !getHealthyBench().isEmpty();
    }

    /** True only when every party member has fainted. */
    public boolean isDefeated()
    {
        for (Battler member : members)
        {
            if (!member.isFainted())
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Makes the member at {@code index} active.
     *
     * @throws IllegalArgumentException if the index is invalid, already active,
     *                                  or points at a fainted member
     */
    public void switchTo(int index)
    {
        if (index < 0 || index >= members.size())
        {
            throw new IllegalArgumentException("Party index out of range: " + index);
        }
        if (index == activeIndex)
        {
            throw new IllegalArgumentException(members.get(index).getName() + " is already active");
        }
        if (members.get(index).isFainted())
        {
            throw new IllegalArgumentException(members.get(index).getName() + " has fainted");
        }
        activeIndex = index;
    }

    /**
     * Sends out the first healthy bench member after the active Pokémon fainted.
     *
     * @return the replacement, or {@code null} when no healthy bench member remains
     */
    public Battler replaceFaintedActive()
    {
        if (!getActive().isFainted())
        {
            return null;
        }
        for (int i = 0; i < members.size(); i++)
        {
            if (i != activeIndex && !members.get(i).isFainted())
            {
                activeIndex = i;
                return members.get(i);
            }
        }
        return null;
    }
}
