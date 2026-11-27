package com.pokemon.battle;

import java.util.Objects;
import java.util.Random;

/**
 * Represents the state of a single turn.
 * Has an attacker and a defender. Flipping them advances the turn.
 *
 * Extended with helper fields that let combinators communicate:
 *   - lastAttemptHit     : used by Cascade to decide whether to continue
 *   - lastDamageDealt    : used by Drain and LastDamageDealt INumber
 *   - currentMoveElement : set by Move so FormulaDamage can apply STAB/type chart
 *   - random             : single seedable RNG shared by all battle randomness
 */
public class Battle
{
    private Battler attacker;
    private Battler defender;
    private final Random random;

    // ── Communication slots (Cascade, Drain, type effectiveness) ──
    private boolean lastAttemptHit;
    private double  lastDamageDealt;
    private Element currentMoveElement;

    // ── Battle-wide weather ──
    private Weather weather = Weather.NONE;

    // ── Constructors ──
    public Battle(Battler attacker, Battler defender)
    {
        this(attacker, defender, new Random());
    }

    public Battle(Battler attacker, Battler defender, long seed)
    {
        this(attacker, defender, new Random(seed));
    }

    public Battle(Battler attacker, Battler defender, Random random)
    {
        this.attacker = attacker;
        this.defender = defender;
        this.random   = Objects.requireNonNull(random, "random must not be null");
    }

    // ── Core access ──
    public Battler getAttacker() { return attacker; }
    public Battler getDefender() { return defender; }

    /** Swap sides to begin the next combatant's turn. */
    public void flipTurn()
    {
        Battler tmp = attacker;
        attacker    = defender;
        defender    = tmp;
    }

    // ── Randomness helpers ──
    public Random  getRandom()          { return random; }
    public double  nextDouble()         { return random.nextDouble(); }
    public int     nextInt(int bound)   { return random.nextInt(bound); }
    public boolean nextBoolean()        { return random.nextBoolean(); }

    // ── Communication getters/setters ──
    public boolean isLastAttemptHit()              { return lastAttemptHit; }
    public void    setLastAttemptHit(boolean v)    { lastAttemptHit = v; }
    public double  getLastDamageDealt()            { return lastDamageDealt; }
    public void    setLastDamageDealt(double v)    { lastDamageDealt = v; }
    public Element getCurrentMoveElement()         { return currentMoveElement; }
    public void    setCurrentMoveElement(Element e){ currentMoveElement = e; }

    // ── Weather ──
    public Weather getWeather()
    {
        return weather;
    }

    public void setWeather(Weather weather)
    {
        this.weather = Objects.requireNonNull(weather, "weather must not be null");
    }
}