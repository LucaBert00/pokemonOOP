# AGENTS.md

Repository rules for agents. The global `AGENTS.md` still applies (skills, smallest
complete change, real-user verification); this file adds the project specifics.

## Project

A Java 27 terminal Pokémon battle that implements the object-oriented DSL from Christopher
Okhravi's *Rebuilding Pokémon with Object Oriented Programming*. The companion video names
the design patterns the DSL accidentally uses. Read [CONTEXT.md](CONTEXT.md) before
changing the model, and the two transcript files when a behaviour needs its source.

## Workflow

- One issue per session: issue/ticket : session = 1:1. A session resolves exactly one
  GitHub issue, start to finish, and does not start another.

## Commands

```powershell
mvn -q compile                                            # fast check
mvn test                                                  # run JUnit 5 test suite
mvn -q clean package                                      # build the executable JAR
java -jar target/PokemonOOP-1.0-SNAPSHOT.jar              # play (Java 27+)
java -jar target/PokemonOOP-1.0-SNAPSHOT.jar --seed 42    # deterministic battle
```

If `java` on `PATH` is older than 27, use the JDK Maven used:
`& "$env:JAVA_HOME\bin\java" -jar target/PokemonOOP-1.0-SNAPSHOT.jar`.

There is no test suite and no test dependency. The battle itself is the integration test:
run it, pick a Pokémon, pick moves, watch the output.

## Non-negotiables

- **Java 27, zero runtime dependencies.** Production code (`src/main/java/`) and the packaged 
  JAR have zero external dependencies; `test`-scoped JUnit 5 (`org.junit.jupiter:junit-jupiter`) 
  in `pom.xml` is standard for automated verification.
- **The DSL is the design.** `com.pokemon.api` defines six interfaces; variation is a new
  implementation or a new composition, never a branch on a move name, type or string.
- **`Move` stays data.** It holds name, element and attempt, delegates, and prints its
  use line. Mechanics belong in `IAttempt` / `IEffect` / `ICondition` / `INumber`.
- **No null for absence.** Use `NoEffect` (or another null object) for empty slots.
- **Do not restructure working code** to make a small change fit. Add the smallest
  implementation that solves the case.

## Where changes go

| Change | Location |
|---|---|
| A new move | `data/MoveRegistry` static block |
| A new Pokémon | `src/main/resources/pokemon.csv` (14 columns, no header) |
| New damage/status/heal behaviour | `effects/`, implement `IEffect` |
| New predicate | `conditions/`, implement `ICondition` |
| New computed value | `numbers/`, implement `INumber` |
| New hit/miss pattern | `attempts/`, implement `IAttempt` |
| New check wrapping a move | `moves/`, implement `IMove` |
| Turn order, status gates, win condition | `BattleEngine` |
| Type effectiveness | `data/TypeChart` |

Before adding a class, check whether an existing composite already expresses the idea
(`Sequence`, `ConditionalEffect`, `And`, `Or`, `Not`, `Sum`, `Product`, `Cascade`,
`Combo`, `For`). Most "new mechanics" are compositions.

## Verification

1. `mvn test` and `mvn -q compile` must pass.
2. Build and run the JAR (optionally with `--seed <long>`). Drive a full turn: choose a Pokémon, 
   choose a move, read the output. For a decorated move, confirm the menu name and the failure message.
3. If a change touches damage, status or turn order, play until a faint.
4. Report anything you could not verify. Do not claim a behaviour works because the code
   looks right.

Issue #14 records the completed Fase 1 remediation behaviour, the observed terminal evidence, 
and the two approved proof-fixture substitutions: Machop uses `JumpKick` in place of `CrossChop`, 
and Gengar uses `ThunderWave` in place of `SwordsDance`. Issue #16 introduces `Battle`-owned 
seeded randomness and the `test`-scoped JUnit 5 harness.

This repository has no web UI, so Playwright is not part of normal verification. If it is
used (for example to research the videos), its output lives in `.playwright-mcp/`, which
is ignored and must never be committed or pushed.

## Documentation

- `TRANSCRIPT-01-*.md` and `TRANSCRIPT-02-*.md` are evidence. Do not rewrite the speaker's
  words; fix only encoding corruption. The Mermaid diagrams are reconstructions and are
  labelled as such.
- `CONTEXT.md` is the architecture source of truth. Update it when the type map, package
  layout, or known deviations change.
- The known deviations in `CONTEXT.md` are intentional and unfixed. Do not "clean them up"
  unless the task asks for it.

## Git

- One issue per session: resolve exactly one GitHub issue, then stop.
- When the issue's implementation and verification are complete, close the GitHub issue
  before finishing the session.
- Commit every repository change made for the issue and push it directly to `origin/main`.
- Keep `target/`, `out/`, IDE files and `.playwright-mcp/` out of version control.
