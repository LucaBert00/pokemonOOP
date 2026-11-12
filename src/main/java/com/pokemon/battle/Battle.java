package com.pokemon.battle;

/**
 * Represents the state of a single turn.
 * Has an attacker and a defender. Flipping them advances the turn.
 *
 * Extended with helper fields that let combinators communicate:
 *   - lastAttemptHit  : used by Cascade to decide whether to continue
 *   - lastDamageDealt : used by Drain and LastDamageDealt INumber
 *   - currentMoveElement : set by Move so FormulaDamage can apply STAB/type chart
 */

public class Battle
{
    private Battler attacker;
    private Battler defender;

    // ── Communication slots (Cascade, Drain, type effectiveness) ─────────────
    private boolean lastAttemptHit;
    private double  lastDamageDealt;
    private Element currentMoveElement;

    // ── Constructor ───────────────────────────────────────────────────────────
    public Battle(Battler attacker, Battler defender)
    {
        this.attacker = attacker;
        this.defender = defender;
    }

    // ── Core access ───────────────────────────────────────────────────────────
    public Battler getAttacker() { return attacker; }
    public Battler getDefender() { return defender; }

    /** Swap sides to begin the next combatant's turn. */
    public void flipTurn()
    {
        Battler tmp = attacker;
        attacker    = defender;
        defender    = tmp;
    }

    // ── Communication getters/setters ─────────────────────────────────────────
    public boolean isLastAttemptHit()              { return lastAttemptHit; }
    public void    setLastAttemptHit(boolean v)    { lastAttemptHit = v; }

    public double  getLastDamageDealt()            { return lastDamageDealt; }
    public void    setLastDamageDealt(double v)    { lastDamageDealt = v; }

    public Element getCurrentMoveElement()         { return currentMoveElement; }
    public void    setCurrentMoveElement(Element e){ currentMoveElement = e; }
}
