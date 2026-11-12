package com.pokemon.battle;

import com.pokemon.api.IMove;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private boolean paralyzed;
    private boolean burned;
    private boolean frozen;
    private boolean poisoned;
    private boolean badlyPoisoned;
    private boolean asleep;

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
        this.paralyzed      = false;
        this.burned         = false;
        this.frozen         = false;
        this.poisoned       = false;
        this.badlyPoisoned  = false;
        this.asleep         = false;
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
    public boolean       isParalyzed()                  { return paralyzed; }
    public void          setParalyzed(boolean v)        { paralyzed = v; }
    public boolean       isBurned()                     { return burned; }
    public void          setBurned(boolean v)           { burned = v; }
    public boolean       isFrozen()                     { return frozen; }
    public void          setFrozen(boolean v)           { frozen = v; }
    public boolean       isPoisoned()                   { return poisoned; }
    public void          setPoisoned(boolean v)         { poisoned = v; }
    public boolean       isBadlyPoisoned()              { return badlyPoisoned; }
    public void          setBadlyPoisoned(boolean v)    { badlyPoisoned = v; }
    public boolean       isAsleep()                     { return asleep; }
    public void          setAsleep(boolean v)           { asleep = v; }
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
