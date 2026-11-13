# Fase 1 DSL remediation specification

This document is the implementation-ready specification for the Fase 1 fidelity and
output corrections listed in [`FINDINGS.md`](../FINDINGS.md). It records the decisions
closed in GitHub issues [#2](https://github.com/LucaBert00/pokemonOOP/issues/2),
[#4](https://github.com/LucaBert00/pokemonOOP/issues/4),
[#5](https://github.com/LucaBert00/pokemonOOP/issues/5),
[#6](https://github.com/LucaBert00/pokemonOOP/issues/6), and
[#7](https://github.com/LucaBert00/pokemonOOP/issues/7). The transcripts are the
source of truth for claims about the video and the DSL:
[`TRANSCRIPT-01-rebuilding-pokemon-with-oop.md`](../TRANSCRIPT-01-rebuilding-pokemon-with-oop.md)
and
[`TRANSCRIPT-02-accidentally-used-design-patterns.md`](../TRANSCRIPT-02-accidentally-used-design-patterns.md).

This is a specification, not an implementation. It contains no Java code and does not
change `CONTEXT.md`.

## Scope and non-goals

### In scope

Fase 1 items 1–7 only:

1. Applicability must be the outer decorator and precondition the inner decorator for
   Thunder Wave and Will-O-Wisp.
2. Decorated moves must retain their real identity in the menu.
3. A decorated move must print its used line before a precondition or applicability
   failure.
4. Jump Kick must use a distinct crash-damage effect and its crash output.
5. Body Slam must use 100% accuracy.
6. Combo must animate and apply its effect once per applied hit and report applied hits.
7. Leq and Geq remain in their current battle-level shape for this phase.

### Out of scope

- Fase 1 item 8: moving randomness into `Battle` or adding seeded randomness.
- All Fase 2 work: status duration and exclusivity, residual-damage timing, Toxic
  scaling, special-stat damage, Drain/RestoreHP edge cases, unknown move-key failure,
  and unrelated dead-code cleanup.
- All Fase 3 work as separate implementation: the later `CONTEXT.md` edit and the
  manual battle checklist are specified here but are not performed by this issue.
- Any Java implementation, refactoring beyond the named changes, dependency addition,
  JUnit, web UI, PokéAPI, database, switching, items, abilities, weather, PP, priority,
  critical hits, or a redesign of the DSL.

The repository constraints remain binding: Java 27, no new dependencies, composition
instead of branches on move names/types/strings, `Move` remains data, and absence is
represented by `NoEffect` rather than `null`.

## Closed decision 1: move identity, output, and decorator order

Decision: [issue #2 — Define the move identity and output contract](https://github.com/LucaBert00/pokemonOOP/issues/2).

### Settled contract

The video gives `IMove` only `void execute(Battle battle)`. Keep that interface
signature unchanged. `Move` remains the named data object and keeps `getName()`.
`WithPrecondition` and `WithApplicability` expose `IMove getWrapped()` so
`BattleEngine` can recursively unwrap those two known decorators until it reaches the
inner `Move` and read its `getName()`. Use direct type checks and recursion; do not use
reflection. Delete the dead `MoveRegistry.displayName` helper.

The used line belongs to `BattleEngine`, immediately before `move.execute(battle)`:

```text
<attacker name> used <move name>!
```

It has no leading spaces. `Move.execute` must no longer print this line; it only sets
the current move element and delegates to its `IAttempt`.

Applicability is the outer wrapper and precondition is the inner wrapper for both
Thunder Wave and Will-O-Wisp:

```text
WithApplicability(..., WithPrecondition(..., Move(...)))
```

The outer applicability check therefore wins when both checks fail. A paralyzed
Ground-type defender hit by Thunder Wave prints the applicability failure, not the
precondition failure.

Exact failure strings, including their two-space indentation, are:

```text
  It doesn't affect <defender name>!
  But it failed!
```

The exact output order for a decorated failure is:

1. `<attacker name> used <move name>!`
2. `  It doesn't affect <defender name>!`, when applicability fails; otherwise
3. `  But it failed!`, when the precondition fails.

The menu names must be the inner `Move` names, including `Thunder Wave`, `Thunderbolt`,
`Thunder`, and `Will-O-Wisp`, never `WithPrecondition` or `WithApplicability`.

### Later implementation surface

- `src/main/java/com/pokemon/api/IMove.java`: unchanged `execute` contract.
- `src/main/java/com/pokemon/moves/Move.java`: retain `getName()`; remove used-line
  printing from `execute`.
- `src/main/java/com/pokemon/moves/WithPrecondition.java`: add the direct wrapper
  accessor; retain the exact failure string.
- `src/main/java/com/pokemon/moves/WithApplicability.java`: add the direct wrapper
  accessor; retain the exact failure string.
- `src/main/java/com/pokemon/BattleEngine.java`: print the used line and recursively
  resolve the display name.
- `src/main/java/com/pokemon/data/MoveRegistry.java`: reverse the two wrapper chains
  and delete `displayName`.

### Observable proof

The player menu shows the real names. A Thunder Wave attempt against a Ground-type
prints the used line first and then `It doesn't affect <name>!`. Against a defender that
is both Ground-type and paralyzed, the applicability message still wins. Against a
non-Ground defender that is already paralyzed, the used line is followed by
`But it failed!`. A successful decorated move prints the used line once and then its
ordinary attempt output.

## Closed decision 2: Body Slam accuracy

Decision: [issue #4 — Choose Body Slam accuracy](https://github.com/LucaBert00/pokemonOOP/issues/4),
based on the evidence gathered in [issue #3](https://github.com/LucaBert00/pokemonOOP/issues/3).

### Settled contract

Body Slam uses a single 100% accuracy probability. The later registry composition must
therefore use `Probability(1.0)` for its `Attempt` accuracy. The rationale is canonical
Pokémon move data: Body Slam has 100 accuracy and 85 power in Generations I–IX. The
transcript states the 85 power and the 30% paralysis chance, but does not state an
accuracy; the existing 85% accuracy was invented and is not retained. The transcript's
later “80%” paralysis wording is a slip; the settled paralysis chance remains 30%.

The move remains composition-first:

```text
Move("Body Slam", NORMAL,
  Attempt("Body Slam", Probability(1.0),
    Sequence(
      FormulaDamage(Exactly(85)),
      ConditionalEffect(Probability(0.30), Paralysis(DEF)))))
```

The code block is a shape description, not implementation code to copy into this
documentation-only issue.

### Later implementation surface

- `src/main/java/com/pokemon/data/MoveRegistry.java`: change only Body Slam's accuracy
  value from 0.85 to 1.0; preserve its damage, sequence, and 30% paralysis composition.
- No `CONTEXT.md` fidelity note is required for this decision.

### Observable proof

Body Slam never reaches its miss branch because its accuracy is 100%. On a successful
use, the normal attempt animation is followed by formula damage of exactly 85 power,
and the conditional paralysis effect remains a separate 30% check.

## Closed decision 3: Combo semantics and output

Decision: [issue #5 — Define Combo's per-hit output and applied-hit count](https://github.com/LucaBert00/pokemonOOP/issues/5).

### Settled contract

`Combo` performs one accuracy check. When it succeeds, it resolves the lazy `INumber`
hit count and owns the loop. For every hit that is actually applied, it prints the
existing animation line immediately before applying the repeated effect:

```text
  *<animationName>*
```

The final count is the number of applied hits, not the rolled hit count:

```text
  It hit 1 time!
  It hit N times!
```

Use the singular form only for one applied hit. If the defender faints, stop before the
next animation/effect pair and print the count already applied. On an accuracy miss,
print only:

```text
  The attack missed!
```

Do not print an animation or final hit count on a miss.

For the required concrete examples, assuming the one accuracy roll succeeds:

- A five-hit roll with two applied hits prints two `  *Fury Attack*` lines, each
  immediately followed by its damage effect output, then `  It hit 2 times!`.
- A five-hit roll with five applied hits prints five animation/effect pairs, then
  `  It hit 5 times!`.
- A miss prints `  The attack missed!` only.

`lastAttemptHit` records the single Combo accuracy result: true when the accuracy roll
succeeds and false when it misses. It does not mean that at least one effect was
applied. `Cascade` is the only current reader and uses this result to decide whether to
continue. Keep the existing `IAttempt` API; no per-hit hook is introduced.

### Later implementation surface

- `src/main/java/com/pokemon/attempts/Combo.java`: move the animation print into the
  successful per-hit loop, retain the single accuracy roll, stop on defender faint,
  count applied hits, and use the exact singular/plural messages.
- The Fury Attack composition in `src/main/java/com/pokemon/data/MoveRegistry.java`
  remains `Move → Combo(Probability(0.85), Weighted, FormulaDamage(Exactly(15)))`.
- `Attempt.java`, `IAttempt.java`, and `Battle.java` keep their existing contracts.

### Observable proof

Fury Attack prints one animation immediately before each damage application. If a
defender faints on the second of a rolled five hits, the output reports two and no third
animation or effect is attempted. A successful five-hit combo reports five. A miss has
no animation and no count, and `Cascade` still continues only after a hit.

## Closed decision 4: Jump Kick crash damage

Decision: [issue #6 — Choose crash damage's shape and message](https://github.com/LucaBert00/pokemonOOP/issues/6).

### Settled contract

Add a distinct leaf effect, `CrashDamage`, implementing `IEffect`. Conceptually it takes
an `ITarget` and an `INumber`, resolves the target, applies direct non-formula damage,
records the damage in the battle, and owns the crash-specific message. It is not a
branch on the move name and it is not a replacement of the domain-specific effect with
`DirectDamage`; the video explicitly treats crash damage as a distinct effect with a
distinct message.

The exact crash message is:

```text
  <attacker name> kept going and crashed!
```

The damage amount is not printed. A missed Jump Kick has this exact output order:

1. `<attacker name> used Jump Kick!`
2. `  *Jump Kick*`
3. `  The attack missed!`
4. `  <attacker name> kept going and crashed!`

The later `MoveRegistry` composition is:

```text
Move("Jump Kick", FIGHTING,
  Attempt(
    "Jump Kick",
    Probability(0.95),
    onHit = FormulaDamage(Exactly(100)),
    onMiss = CrashDamage(ATK, Product(MaxHP(ATK), Exactly(0.5))),
    after = NoEffect))
```

This is a shape description, not Java implementation code. Crash damage targets the
attacker and evaluates half of that attacker's maximum HP. Convert the result to the
existing integer damage policy by truncating/flooring it and clamping it to a minimum of
1. Do not cap the damage at current HP. The attacker may faint from the crash.

If the crash faints the attacker, the faint is immediate: no remaining action or
end-of-turn status damage is performed for that fainted attacker, and the opponent's
pending action in the same round is skipped. The later `BattleEngine` change must guard
the current unconditional post-move status-damage path as well as preserving its
existing pending-action guard. This issue does not implement that change.

### Later implementation surface

- Create `src/main/java/com/pokemon/effects/CrashDamage.java` as the named `IEffect`
  leaf with `ITarget` and `INumber` inputs.
- `src/main/java/com/pokemon/data/MoveRegistry.java`: replace Jump Kick's `DirectDamage`
  miss effect with the `CrashDamage` composition and retain `NoEffect` for `after`.
- `src/main/java/com/pokemon/BattleEngine.java`: later guard post-move status damage
  when the attacker has fainted from its own crash.
- Reuse `Attacker`, `MaxHP`, `Product`, and the existing minimum-one damage policy; do
  not add a second damage formula.

### Observable proof

A forced or repeatedly attempted Jump Kick miss produces the four lines above, reduces
the attacker's HP by half its maximum HP using the existing integer policy, and does not
print a direct-damage line. A crash that reaches zero HP ends that attacker's action and
prevents the pending opposing action and the attacker's post-move residual damage.

## Closed decision 5: Leq and Geq disposition

Decision: [issue #7 — Decide Leq/Geq: retype or delete](https://github.com/LucaBert00/pokemonOOP/issues/7).

### Settled contract

Retain `Leq` and `Geq` in their current battle-level shape for Fase 1. Do not delete
them, retype them, change `ICondition`, change `INumber`, add a value-carrying
abstraction, or introduce a numeric lifting adapter.

The exact existing contracts remain:

```text
ICondition<T>        → boolean check(T input)
INumber              → double evaluate(Battle battle)
Leq                  → ICondition<Battle>
Geq                  → ICondition<Battle>
```

Each comparator is constructed with an `INumber` and a `double` threshold. `Leq.check`
evaluates the stored number with the supplied `Battle` and returns whether it is less
than or equal to the threshold. `Geq.check` evaluates it with the supplied `Battle` and
returns whether it is greater than or equal to the threshold.

The video labels these classes as `ICondition<INumber>`, but also defines
`ICondition<T>` as a one-argument predicate and `INumber.evaluate` as requiring a
`Battle`. It does not specify a compile-consistent bridge for those two contracts. The
repository's battle-level shape is therefore an intentional, documented deviation that
avoids inventing a broader DSL change. Neither comparator is used by a registered move,
so there is no current user-visible message or battle output to change.

### Later implementation surface

- `src/main/java/com/pokemon/conditions/Leq.java`: retain unchanged.
- `src/main/java/com/pokemon/conditions/Geq.java`: retain unchanged.
- `src/main/java/com/pokemon/api/ICondition.java`: unchanged.
- `src/main/java/com/pokemon/api/INumber.java`: unchanged.
- `src/main/java/com/pokemon/conditions/For.java`: unchanged; no numeric variant.

### Observable proof

The project compiles with the existing comparator signatures, no registered move gains
new output, and no caller changes. Any future move that needs numeric comparison must
reopen this design with a concrete caller before changing the DSL.

## Documentation changes

Do not edit `CONTEXT.md` in issue #8. After the Java remediation is implemented, replace
the current `CONTEXT.md` fidelity-notes section with the following exact wording. The
wording distinguishes corrected behaviour from the remaining out-of-scope deviation:

```markdown
### Fase 1 remediation contract

The Fase 1 fidelity and output corrections are implemented as follows:

- Decorated move identity is read through the known `IMove` decorator chain, so the
  menu shows the inner `Move` name. The used line is printed by `BattleEngine` before
  execution, including when a decorator rejects the move.
- Applicability is the outer decorator and precondition is the inner decorator for
  Thunder Wave and Will-O-Wisp. Applicability therefore has failure precedence when
  both checks fail.
- Jump Kick uses a distinct `CrashDamage` effect. On a miss it prints
  `  <attacker name> kept going and crashed!` and applies half of the attacker's
  maximum HP as self-damage.
- Body Slam uses 100% accuracy, 85 power, and a 30% conditional paralysis effect.
- Combo performs one accuracy check, prints its animation before each applied hit, and
  reports the number of applied hits. `lastAttemptHit` remains the single accuracy-roll
  result.
- `Leq` and `Geq` intentionally remain `ICondition<Battle>` because the repository's
  `INumber` contract evaluates with `Battle`; no broader numeric lifting seam was
  specified.
- Seeded randomness in `Battle` remains out of scope for Fase 1. The random objects
  remain composable but are not seedable through the battle context.
```

No other `CONTEXT.md` section is changed by this specification.

## Manual battle checklist

Run the executable JAR and record the observed output. Use a roster/setup that makes the
listed scenario possible; where a probabilistic miss or hit is required, repeat the
move until the branch is observed rather than changing the implementation for the
check.

- [ ] A Pokémon with decorated moves sees `Thunder Wave`, `Thunderbolt`, `Thunder`, and
      `Will-O-Wisp` in the menu, not decorator class names.
- [ ] A decorated move failure prints `<attacker> used <move>!` first, then exactly one
      applicable failure line with two-space indentation.
- [ ] Thunder Wave against a defender that is both Ground-type and paralyzed prints
      `  It doesn't affect <name>!`, proving applicability precedence over precondition.
- [ ] Thunder Wave succeeds against a non-Ground, non-paralyzed defender and applies
      paralysis.
- [ ] Will-O-Wisp against a Fire-type prints `  It doesn't affect <name>!` after the
      used line.
- [ ] Jump Kick miss prints, in order, the used line, `  *Jump Kick*`,
      `  The attack missed!`, and `  <attacker> kept going and crashed!`; verify the
      attacker's HP loses half its maximum under the integer policy and no direct-damage
      line appears.
- [ ] If a Jump Kick crash faints its attacker, verify the pending opposing action and
      that attacker's post-move residual damage are skipped.
- [ ] Fury Attack prints one animation line immediately before each applied damage
      effect.
- [ ] Fury Attack against a low-HP defender stops at the early faint and reports the
      applied count, not the rolled count.
- [ ] Body Slam does not miss; on a hit it performs the 85-power damage composition and
      retains its separate 30% paralysis check.
- [ ] Leq/Geq remain unused and produce no new battle output; their documented
      `ICondition<Battle>` disposition is unchanged.
- [ ] Tackle still displays and executes as the leaf move, with its existing 95%
      accuracy and 40-power formula composition.
- [ ] Triple Kick still performs up to three independent attempts, with its existing
      90% rolls, separate animations, 10/20/30 damage values, and cascade stop after a
      miss.
- [ ] A complete player win still reaches the existing win result.
- [ ] A complete player loss still reaches the existing loss result.

For every checked branch, preserve the terminal transcript as the evidence record. Do
not treat a code inspection as a substitute for the battle observation.

## Verification protocol

The later implementation session must perform all of the following:

1. Run `mvn -q compile`.
2. Run `mvn -q clean package`.
3. Run the executable JAR with Java 27 or newer. If `java` on `PATH` is older, use the
   JDK Maven used, as documented in `CONTEXT.md`.
4. Drive the complete manual battle checklist above, including both success and failure
   branches and a full win and loss.
5. Do not add or run JUnit for this remediation. The terminal battle is the integration
   test and the repository has no test dependency.
6. Report explicitly which checklist entries were not observed or which commands could
   not be run. No behaviour may be claimed as verified from source inspection alone.

Issue #8 itself performs only the documentation work. Its completion verification is
that this file is present, contains every closed decision and exact contract above, and
does not modify Java source, `CONTEXT.md`, generated files, or unrelated work.
