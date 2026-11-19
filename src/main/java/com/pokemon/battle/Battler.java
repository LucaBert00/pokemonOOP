package com.pokemon.battle;

import com.pokemon.api.IMove;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents a Pokémon in battle.
 * Holds stats, status conditions, and the set of moves it can use.
 */

public class Battler
{
    // ── Identity ──────────────────────────────────────────────────────────────
    private final String        name;
    private final List<Element> elements;

    // ── Stats ─────────────────────────────────────────────────────────────────
    private final int maxHp;
    private       int currentHp;
    private final int level;
    private       int attack;      // mutable — AttackStatChange modifies it
    private final int defense;
    private final int spAttack;
    private final int spDefense;
    private final int speed;

    // ── Status ────────────────────────────────────────────────────────────────
    private MajorStatus majorStatus = MajorStatus.NONE;
    private int sleepTurnsRemaining = 0;

    // ── Moves ─────────────────────────────────────────────────────────────────
    private final List<IMove> moves;

    // ── Constructor ───────────────────────────────────────────────────────────
    public Battler(String name, List<Element> elements, int maxHp, int level,
                   int attack, int defense, int spAttack, int spDefense,
                   int speed, List<IMove> moves)
    {
        this.name           = name;
        this.elements       = new ArrayList<>(elements);
        this.maxHp          = maxHp;
        this.currentHp      = maxHp;
        this.level          = level;
        this.attack         = attack;
        this.defense        = defense;
        this.spAttack       = spAttack;
        this.spDefense      = spDefense;
        this.speed          = speed;
        this.moves          = new ArrayList<>(moves);
    }

    // ── Combat helpers ────────────────────────────────────────────────────────
    public void takeDamage(int amount) {
        currentHp = Math.max(0, currentHp - amount);
    }

    public void restoreHp(int amount) {
        currentHp = Math.min(maxHp, currentHp + amount);
    }

    public boolean isFainted() {
        return currentHp <= 0;
    }

    public boolean hasElement(Element e) {
        return elements.contains(e);
    }

    /** Modify attack stat; clamps at 1. */
    public void modifyAttack(int delta) {
        attack = Math.max(1, attack + delta);
    }

    // ── Getters / setters ─────────────────────────────────────────────────────
    public String        getName()                      { return name; }
    public List<Element> getElements()                  { return elements; }
    public int           getMaxHp()                     { return maxHp; }
    public int           getCurrentHp()                 { return currentHp; }
    public int           getLevel()                     { return level; }
    public int           getAttack()                    { return attack; }
    public int           getDefense()                   { return defense; }
    public int           getSpAttack()                  { return spAttack; }
    public int           getSpDefense()                 { return spDefense; }
    public int           getSpeed()                     { return speed; }

    // ── Major status (mutually exclusive) ──
    public MajorStatus getMajorStatus()
    {
        return majorStatus;
    }

    public boolean hasMajorStatus()
    {
        return majorStatus.isActive();
    }

    /**
     * Applies {@code status} only if this battler has no active major status.
     * Refuses to overwrite an existing status (no-op, returns {@code false}).
     */
    public boolean applyMajorStatus(MajorStatus status)
    {
        Objects.requireNonNull(status, "status must not be null");
        if (status == MajorStatus.NONE || hasMajorStatus())
        {
            return false;
        }
        this.majorStatus = status;
        return true;
    }

    /** Clears any active major status (e.g. on wake or thaw) and resets sleep turns. */
    public void clearMajorStatus()
    {
        this.majorStatus = MajorStatus.NONE;
        this.sleepTurnsRemaining = 0;
    }

    public boolean isParalyzed()     { return majorStatus == MajorStatus.PARALYZED; }
    public boolean isBurned()        { return majorStatus == MajorStatus.BURNED; }
    public boolean isFrozen()        { return majorStatus == MajorStatus.FROZEN; }
    public boolean isPoisoned()      { return majorStatus == MajorStatus.POISONED; }
    public boolean isBadlyPoisoned() { return majorStatus == MajorStatus.BADLY_POISONED; }
    public boolean isAsleep()        { return majorStatus == MajorStatus.ASLEEP; }

    // ── Sleep duration tracking ──
    public int getSleepTurnsRemaining()
    {
        return sleepTurnsRemaining;
    }

    public void setSleepTurnsRemaining(int turns)
    {
        if (turns < 0)
        {
            throw new IllegalArgumentException("sleepTurnsRemaining must be >= 0, got " + turns);
        }
        this.sleepTurnsRemaining = turns;
    }

    public void decrementSleepTurns()
    {
        if (sleepTurnsRemaining > 0)
        {
            sleepTurnsRemaining--;
        }
    }

    public List<IMove>   getMoves()                     { return moves; }

    /** Visual HP bar for terminal display. */
    public String hpBar()
    {
        int total = 20;
        int filled = (int) Math.round((double) currentHp / maxHp * total);
        String bar = "█".repeat(filled) + "░".repeat(total - filled);

        double pct = (double) currentHp / maxHp * 100;

        String color = pct > 50 ? "" : pct > 20 ? "(LOW) " : "(CRIT) ";

        return String.format("[%s] %s%d/%d HP", bar, color, currentHp, maxHp);
    }

    @Override
    public String toString()
    {
        String typeStr = elements
            .stream()
            .map(Element::display)
            .reduce((a, b) -> a + "/" + b).orElse("???");

        return String.format("%s [%s] Lv.%d | %s", name, typeStr, level, hpBar());
    }
}
