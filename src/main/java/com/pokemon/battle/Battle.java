package com.pokemon.battle;

import java.util.Objects;
import java.util.Random;

/**
 * Represents the state of a single turn.
 * Each side is a {@link Party}; the active member of each party is the
 * attacker or defender. Flipping the sides advances the turn.
 *
 * Extended with helper fields that let combinators communicate:
 *   - lastAttemptHit     : used by Cascade to decide whether to continue
 *   - lastDamageDealt    : used by Drain and LastDamageDealt INumber
 *   - currentMoveElement : set by Move so FormulaDamage can apply STAB/type chart
 *   - random             : single seedable RNG shared by all battle randomness
 */
public class Battle
{
    private Party attackerParty;
    private Party defenderParty;
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
        this(new Party(attacker), new Party(defender), new Random());
    }

    public Battle(Battler attacker, Battler defender, long seed)
    {
        this(new Party(attacker), new Party(defender), new Random(seed));
    }

    public Battle(Battler attacker, Battler defender, Random random)
    {
        this(new Party(attacker), new Party(defender), random);
    }

    public Battle(Party attackerParty, Party defenderParty)
    {
        this(attackerParty, defenderParty, new Random());
    }

    public Battle(Party attackerParty, Party defenderParty, long seed)
    {
        this(attackerParty, defenderParty, new Random(seed));
    }

    public Battle(Party attackerParty, Party defenderParty, Random random)
    {
        this.attackerParty = Objects.requireNonNull(attackerParty, "attackerParty must not be null");
        this.defenderParty = Objects.requireNonNull(defenderParty, "defenderParty must not be null");
        this.random        = Objects.requireNonNull(random, "random must not be null");
    }

    // ── Core access ──
    public Battler getAttacker()      { return attackerParty.getActive(); }
    public Battler getDefender()      { return defenderParty.getActive(); }
    public Party   getAttackerParty() { return attackerParty; }
    public Party   getDefenderParty() { return defenderParty; }

    /** Swap sides to begin the next combatant's turn. */
    public void flipTurn()
    {
        Party tmp    = attackerParty;
        attackerParty = defenderParty;
        defenderParty = tmp;
    }

    // ── Switching ──
    /** Sends out another member as the attacker, resolving before any move is executed. */
    public void switchAttacker(int index)
    {
        attackerParty.switchTo(index);
        resetVolatileState();
    }

    /** Sends out another member as the defender, resolving before any move is executed. */
    public void switchDefender(int index)
    {
        defenderParty.switchTo(index);
        resetVolatileState();
    }

    private void resetVolatileState()
    {
        lastAttemptHit     = false;
        lastDamageDealt    = 0.0;
        currentMoveElement = null;
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
