# Body Slam accuracy: evidence

Research for issue #3. This document gathers evidence only. It does not choose the value the
code should use; issue #4 ("Choose Body Slam accuracy") makes that decision.

All external pages below were fetched live on 13 November 2026 and quoted from the response
body. Values are transcribed exactly, including the source's own labels (`power` vs
`basePower`, `Accuracy` vs `accuracy`).

## Bottom line

Body Slam's accuracy is **100%** in every core-series generation, Generation I through
Generation IX. Its base power is **85** and its PP is **15**.

**85 is never Body Slam's accuracy in any generation.** It is the move's power. Every source
examined pairs 85 with "power"/"base power" and 100 with "accuracy".

## 1. What the project's own sources say (internal)

The transcripts are the project's source of truth for what the video builds. They are quoted
here as internal evidence, not as external Pokémon data.

`TRANSCRIPT-01-rebuilding-pokemon-with-oop.md`, `Code Example 2`, 52:55–54:08:

- 53:27: "Well first we will make formula damage of exactly 85. So we will inflict 85 of
  damage following the standard formula."
- 53:35: "the condition is that we have a probability of 30%."
- 53:43: "So 30% of the time we will also have this following effect. And what's that effect?
  Well it's paralysis on the defender."

The transcript states power (85) and the paralysis chance (30%). It **never states Body Slam's
accuracy**. The sentence at 53:59, "With an 80% chance you paralyze the defender", contradicts
the 30% stated 24 seconds earlier and is a speaker slip; it is not accuracy evidence.

`TRANSCRIPT-01-rebuilding-pokemon-with-oop.md`, 0:38:

> So there is a lot of variation between the different generations of the Pokemon games, and
> we're not going to stay true to any single one generation or game.

So the video does not target one generation. That does not create ambiguity here: accuracy is
100 in all of them.

`TRANSCRIPT-02-accidentally-used-design-patterns.md` lines 268–269 restate the damage-then-30%
paralysis structure and add no accuracy figure.

## 2. External evidence

### Bulbapedia — infobox

URL: <https://bulbapedia.bulbagarden.net/wiki/Body_Slam_(move)>

Raw wikitext of the infobox (retrieved via `action=raw`):

```
|basepp=15
|maxpp=24
|power=85
|accuracy=100
|gen=I
```

The article body adds:

> Body Slam inflicts damage and has a 30% chance of {{status|Paralysis|paralyzing}} the target.

The infobox carries one accuracy value for the move across generations; the generation-specific
sections describe paralysis targets, not accuracy changes.

### PokéAPI — move 34

URL: <https://pokeapi.co/api/v2/move/34>

Response fields:

```json
"accuracy": 100,
"effect_chance": 30,
"pp": 15,
"power": 85,
"generation": { "name": "generation-i" }
```

The response also contains `"past_values": []` and `"effect_changes": []`. PokéAPI records no
historical change to this move's accuracy, power or PP: the Generation I values are the current
values.

### Serebii — per-generation attackdex pages

Each page renders the same three-column table header and the same row. Quoted text after
stripping markup:

```
Power Points | Base Power | Accuracy
15           | 85         | 100
```

Pages checked, one per generation:

| Gen | URL path | Header and row |
|---|---|---|
| I | `https://www.serebii.net/attackdex-rby/bodyslam.shtml` | `Power Points Base Power Accuracy 15 85 100` |
| II | `https://www.serebii.net/attackdex-gs/bodyslam.shtml` | same |
| III | `https://www.serebii.net/attackdex/bodyslam.shtml` | same |
| IV | `https://www.serebii.net/attackdex-dp/bodyslam.shtml` | same |
| V | `https://www.serebii.net/attackdex-bw/bodyslam.shtml` | same |
| VI | `https://www.serebii.net/attackdex-xy/bodyslam.shtml` | same |
| VII | `https://www.serebii.net/attackdex-sm/bodyslam.shtml` | same |
| VIII | `https://www.serebii.net/attackdex-swsh/bodyslam.shtml` | same |
| IX | `https://www.serebii.net/attackdex-sv/bodyslam.shtml` | same |

### Pokémon Showdown — battle data

URL: <https://raw.githubusercontent.com/smogon/pokemon-showdown/master/data/moves.ts>

The `bodyslam` entry:

```ts
bodyslam: {
    num: 34,
    accuracy: 100,
    basePower: 85,
    category: "Physical",
    name: "Body Slam",
    pp: 15,
    priority: 0,
    flags: { contact: 1, protect: 1, mirror: 1, nonsky: 1, metronome: 1, minimize: 1 },
    secondary: {
        chance: 30,
        status: 'par',
    },
    target: "normal",
    type: "Normal",
    contestType: "Tough",
}
```

