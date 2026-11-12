# Pokémon Battle Engine — an object-oriented DSL

A terminal Pokémon battle engine written in Java 11, where every piece of the game is an object that composes with the others. There are no `switch` statements on move types, no `if (move == "Tackle")`, and no special cases: a move is a tree of `IMove`, `IAttempt`, `IEffect`, `ICondition` and `INumber` objects.

The design follows Christopher Okhravi's video **[Rebuilding Pokémon with Object Oriented Programming](https://www.youtube.com/watch?v=CyRtTwKeulE)** and his book *The Object Oriented Way*. The architecture is his; the implementation, the move set, the roster and the battle loop are mine.

## Run it

Requires JDK 11 or newer and Maven.

```bash
mvn -DskipTests package
java -jar target/PokemonOOP-1.0-SNAPSHOT.jar
```

Pick a Pokémon from the roster, the CPU picks another one at random, and the battle runs in the terminal.

## The design

Everything the battle can do is expressed through six interfaces:

| Interface | Responsibility |
|---|---|
| `IMove` | a playable move |
| `IAttempt` | the mechanical act of trying: accuracy roll, `onHit`, `onMiss`, `after` |
| `IEffect` | something that happens in battle: damage, healing, status, stat change |
| `INumber` | a number evaluated lazily against the battle state at execution time |
| `ICondition<T>` | a predicate over any context (`Battle`, `Battler`, …) |
| `ITarget` | resolves to the attacker or the defender |

Behaviour is built by composition and decoration, not inheritance:

| Combinator | Meaning |
|---|---|
| `Sequence` | run every child, in order |
| `Cascade` | run children until one yields a result |
| `Combo` | run a child N times (multi-hit moves) |
| `And` / `Or` / `Not` | boolean composition of conditions |
| `For` | condition over every member of a collection |
| `Probability` | random condition with a given chance |
| `Weighted` | pick a value at random from weighted options |
| `WithPrecondition` | the move only executes if a precondition holds |
| `WithApplicability` | the move is only *offered* when applicable |

A move with three behaviours stacked on it, from `MoveRegistry`:

```java
registry.put("ThunderWave", new WithPrecondition(
    new Not<>(new For(DEF, new IsParalyzed())),          // target must not already be paralyzed
    new WithApplicability(
        new Not<>(new HasElement(Element.GROUND)),       // and must not be Ground-type
        new Move(
            "Thunder Wave", Element.ELECTRIC,
            new Attempt(
                "Thunder Wave",
                new Probability<>(0.90),                 // accuracy
                new Paralysis(DEF),                      // onHit
                new NoEffect(),                          // onMiss
                new NoEffect()                           // after
            )
        )
    )
));
```

No move subclasses. A new move is a new composition.

## What's in the box

- 16 Pokémon loaded from `src/main/resources/pokemon.csv`
- 27 moves built from the DSL: damage, status, stat changes, multi-hit, healing, OHKO
- A type chart with super-effective, not-very-effective and immune matchups
- STAB, accuracy rolls, paralysis, burn, poison, sleep, freeze
- A turn-based terminal battle loop

## Known limitations

- The move menu prints the decorator's class name (`WithPrecondition`) instead of the move name for decorated moves — a half-finished unwrap in `BattleEngine.moveName()`.
- No automated tests yet.
- Terminal output uses box-drawing characters; on a non-UTF-8 console they render as `?`.

## Credit

- Design: **Christopher Okhravi** — [*Rebuilding Pokémon with Object Oriented Programming*](https://www.youtube.com/watch?v=CyRtTwKeulE), and the book [*The Object Oriented Way*](https://theobjectorientedway.com).
- Pokémon is a trademark of Nintendo, Game Freak and Creatures Inc. This is a non-commercial learning project.
