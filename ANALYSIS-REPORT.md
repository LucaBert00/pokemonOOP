# Analysis Report — PokemonOOP

> Snapshot of the project as of **2026-11-12**, commit `4447ffa` (`build: target JDK 27 and add fidelity audit`), branch `main`.
> This report complements `FINDINGS.md` (fidelity audit) and does **not** replace `CONTEXT.md`, which stays the architecture source of truth.

## 1. Scope and method

- Cloned the repo and read `AGENTS.md`, `CONTEXT.md`, `FINDINGS.md`, `pom.xml` and the engine, effect, registry and data code.
- Compiled all 62 source files with `javac --release 21` (JDK 21 was the newest available; **Maven and JDK 27 were not**).
- Ran one scripted battle (Pikachu vs a random CPU opponent) to check the menu, a move and the win path.
- Cross-checked the claims in `FINDINGS.md` against the code with `grep` and by reading the relevant classes.

**Not covered:** all 27 moves, all type combinations, a build with Maven on JDK 27, a full battle played to a faint on both sides.

## 2. Project snapshot

| | |
|---|---|
| What it is | Terminal Pokémon battle implementing the OOP DSL from Christopher Okhravi's *Rebuilding Pokémon with Object Oriented Programming* and its follow-up on design patterns |
| Language / build | Java 27 target, Maven, executable JAR, **no dependencies** |
| Size | 62 classes, about 2,600 lines (`MoveRegistry` 397, `BattleEngine` 262, `TypeChart` 137, `Battler` 135) |
| Data | `pokemon.csv` with 16 Pokémon, `MoveRegistry` with 27 moves |
| History | 4 commits |
| Tests | none (the battle itself is the integration test) |

## 3. Current structure

### Six interfaces in `com.pokemon.api`

| Interface | Method | Role |
|---|---|---|
| `IMove` | `execute(Battle)` | A complete move |
| `IAttempt` | `execute(Battle)` | Hit/miss machinery, leaf or chain |
| `IEffect` | `apply(Battle)` | A state change: damage, status, heal |
| `ICondition<T>` | `check(T)` | A predicate |
| `INumber` | `evaluate(Battle)` | A computed `double` |
| `ITarget` | `resolve(Battle)` | Picks attacker or defender |

### Packages

| Package | Contents |
|---|---|
| `moves` | `Move` plus the decorators `WithPrecondition` and `WithApplicability` |
| `attempts` | `Attempt`, `Cascade`, `Combo` |
| `effects` | damage, status, heal, drain, OHKO, `Sequence`, `ConditionalEffect`, `NoEffect` |
| `conditions` | `And`, `Or`, `Not`, `Probability`, `HasElement`, status checks, `For` |
| `numbers` | `Exactly`, `Sum`, `Product`, `Quotient`, `Weighted`, `MaxHP`, `CurrentHP`, `Level`, ... |
| `targets` | `Attacker`, `Defender` |
| `battle` | `Battle` (turn state), `Battler`, `Element` |
| `data` | `MoveRegistry`, `PokemonLoader`, `TypeChart` |
| root | `Main`, `BattleEngine` (turn loop, status gates, UI) |

### How a move is built

```text
Tackle      Move("Tackle", NORMAL) -> Attempt(95%, FormulaDamage(Exactly(40)))
Fury Attack Move -> Combo(accuracy, Weighted(hit count), FormulaDamage)
Body Slam   Move -> Attempt -> Sequence(damage, ConditionalEffect(30%, Paralysis))
Thunder Wave  WithPrecondition(WithApplicability(Move)) -- decorators around a Move
```

There is no `switch` on move names anywhere; variation is always a new object or a new composition.

### Design patterns present

Strategy, Composite, Decorator, Command and Null Object are all present at the type level, as `CONTEXT.md` claims.

## 4. Implementation status

### Works

