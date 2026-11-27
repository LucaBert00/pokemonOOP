# FINDINGS — Audit di fedeltà e roadmap

Audit del progetto rispetto a `TRANSCRIPT-01-rebuilding-pokemon-with-oop.md` e
`TRANSCRIPT-02-accidentally-used-design-patterns.md`, con stato del codice, bug noti e
piano di correzione.

Il progetto è **fedele nell'architettura**, non al 100% nei dettagli. Il DSL del video 1
c'è tutto; i cinque pattern del video 2 ci sono a livello di tipi. Roster, type chart,
status e battle loop sono aggiunte dichiarate, non parti dei video.

## JDK 27

Il progetto è stato portato da Java 11 a Java 27.

- `pom.xml`: rimosse `maven.compiler.source`/`target` 11, aggiunto
  `<maven.compiler.release>27</maven.compiler.release>`.
- `CONTEXT.md` e `AGENTS.md`: aggiornati a Java 27 e ai comandi di build/run.
- `mvn -q clean package`: passa.
- Bytecode verificato: `major version: 71` (Java 27).
- Runtime verificato: Temurin 27+35.
- Due battaglie complete giocate dal JAR: selezione, menu, turni, danno, faint,
  vittoria/sconfitta, uscita con codice 0.

**Attenzione:** `java` sul `PATH` è ancora 1.8.0_401. Con `java -jar` il JAR non parte,
perché è bytecode 71. Usare:

```powershell
& "$env:JAVA_HOME\bin\java" -jar target/PokemonOOP-1.0-SNAPSHOT.jar
```

oppure aggiornare il `PATH`. Maven usa già JDK 27. I warning del build arrivano dalle
librerie di Maven/Jansi, non dal progetto.

## Fedeltà al video 1

### Cosa è fedele

- `IMove`, `IAttempt`, `IEffect`, `ICondition<T>`, `INumber`, `ITarget`.
- `Move(name, element, attempt)`.
- `Attempt(animation, accuracy, onHit, onMiss, after)` con overload che usa `NoEffect`
  per gli slot vuoti.
- `Cascade`: ferma la catena al primo miss.
- `Combo`: un solo tiro di accuratezza, conteggio da `INumber`, effetto ripetuto.
- `Sequence`, `ConditionalEffect`, `NoEffect`.
- `For` come lifting da `ICondition<Battler>` a `ICondition<Battle>`.
- `And`, `Or`, `Not`, `Probability`, `HasElement`, `IsParalyzed`.
- `Exactly`, `Sum`, `Product`, `Quotient`, `MaxHP`, `CurrentHP`, `Level`, `Weighted`,
  `LastDamageDealt`.
- `Attacker`, `Defender`.
- `Tackle` (95%, 40), `Triple Kick` (3 × 90%, 10/20/30), `Fury Attack` nella forma,
  `Thunder Wave` nella forma, `Body Slam` nel danno e nella paralisi al 30%,
  `Jump Kick` nella matematica.

### Scostamenti e stato della remediation Fase 1

| # | Scostamento | Tipo | Dettaglio |
|---|---|---|---|
| 1 | Ordine dei decoratori invertito in Thunder Wave e Will-O-Wisp | Corretto in Fase 1 | `WithApplicability` è ora **fuori** e `WithPrecondition` dentro; l'applicabilità prevale quando entrambe falliscono. |
| 2 | Nomi delle mosse decorate nel menu | Corretto in Fase 1 | Il menu percorre la catena nota dei decoratori e mostra il nome della `Move` interna. |
| 3 | Manca "X used Y!" quando fallisce precondizione/applicabilità | Corretto in Fase 1 | `BattleEngine` stampa la riga prima di ogni esecuzione, anche per un rifiuto del decoratore. |
| 4 | Jump Kick non usa `CrashDamage` | Corretto in Fase 1 | Jump Kick usa l'effetto distinto e il messaggio di crash, con autodanno pari a metà degli HP massimi. |
| 5 | Body Slam ha accuratezza 85% | Corretto in Fase 1 | Body Slam usa accuratezza 100%, potenza 85 e un controllo separato di paralisi al 30%. |
| 6 | Combo: animazione una volta e conteggio sbagliato | Corretto in Fase 1 | L'animazione precede ogni hit applicato e il conteggio finale riferisce gli hit applicati. |
| 7 | `Leq`/`Geq` hanno il tipo sbagliato | Disposizione confermata | Restano `ICondition<Battle>` e non sono usati da mosse registrate; nessuna modifica DSL è prevista. |
| 8 | Random non è nel contesto di battaglia | Fedeltà minore | Il video suggerisce un randomizer con seed nel `Battle`. Il codice chiama `Math.random()` in `Weighted`, `Probability`, `FormulaDamage`, `Main` e `BattleEngine`. Il design resta componibile, ma non è riproducibile. |

