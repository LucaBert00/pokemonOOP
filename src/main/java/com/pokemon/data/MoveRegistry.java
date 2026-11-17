package com.pokemon.data;

import com.pokemon.api.*;
import com.pokemon.attempts.*;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Element;
import com.pokemon.conditions.*;
import com.pokemon.effects.*;
import com.pokemon.moves.*;
import com.pokemon.numbers.*;
import com.pokemon.targets.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines all moves using the full DSL from the video.
 * Each move is constructed as a composition of IMove, IAttempt, IEffect,
 * ICondition, and INumber objects — no special cases, no switch statements.
 *
 * Shared ITarget singletons (attacker/defender) for readability.
 */

public class MoveRegistry
{
    private static final Attacker ATK = new Attacker();
    private static final Defender DEF = new Defender();

    private static final Map<String, IMove> registry = new HashMap<>();

    static
    {
        // ── From the video examples ───────────────────────────────────────────

        // 1. Tackle  (Chapter 27)
        registry.put("Tackle", new Move(
            "Tackle", Element.NORMAL,
            new Attempt(
                "Tackle",
                new Probability<>(0.95),
                new FormulaDamage(new Exactly(40), MoveCategory.PHYSICAL)
            )
        ));

        // 2. Body Slam  (Chapter 28) — damage + 30% paralysis on hit
        registry.put("BodySlam", new Move(
            "Body Slam", Element.NORMAL,
            new Attempt(
                "Body Slam",
                new Probability<>(1.0),
                new Sequence(
                    new FormulaDamage(new Exactly(85), MoveCategory.PHYSICAL),
                    new ConditionalEffect(
                        new Probability<>(0.30),
                        new Paralysis(DEF)
                    )
                )
            )
        ));

        // 3. Triple Kick  (Chapter 29) — Cascade of 3 attempts, 10/20/30 damage
        registry.put("TripleKick", new Move(
            "Triple Kick", Element.FIGHTING,
            new Cascade(
                new Attempt("Kick", new Probability<>(0.90),
                    new FormulaDamage(new Exactly(10), MoveCategory.PHYSICAL)),
                new Attempt("Kick", new Probability<>(0.90),
                    new FormulaDamage(new Exactly(20), MoveCategory.PHYSICAL)),
                new Attempt("Kick", new Probability<>(0.90),
                    new FormulaDamage(new Exactly(30), MoveCategory.PHYSICAL))
            )
        ));

        // 4. Jump Kick  (Chapter 30) — crash damage on miss (half attacker's max HP)
        registry.put("JumpKick", new Move(
            "Jump Kick", Element.FIGHTING,
            new Attempt(
                "Jump Kick",
                new Probability<>(0.95),
                new FormulaDamage(new Exactly(100), MoveCategory.PHYSICAL),
                new CrashDamage(ATK,
                    new Product(new MaxHP(ATK), new Exactly(0.5))),
                new NoEffect()
            )
        ));

        // 5. Fury Attack  (Chapter 31) — Combo: 85% accuracy, 2–5 weighted hits
        registry.put("FuryAttack", new Move(
            "Fury Attack", Element.NORMAL,
            new Combo(
                "Fury Attack",
                new Probability<>(0.85),
                new Weighted(
                    new double[]{2, 0.375},
                    new double[]{3, 0.375},
                    new double[]{4, 0.125},
                    new double[]{5, 0.125}
                ),
                new FormulaDamage(new Exactly(15), MoveCategory.PHYSICAL)
            )
        ));

        // 6. Thunder Wave  (Chapter 32) — full decorator chain
        //    Precondition: defender not already paralyzed
        //    Applicability: defender is not Ground-type
        registry.put("ThunderWave", new WithApplicability(
            new Not<>(new HasElement(Element.GROUND)),
            new WithPrecondition(
                new Not<>(new For(DEF, new IsParalyzed())),
                new Move(
                    "Thunder Wave", Element.ELECTRIC,
                    new Attempt(
                        "Thunder Wave",
                        new Probability<>(0.90),
                        new Paralysis(DEF),
                        new NoEffect(),
                        new NoEffect()
                    )
                )
            )
        ));

        // ── Additional moves ──────────────────────────────────────────────────

        registry.put("Ember", new Move(
            "Ember", Element.FIRE,
            new Attempt(
                "Ember",
                new Probability<>(1.0),
                new FormulaDamage(new Exactly(40), MoveCategory.SPECIAL)
            )
        ));

        registry.put("Flamethrower", new Move(
            "Flamethrower", Element.FIRE,
            new Attempt(
                "Flamethrower",
                new Probability<>(1.0),
                new FormulaDamage(new Exactly(90), MoveCategory.SPECIAL)
            )
        ));

        registry.put("WaterGun", new Move(
            "Water Gun", Element.WATER,
            new Attempt(
                "Water Gun",
                new Probability<>(1.0),
                new FormulaDamage(new Exactly(40), MoveCategory.SPECIAL)
            )
        ));

        registry.put("Surf", new Move(
            "Surf", Element.WATER,
            new Attempt(
                "Surf",
                new Probability<>(1.0),
                new FormulaDamage(new Exactly(90), MoveCategory.SPECIAL)
            )
        ));

        registry.put("Thunderbolt", new WithApplicability(
            new Not<>(new HasElement(Element.GROUND)),
            new Move(
                "Thunderbolt", Element.ELECTRIC,
                new Attempt(
                    "Thunderbolt",
                    new Probability<>(1.0),
                    new Sequence(
                        new FormulaDamage(new Exactly(90), MoveCategory.SPECIAL),
                        new ConditionalEffect(
                            new Probability<>(0.10),
                            new Paralysis(DEF)
                        )
                    )
                )
            )
        ));

        registry.put("Thunder", new WithApplicability(
            new Not<>(new HasElement(Element.GROUND)),
            new Move(
                "Thunder", Element.ELECTRIC,
                new Attempt(
                    "Thunder",
                    new Probability<>(0.70),
                    new Sequence(
                        new FormulaDamage(new Exactly(110), MoveCategory.SPECIAL),
                        new ConditionalEffect(
                            new Probability<>(0.30),
                            new Paralysis(DEF)
                        )
                    )
                )
            )
        ));

        registry.put("VineWhip", new Move(
            "Vine Whip", Element.GRASS,
            new Attempt(
                "Vine Whip",
                new Probability<>(1.0),
                new FormulaDamage(new Exactly(45), MoveCategory.PHYSICAL)
            )
        ));

        // Mega Drain — deals damage then drains 50%
        registry.put("MegaDrain", new Move(
            "Mega Drain", Element.GRASS,
            new Attempt(
                "Mega Drain",
                new Probability<>(1.0),
                new Sequence(
                    new FormulaDamage(new Exactly(40), MoveCategory.SPECIAL),
                    new Drain(ATK, new Exactly(0.5))
                )
            )
        ));

        registry.put("Scratch", new Move(
            "Scratch", Element.NORMAL,
            new Attempt(
                "Scratch",
                new Probability<>(1.0),
                new FormulaDamage(new Exactly(40), MoveCategory.PHYSICAL)
            )
        ));

        registry.put("QuickAttack", new Move(
            "Quick Attack", Element.NORMAL,
            new Attempt(
                "Quick Attack",
                new Probability<>(1.0),
                new FormulaDamage(new Exactly(40), MoveCategory.PHYSICAL)
            )
        ));

        registry.put("RockThrow", new Move(
            "Rock Throw", Element.ROCK,
            new Attempt(
                "Rock Throw",
                new Probability<>(0.90),
                new FormulaDamage(new Exactly(50), MoveCategory.PHYSICAL)
            )
        ));

        registry.put("KarateChop", new Move(
            "Karate Chop", Element.FIGHTING,
            new Attempt(
                "Karate Chop",
                new Probability<>(1.0),
                new FormulaDamage(new Exactly(50), MoveCategory.PHYSICAL)
            )
        ));

        registry.put("CrossChop", new Move(
            "Cross Chop", Element.FIGHTING,
            new Attempt(
                "Cross Chop",
                new Probability<>(0.80),
                new FormulaDamage(new Exactly(100), MoveCategory.PHYSICAL)
            )
        ));

        // Swords Dance — raises Attack by 20
        registry.put("SwordsDance", new Move(
            "Swords Dance", Element.NORMAL,
            new Attempt(
                "Swords Dance",
                new Probability<>(1.0),
                new AttackStatChange(ATK, new Exactly(20))
            )
        ));

        // Guillotine — OHKO with 30% accuracy
        registry.put("Guillotine", new Move(
            "Guillotine", Element.NORMAL,
            new Attempt(
                "Guillotine",
                new Probability<>(0.30),
                new OHKO(DEF)
            )
        ));

        // Recover — restore half max HP
        registry.put("Recover", new Move(
            "Recover", Element.NORMAL,
            new Attempt(
                "Recover",
                new Probability<>(1.0),
                new RestoreHP(ATK,
                    new Product(new MaxHP(ATK), new Exactly(0.5)))
            )
        ));

        // Will-O-Wisp — applies Burn; fails if defender is already burned or is Fire-type
        registry.put("WillOWisp", new WithApplicability(
                new Not<>(new HasElement(Element.FIRE)),
                new WithPrecondition(
                        new Not<>(new For(DEF, new IsBurned())),
                        new Move(
                                "Will-O-Wisp", Element.FIRE,
                                new Attempt(
                                        "Will-O-Wisp",
                                        new Probability<>(0.85),
                                        new Burn(DEF),
                                        new NoEffect(),
                                        new NoEffect()
                                )
                        )
                )
        ));

        // Toxic — badly poisons the defender; fails if already badly poisoned
        registry.put("Toxic", new WithPrecondition(
                new Not<>(new For(DEF, new IsBadlyPoisoned())),
                new Move(
                        "Toxic", Element.POISON,
                        new Attempt(
                                "Toxic",
                                new Probability<>(0.90),
                                new BadlyPoison(DEF),
                                new NoEffect(),
                                new NoEffect()
                        )
                )
        ));

        // Spore — puts the defender to sleep; fails if already asleep
        registry.put("Spore", new WithPrecondition(
                new Not<>(new For(DEF, new IsAsleep())),
                new Move(
                        "Spore", Element.GRASS,
                        new Attempt(
                                "Spore",
                                new Probability<>(1.0),
                                new Sleep(DEF),
                                new NoEffect(),
                                new NoEffect()
                        )
                )
        ));

        // Ice Beam — 90 damage + 10% chance to freeze
        registry.put("IceBeam", new Move(
                "Ice Beam", Element.ICE,
                new Attempt(
                        "Ice Beam",
                        new Probability<>(1.0),
                        new Sequence(
                                new FormulaDamage(new Exactly(90), MoveCategory.SPECIAL),
                                new ConditionalEffect(
                                        new Probability<>(0.10),
                                        new Freeze(DEF)
                                )
                        )
                )
        ));

        // Sludge Bomb — 90 damage + 30% chance to poison
        registry.put("SludgeBomb", new Move(
                "Sludge Bomb", Element.POISON,
                new Attempt(
                        "Sludge Bomb",
                        new Probability<>(1.0),
                        new Sequence(
                                new FormulaDamage(new Exactly(90), MoveCategory.SPECIAL),
                                new ConditionalEffect(
                                        new Probability<>(0.30),
                                        new Poison(DEF)
                                )
                        )
                )
        ));
    }

    /** Returns the IMove for the given key, or Tackle as fallback. */

    public static IMove get(String key)
    {
        return registry.getOrDefault(key, registry.get("Tackle"));
    }

}