`data/mods/gen1/moves.ts` and `data/mods/gen2/moves.ts` contain no `bodyslam` override, so the
base entry above is what Showdown uses for Generations I and II as well.

### Pokémon Database — secondary corroboration

URL: <https://pokemondb.net/move/body-slam>

> Power 85 Accuracy 100 PP 15 (max. 24)

## 3. Generation table

Sources do not disagree on accuracy, so this table is a confirmation map rather than a
disagreement table.

| Generation | Accuracy | Power | Source |
|---|---|---|---|
| I | 100 | 85 | Serebii `attackdex-rby`; Bulbapedia `gen=I`; PokéAPI `generation-i`; Showdown base data (no gen1 override) |
| II | 100 | 85 | Serebii `attackdex-gs`; Showdown base data (no gen2 override) |
| III | 100 | 85 | Serebii `attackdex` |
| IV | 100 | 85 | Serebii `attackdex-dp` |
| V | 100 | 85 | Serebii `attackdex-bw` |
| VI | 100 | 85 | Serebii `attackdex-xy` |
| VII | 100 | 85 | Serebii `attackdex-sm` |
| VIII | 100 | 85 | Serebii `attackdex-swsh`; Bulbapedia `tr8=yes` |
| IX | 100 | 85 | Serebii `attackdex-sv`; Bulbapedia `tm9=yes` |

## 4. Does 85% appear anywhere as Body Slam's accuracy?

No. In every source above, in every generation:

- the **accuracy** field is 100;
- the **power / base power** field is 85.

The code's 85% accuracy matches the move's power, not its accuracy. The transcript supplies 85
as damage at 53:27 and never as an accuracy.

For completeness, the similarly named move **Slam** is a different move and does not supply an
85 either: PokéAPI move 21 has `accuracy: 75`, `power: 80`, `pp: 20`. The 85 in the code is
therefore best explained by the power value, not by confusion with Slam.

## 5. Discrepancies and limits

- **Paralysis rate in Generation I.** Serebii's Generation I page says "Has a one-in-three
  chance of paralyzing the target if it connects", while Bulbapedia, PokéAPI and Showdown give
  30%. This is the secondary effect rate, not accuracy, and it does not change the answer.
  Bulbapedia also notes that in Generation I Body Slam cannot paralyze Normal-type Pokémon; that
  is a target restriction, again not accuracy.
- **Pokémon Legends: Z-A.** Bulbapedia's Generation IX `LegendsZAMove` block lists `power=85`,
  `cooldown=8`, `charge-time=15` and a 30% paralysis chance, with no accuracy percentage. That
  game uses an action-RPG stat block, so it is not evidence for an 85% accuracy.
- **Accuracy stat vs hit chance.** 100 is the move's accuracy stat. In battle it can still be
  altered by accuracy/evasion stages and abilities. This report answers the stat value the issue
  asks for.
- **Coverage.** Four independent sources (Bulbapedia, PokéAPI, Serebii's nine generation pages,
  Pokémon Showdown) agree. No source was found that lists any accuracy other than 100 for Body
  Slam, so there is no per-generation disagreement to report.

## Sources

- Bulbapedia, "Body Slam (move)": <https://bulbapedia.bulbagarden.net/wiki/Body_Slam_(move)>
- PokéAPI, move 34: <https://pokeapi.co/api/v2/move/34>
- PokéAPI, move 21 (Slam): <https://pokeapi.co/api/v2/move/21>
- Serebii Generation I: <https://www.serebii.net/attackdex-rby/bodyslam.shtml>
- Serebii Generation II: <https://www.serebii.net/attackdex-gs/bodyslam.shtml>
- Serebii Generation III: <https://www.serebii.net/attackdex/bodyslam.shtml>
- Serebii Generation IV: <https://www.serebii.net/attackdex-dp/bodyslam.shtml>
- Serebii Generation V: <https://www.serebii.net/attackdex-bw/bodyslam.shtml>
- Serebii Generation VI: <https://www.serebii.net/attackdex-xy/bodyslam.shtml>
- Serebii Generation VII: <https://www.serebii.net/attackdex-sm/bodyslam.shtml>
- Serebii Generation VIII: <https://www.serebii.net/attackdex-swsh/bodyslam.shtml>
- Serebii Generation IX: <https://www.serebii.net/attackdex-sv/bodyslam.shtml>
- Pokémon Showdown move data: <https://raw.githubusercontent.com/smogon/pokemon-showdown/master/data/moves.ts>
- Pokémon Database: <https://pokemondb.net/move/body-slam>