- Roster loading, player selection, random CPU opponent, speed-based turn order.
- Damage with STAB and type chart, faint, win/lose/draw.
- Sleep, freeze and paralysis checks before acting; burn, poison and badly-poison damage after acting.
- Decorated moves, status moves, heal, drain, multi-hit (`Cascade`, `Combo`), OHKO, stat change.
- The project compiles cleanly (checked on JDK 21 with `--release 21`, so nothing in the code needs a newer Java).

### Not implemented

PP, switching, items, abilities, weather, move priority, critical hits, status duration, status type immunities, saves, tests, web UI, PokéAPI, database. Turn order is computed once from Speed at the start of the battle, so `Quick Attack` has no priority. The CPU picks moves at random.

## 5. Findings

### 5.1 Confirmed from `FINDINGS.md`

| # | Finding | Evidence | Status |
|---|---|---|---|
| 1 | Move menu shows `WithPrecondition` / `WithApplicability` instead of the move name | Reproduced: Pikachu menu printed `WithPrecondition`, `WithApplicability`, `WithApplicability`, `Quick Attack`. `BattleEngine.moveName` has an empty `WithPrecondition` branch | Open |
| 2 | Sleep never ends | `setAsleep` appears only in `Sleep.java` (sets `true`); the only thaw path in the engine is for freeze | Open |
| 3 | Residual damage is applied per half-turn, and skipped when the turn is skipped | `BattleEngine.executeTurn` returns early before the burn/poison block | Open |
| 4 | Only one status is processed even if several are set | `if / else if` chain for burn, poison, badly poison; `statusTag` shows the first match | Open |
| 5 | Badly poison does not scale | Engine uses `maxHp / 16` every turn | Open |
| 6 | `spAttack` / `spDefense` are loaded but never used | No reads outside `Battler` and `PokemonLoader` | Open |
| 7 | Randomness is not owned by the battle | `Math.random()` is called in **6** places: `FormulaDamage`, `Probability`, `Weighted`, `Between`, `BattleEngine`, `Main` (the audit lists five) | Open |
| 8 | Decorator order inverted in Thunder Wave and Will-O-Wisp | As documented in `CONTEXT.md` | Open |

The remaining items in `FINDINGS.md` (Combo animation and count, Jump Kick crash message, Body Slam accuracy, `Drain` and `RestoreHP` edge cases, silent Tackle fallback, dead code) were **not re-verified** here.

### 5.2 New: stat scale is wrong, so battles end in one or two hits

`pokemon.csv` stores values that match the species' **base stats** (for example Pikachu: 35 HP, 55 Atk, 40 Def, 90 Speed), but every Pokémon is set to **level 50** and `FormulaDamage` uses the mainline-style formula that expects **calculated** stats at that level.

Observed in a scripted run: Pikachu's Thunderbolt dealt **97 damage** to a Squirtle with **44 HP**, ending the battle on turn 1.

Consequences:

- Most matchups finish in one or two turns.
- Status effects, Toxic, sleep, healing and multi-turn strategy barely matter, and several of the bugs above are hard to even notice.

Suggested fix (standard formulas with IV = 0 and EV = 0):

```text
HP    = 2 * base * level / 100 + level + 10     e.g. Pikachu 35 -> 95 HP at level 50
other = 2 * base * level / 100 + 5              e.g. Pikachu Atk 55 -> 60
```

Apply it in `PokemonLoader` (or a small `StatCalculator`), or change the CSV to hold final stats. Either way, decide which one the CSV means and document it in `CONTEXT.md`.

## 6. Framework and tooling assessment