Nota su Body Slam: il transcript a un certo punto dice "80%" per la paralisi, ma è un
lapsus; il valore coerente con il resto della spiegazione è 30%, ed è quello che il codice
usa.

## Fedeltà al video 2

| Pattern | Dove | Verdetto |
|---|---|---|
| **Strategy** | `INumber` in `FormulaDamage`, `IEffect` in `Attempt`, `ICondition` per accuratezza/applicabilità | Corretto. Famiglie di algoritmi intercambiabili. |
| **Composite** | `Sum`/`Product`/`Quotient`, `Sequence`/`ConditionalEffect`, `And`/`Or`/`Not`, `Cascade`/`Combo` | Corretto. Il composto implementa la stessa interfaccia della foglia. |
| **Decorator** | `WithPrecondition`, `WithApplicability` | Corretto: stessa interfaccia, un solo componente avvolto. Ordine corretto in Fase 1 (#10): `WithApplicability` fuori, `WithPrecondition` dentro. |
| **Command** | `IMove` comando, `Battle` receiver, `Battler` tiene le mosse, `BattleEngine` chiama `execute` | Corretto, con una sfumatura: il video identifica l'invoker nel `Battler`; qui il ruolo è diviso tra `Battler` (tiene) e `BattleEngine` (invoca). Non è un fallimento del pattern, è una scelta architetturale. |
| **Null Object** | `NoEffect` come default di `onMiss`/`after` e come else di `ConditionalEffect` | Corretto. Gli slot non sono mai `null`. |

## Stato del progetto

### Funziona (verificato)

- Caricamento di **16 Pokémon** da `pokemon.csv` e **36 mosse** da `MoveRegistry` (tutte raggiungibili dal roster CSV; `PokemonLoaderTest`).
- Suite JUnit 5 attiva: **13 classi di test, 78 test verdi** (`mvn test`).
- Selezione del giocatore, CPU casuale diversa, ordine per Speed.
- Menu, attacco, danno con STAB e type chart, faint, vittoria/sconfitta.
- Controlli di sonno, congelamento e paralisi prima dell'azione.
- Mosse decorate, mosse di stato, cura, drain, multi-hit, OHKO, cambio statistica.
- Due battaglie reali: Pikachu vs Alakazam (vinta con Thunderbolt) e Pikachu vs Onix
  (persa; Thunder Wave bloccata dall'immunità Ground).
- Fase 1 verificata nel terminale: menu decorato, righe e ordine dei fallimenti, successo
  e paralisi di Thunder Wave, immunità Fire di Will-O-Wisp, crash di Jump Kick, animazione
  e conteggio di Fury Attack, Body Slam a 85 di potenza, Tackle, Triple Kick, vittoria e
  sconfitta. L'evidenza completa è registrata nel commento all'issue #14.

### Checklist manuale issue #14

Osservato nel terminale durante questa sessione:

1. Menu con `Thunder Wave`, `Thunderbolt`, `Thunder` e `Will-O-Wisp` senza nomi dei
   decoratori.
2. Fallimenti decorati: riga `used` prima di una sola riga di fallimento indentata.
3. Thunder Wave contro Onix: `It doesn't affect Onix!`.
4. Thunder Wave riuscita e messaggio di paralisi sul difensore non Ground.
5. Will-O-Wisp contro Charmander: immunità Fire dopo la riga `used`.
6. Jump Kick mancato: animazione, miss, crash, nessuna riga di danno diretto.
7. Jump Kick crash fatale: HP dell'attaccante azzerati e risultato immediato.
8. Fury Attack: animazione prima di ogni hit applicato e conteggio osservato.
9. Body Slam: nessun miss nella prova, danno della composizione a 85 osservato.
10. Tackle: miss e hit della composizione 95%/40 osservati.
11. Triple Kick: tre animazioni, danni separati e vittoria osservati.
12. Una vittoria completa del giocatore e una sconfitta completa del giocatore.

Non osservato o non verificabile con il terminale e il roster attuali:

1. Thunder Wave contro un difensore contemporaneamente Ground e già paralizzato: non
   esiste switching né un setup che crei quello stato in una sola battaglia.
2. Jump Kick crash fatale con azione avversaria pendente e danno residuo da saltare: nel
   run fatale osservato il CPU aveva già agito e Machop non aveva danno residuo.
3. Fury Attack con KO anticipato prima del conteggio finale.
4. Body Slam con il controllo separato di paralisi al 30%.
5. Output runtime di Leq/Geq: restano non usati da ogni mossa registrata; la disposizione
   è stata confermata da compilazione e ispezione del contratto, non da una battaglia.

### Fixture di prova approvate

- Machop: sostituzione minima `CrossChop → JumpKick`; nessuna riga o statistica cambia.
- Gengar: sostituzione minima `SwordsDance → ThunderWave`; nessuna riga o statistica cambia.
- Le chiavi CSV e il formato a 14 colonne restano invariati.

### Non funziona (bug del gioco aggiunto)

1. **Il sonno è permanente.** `Sleep` mette `asleep=true`; il motore salta il turno, ma
   niente resetta il flag. Non esiste una cura esterna nonostante il commento. In pratica
   chi si addormenta non agisce più.
2. **Il danno residuo di burn/poison è applicato dopo ogni mezza azione**, non alla fine
   del round. Inoltre, se il turno viene saltato per sonno/freeze/paralisi, il danno
   residuo non viene applicato affatto, perché la funzione esce prima.
3. **Gli status non si escludono.** Un Pokémon può essere paralizzato, bruciato,
   avvelenato e congelato insieme. Lo `statusTag` mostra solo il primo; il danno residuo
   ne applica solo uno.
4. **Toxic non scala.** `BadlyPoison` fa sempre `maxHp / 16`; manca il contatore
   1/16, 2/16, 3/16…
5. **`spAttack`/`spDefense` sono caricati ma inutilizzati.** `FormulaDamage` usa solo
   Attack/Defense. Le statistiche speciali sono decorazione.
6. **`Drain` cura almeno 1 HP anche su danno 0**; `RestoreHP` stampa "recovered X HP"
   anche a HP già pieni, dove la cura reale è 0.
7. **`Between` dice "inclusive" ma il massimo è escluso**; è anche inutilizzato.
8. **`MoveRegistry.get` fa fallback silenzioso a Tackle** su una chiave sconosciuta. Un
   typo nel CSV diventa una mossa normale senza errore.
9. **Codice morto:** `Faint`, `Between`, `Leq`, `Geq`, `LastDamageDealt`,
   `Battle.flipTurn`, `MoveRegistry.displayName`. *(Risolto tra Fase 1 e Fase 2: `displayName` rimosso in #10, `flipTurn` collegato in #20, `Faint`/`Between`/`Leq`/`Geq`/`LastDamageDealt` collegati in #23.)*
10. **Limiti noti post-Fase 2 / Fase 4 (#26–#31):**
   - `CrashDamage` scrive su `battle.setLastDamageDealt(dmg)`: se chi attacca per primo manca `JumpKick` e sopravvive al crash nello stesso round di `Counter`, `Counter` legge il danno da crash come `LastDamageDealt`.
   - Tutti i Pokémon in `pokemon.csv` sono a Livello 50: i rami `< Lv` e scaling `> 30%` di `Guillotine` (#29) sono coperti in JUnit ma non raggiungibili dal roster CSV.
   - `Weather` (#31) persiste indefinitamente finché non viene sovrascritto (nessun contatore di durata turni) e finora solo `Weather.SUN` ha mosse dedicate (`SunnyDay`, `SolarBeam`); `RAIN`/`SANDSTORM`/`HAIL` hanno enum, `HasWeather` e `SetWeather` pronti ma nessuna mossa ancora collegata.

### È vuoto (non implementato, non necessariamente un problema)

- Switching e party multi-Pokémon (`#32`–`#33`), PP e `Struggle` (`#34`), abilità (`#35`–`#36`), `Cascade` dipendente per `TripleKick` (`#37`).
- Critici, oggetti, durata del meteo e mosse per `RAIN`/`SANDSTORM`/`HAIL`.
- Salvataggi, caricamento partita, account.
- Interfaccia web.
- PokéAPI.
- Database.

Nessun file è uno stub vuoto: le classi "vuote" sopra sono implementazioni non collegate
o non usate, non file lasciati a metà.

## Struttura del progetto

La struttura dei package è corretta. `api` contiene le sei interfacce; `moves`,
`attempts`, `effects`, `conditions`, `numbers`, `targets` contengono le implementazioni per
ruolo; `battle` tiene lo stato; `data` tiene registry, loader e type chart; `Main` e
`BattleEngine` sono l'applicazione. Non ci sono dipendenze circolari evidenti e non c'è
nessuno `switch` sul nome di una mossa.

Tre punti da migliorare, senza ristrutturare adesso:

1. **`IMove` non espone il nome.** È la causa del bug del menu. La soluzione pulita è
   aggiungere `String getName()` a `IMove`, farlo delegare dai decoratori e usarlo nel
   motore. È un'estensione pragmatica, non una violazione del video.
2. **`Battle` è un piccolo bus mutabile** (`lastAttemptHit`, `lastDamageDealt`,
   `currentMoveElement`). Funziona, ma se il DSL cresce questi campi diventano
   accoppiamenti nascosti. Per ora va bene.
3. **`BattleEngine` mescola UI, turni, status e scelta mosse.** A 260 righe è gestibile; se
   aggiungi switching, oggetti e abilità, va spezzato in `BattleUI` e `BattleRules`.

## Framework o Java puro?

Java puro + Maven è la scelta giusta. Non aggiungere Spring, Quarkus o un framework web:
il progetto esiste per mostrare il DSL, e un framework aggiungerebbe annotazioni, DI e
lifecycle proprio dove vuoi che si veda la composizione a oggetti. Maven basta; Gradle non
serve. JDK 27 va bene, ma non riscrivere il codice con record/sealed/pattern matching solo
per usare le novità: lo stile classico rispecchia il video ed è più leggibile per chi studia
il design.

Se un giorno aggiungi una web UI o un'API REST, metti un adapter sottile **sopra** il
dominio (`com.pokemon.web`), non dentro `IMove`/`IEffect`. A quel punto un framework web è
giustificato dal layer HTTP, non dal modello.

## CSV o database?

Ora no: il CSV è sufficiente e corretto. Ci sono 16 righe, 27 mosse, caricamento
all'avvio, nessuna scrittura, nessuna query, nessuna concorrenza. Un database aggiungerebbe
schema, migrazioni, JDBC, gestione connessioni e test per zero benefici.

Passare a un database quando arriva almeno uno di questi:

- cache di PokéAPI con molte entità e bisogno di query/filtri;
- salvataggi o statistiche persistenti;
- account utente o backend web multiutente.

In quel caso partire da **SQLite** (file singolo, zero server), non da PostgreSQL. Niente
JPA/Hibernate: JDBC o un DAO piccolo. Tenere il CSV come fallback offline e come fixture di
test.

## PokéAPI: dopo

PokéAPI non richiede di cambiare il DSL. Richiede un **loader** e un **mapper**:

- `PokemonSource` con due implementazioni: `CsvPokemonSource` e `PokeApiPokemonSource`.
- `MoveCatalog` con `RegistryMoveCatalog` (le 27 mosse attuali) e `PokeApiMoveCatalog`.
- Cache su disco delle risposte grezze (JSON), con TTL/ETag; non chiamare l'API a ogni
  avvio.
- CSV come fallback offline.
- Mappare i tipi API su `Element`; per le mosse sconosciute, composizione generica
  (`FormulaDamage` + effetti base) o skip controllato.
- Le nuove meccaniche restano nuove implementazioni di `IEffect`/`ICondition`/`INumber`.

Non generare una classe per ogni mossa API: sarebbe l'opposto del design del video.

## Roadmap

### Fase 1 — Fedeltà al video 1 (completata)

| # | Correzione | File | Verifica |
|---|---|---|---|
| 1 | Scambiare i decoratori in Thunder Wave e Will-O-Wisp: applicabilità fuori, precondizione dentro | `MoveRegistry.java` | Contro un Ground già paralizzato deve apparire "It doesn't affect …", non "But it failed!" |
| 2 | Esporre il nome in `IMove` (o srotolare i decoratori) e usarlo nel menu | `IMove`, `Move`, `WithPrecondition`, `WithApplicability`, `BattleEngine` | Il menu mostra "Thunder Wave", "Thunderbolt", "Thunder", "Will-O-Wisp" |
| 3 | Stampare "X used Y!" anche quando fallisce la precondizione/applicabilità | due decoratori | Onix: prima "Pikachu used Thunder Wave!", poi "It doesn't affect Onix!" |
| 4 | Aggiungere `CrashDamage` e usarlo come `onMiss` di Jump Kick | nuovo `effects/CrashDamage.java`, `MoveRegistry` | Jump Kick che manca: "Machop kept going and crashed!" |
| 5 | Body Slam: accuratezza 100% (o valore della slide) | `MoveRegistry` | Menu e tiro di accuratezza coerenti |
| 6 | Combo: animazione per ogni hit e conteggio degli hit realmente applicati | `Combo.java` | Fury Attack contro un Pokémon a pochi HP: il messaggio non dice 5 se ne ha fatti 2 |
| 7 | (Opzionale) `Leq`/`Geq` come `ICondition<INumber>` o rimozione | `conditions/Leq.java`, `Geq.java` | Compila; nessuna mossa li usa |
| 8 | (Opzionale) randomizer con seed nel `Battle` | `Battle`, `Weighted`, `Probability`, `FormulaDamage` | Fuori scope; resta non seedabile |

### Fase 2 — Regole di gioco e completezza video DSL (completata)

| # | Correzione | File / Issue | Verifica |
|---|---|---|---|
| 9 | Sonno con durata 1–3 turni e messaggio di risveglio | `BattleEngine`, `Sleep`, issue #19 | Dopo N turni il Pokémon si sveglia; verificato in `Fase2SimulationTest` |
| 10 | Un solo status maggiore per volta; danno residuo una volta per round, dopo entrambe le azioni, anche se il turno è saltato | `Battler`, `BattleEngine`, issues #19, #21 | Burn + poison non convivono; il danno residuo arriva a fine round; verificato in `MajorStatusTest` e `ResidualDamageTest` |
| 11 | Toxic con contatore crescente | `Battler`, `BattleEngine`, issue #21 | 1/16, 2/16, 3/16…; verificato in `ResidualDamageTest` e `Fase2SimulationTest` |
| 12 | Scaling al Livello 50 e split Danno Fisico/Speciale via `MoveCategory` | `PokemonLoader`, `FormulaDamage`, `Battler`, issues #17, #18 | Statistiche scalate da base stats; attacco/spAtt e difesa/spDef instradati correttamente; verificato in `FormulaDamageCategoryTest` e `Fase2SimulationTest` |
| 13 | `Drain` non cura su danno 0; `RestoreHP` non stampa cura a HP pieni | `Drain`, `RestoreHP`, issue #22 | I messaggi e gli HP corrispondono alla cura reale; verificato in `HealingEdgeCaseTest` |
| 14 | `MoveRegistry.get` fallisce rumorosamente su chiave sconosciuta | `MoveRegistry`, `PokemonLoader`, issue #22 | Un typo solleva `IllegalArgumentException("Unknown move: <key>")`; verificato in `MoveRegistryTest` |
| 15 | Collegare tutte le primitive video residue (`SetHP`, `PainSplit`, `SelfDestruct`, `Counter`, `Psywave`, `Endeavor`) | `SetHP`, `Leq`, `Geq`, `MoveRegistry`, issue #23 | `Faint`, `LastDamageDealt`, `Between`, `Leq`, `Geq` hanno chiamanti attivi; verificato in `VideoDslMovesTest` |
| 16 | Randomizer con seed in `Battle` e suite di regressione JUnit 5 | `Battle`, `BattleEngine`, `Main`, issues #16, #24 | Determinismo `--seed`, 11 classi di test JUnit 5 iniziali (48 test verdi al termine di Fase 2) |

### Fase 3 — Documentazione e verifica (completata per issue #14 e #24)

- Aggiornate le "Fidelity notes" di `CONTEXT.md` documentando RNG seedabile in `Battle`, scaling Livello 50, categorie di danno, status mutuamente esclusivi, ordine di round/priorità, guardie di cura, mosse video DSL e suite JUnit 5.
- Aggiornata la roadmap di `FINDINGS.md` marcando Fase 1, Fase 2 e Fase 3 come completate.

### Fase 4 — Complete Video DSL & Expanded Battle Mechanics (in corso, map #25)

Completate e verificate in `main`:
- **#26**: `MoveRegistry.keys()` e copertura completa di tutte le 36 mosse in `pokemon.csv` (inclusi `PainSplit`, `SelfDestruct`, `Counter`, `Psywave`, `Endeavor`, `CrossChop`).
- **#27**: Overloads di `Attempt` con accuratezza di default `1.0`.
- **#28**: `Max` e `ConditionalNumber` collegati a `Psywave` e `Brine` (Dewgong).
- **#29**: `Probability(INumber)` con clamp `[0.0, 1.0]` e `Guillotine` con precondizione `Geq(Level(ATK), Level(DEF))` e scaling `(AtkLv - DefLv + 30) / 100`.
- **#30**: Copertura completa di `Or` e dei 6 predicati `Is*` in `StatusConditionsTest`; `Venoshock` (65 -> 130 su avvelenati) su Muk.
- **#31**: `Weather` (`NONE`/`SUN`/`RAIN`/`SANDSTORM`/`HAIL`), `HasWeather`, `SetWeather`, persistenza multi-round in `BattleEngine`, `SunnyDay` e `SolarBeam` su Ninetales (`WeatherTest` verdi; fixed-seed `27` vs `52` per via del `+2.0` in `FormulaDamage`).

Rimangono aperte in `#25`: `#32`–`#33` (1v1/2v2 switching), `#34` (PP & `Struggle`), `#35`–`#36` (abilità), `#37` (`Cascade` dipendente per `TripleKick`), `#38` (sync finale Fase 4).

## Cosa non era stato verificato manualmente (risolto in JUnit 5)

Nel terminale cinque rami edge-case di Fase 1 non potevano essere forzati deterministicamente:
- Difensore simultaneamente Ground e già paralizzato contro `ThunderWave`: verificato in `Fase1EdgeCaseTest` (`WithApplicability` stampa l'immunità per prima).
- Difensore simultaneamente Fire e già bruciato contro `WillOWisp`: verificato in `Fase1EdgeCaseTest` (`WithApplicability` stampa l'immunità per prima).
- `Combo` (`FuryAttack`) che si ferma al faint del difensore: verificato in `Fase1EdgeCaseTest` (si arresta all'hit 1 e riporta "It hit 1 time!").
- `BodySlam` che infligge danno fisico e attiva la paralisi al 30%: verificato in `Fase1EdgeCaseTest` su seed deterministico `1L`.
- Crash fatale di `JumpKick` che non lascia danno residuo da applicare: verificato in `Fase1EdgeCaseTest` (autodanno del 50% max HP, faint immediato, fine turno senza tick residui).

Tutti i 5 rami sono ora coperti e bloccati da test automatici deterministici in `Fase1EdgeCaseTest.java`.