| Topic | Assessment |
|---|---|
| Framework | **Keep plain Java + Maven.** Spring, Quarkus and similar would add annotations and dependency injection exactly where the object composition is meant to be visible. If a web UI or REST API is ever added, put a thin adapter (`com.pokemon.web`) on top of the domain, not inside `IMove` / `IEffect` |
| Java version | Compiles at release 21, so the 27 target is not required by the code. Lowering to 21 or 25 (LTS) would let more machines run the JAR. Optional |
| Data | CSV is enough for 16 Pokémon and 27 moves. Consider SQLite only if PokéAPI caching, saves or accounts arrive; no JPA/Hibernate |
| Tests | None today. `AGENTS.md` forbids new dependencies, which blocks JUnit. See the decision list below |
| Engine size | `BattleEngine` mixes UI, turns, status and move choice. Fine at 262 lines; split into `BattleUI` and `BattleRules` before adding switching, items or abilities |
| `Battle` as a state bus | `lastAttemptHit`, `lastDamageDealt` and `currentMoveElement` are hidden couplings. Acceptable now, a risk if the DSL grows |

## 7. Recommended roadmap

Order is by value for effort. Paths are indicative.

### Priority 1 — Make battles playable as intended

- [ ] **Fix stat scale** (section 5.2). `PokemonLoader` / `Battler` / CSV semantics; update `CONTEXT.md`.
- [ ] **Show real move names in the menu.** Add `String getName()` to `IMove`, delegate it in `WithPrecondition` / `WithApplicability`, use it in `BattleEngine`. Remove the dead `MoveRegistry.displayName`.
- [ ] **Swap decorator order** in Thunder Wave and Will-O-Wisp (applicability outside, precondition inside).
- [ ] **Print "X used Y!"** also when a precondition or applicability check fails.

### Priority 2 — Game rules

- [ ] Sleep with a duration of 1–3 turns and a wake-up message.
- [ ] One major status per Pokémon.
- [ ] Residual damage once per round, after both actions, even when the turn was skipped.
- [ ] Toxic counter (1/16, 2/16, 3/16, ...).
- [ ] Use `spAttack` / `spDefense` for special moves, or drop the columns.
- [ ] `MoveRegistry.get` fails loudly on an unknown key instead of falling back to Tackle.

### Priority 3 — Structure and quality

- [ ] One seeded `Random` owned by `Battle`; remove the six `Math.random()` calls. This also makes runs reproducible.
- [ ] Remove or connect dead code: `Faint`, `Between`, `Leq`, `Geq`, `LastDamageDealt`, `Battle.flipTurn`.
- [ ] Combo: play the animation per hit and report the number of hits actually applied.
- [ ] Split `BattleEngine` into `BattleUI` and `BattleRules` if switching, items or abilities are added.
- [ ] Add a manual battle checklist (decorated menu, failed move, multi-hit, each status, OHKO, win/loss).

### Priority 4 — Extensions

- [ ] PokéAPI behind `PokemonSource` (`CsvPokemonSource`, `PokeApiPokemonSource`) and `MoveCatalog` (`RegistryMoveCatalog`, `PokeApiMoveCatalog`), with an on-disk cache and the CSV as offline fallback. Do not generate a class per API move.
- [ ] Move priority, critical hits, PP, switching.
- [ ] Smarter CPU than random.

## 8. Decisions to take later

- [ ] **Allow JUnit?** Suggested wording for `AGENTS.md`: "no new *runtime* dependencies; test-scope dependencies are allowed". Otherwise keep manual verification only.
- [ ] **Fix the documented deviations?** `AGENTS.md` and `CONTEXT.md` say they are intentional and unfixed. Priority 1 items above would contradict that rule, so update it explicitly.
- [ ] **What does the CSV mean:** base stats or final stats?
- [ ] **Java target:** keep 27 or lower to 21 / 25.
- [ ] **Where to keep this report:** merge the useful parts into `FINDINGS.md` / `CONTEXT.md`, or keep it as a dated snapshot.

## 9. Caveats

- This report was produced without Maven and without JDK 27; the build was checked with `javac --release 21`.
- Findings marked "not re-verified" come from `FINDINGS.md` and were not independently checked.
- No source file in the repository was modified to produce this report.
