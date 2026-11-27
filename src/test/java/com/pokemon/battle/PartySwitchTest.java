package com.pokemon.battle;

import com.pokemon.BattleEngine;
import com.pokemon.api.IMove;
import com.pokemon.attempts.Attempt;
import com.pokemon.conditions.Probability;
import com.pokemon.effects.FormulaDamage;
import com.pokemon.effects.MoveCategory;
import com.pokemon.effects.NoEffect;
import com.pokemon.moves.Move;
import com.pokemon.numbers.Exactly;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Acceptance suite for issue #32: 1v1 and 2v2 party selection, voluntary
 * switching before moves, and forced replacement when the active Pokémon
 * faints while a healthy bench teammate remains.
 */
class PartySwitchTest
{
    private static IMove guaranteedStrike(String name, int power)
    {
        return new Move(name, Element.NORMAL,
            new Attempt(name, new Probability<>(1.0),
                new FormulaDamage(new Exactly(power), MoveCategory.PHYSICAL)));
    }

    private static IMove waitMove()
    {
        return new Move("Wait", Element.NORMAL,
            new Attempt("Wait", new Probability<>(1.0), new NoEffect()));
    }

    private static Battler battler(String name, int maxHp, int speed, List<IMove> moves)
    {
        return new Battler(name, List.of(Element.NORMAL), maxHp, 50,
            100, 100, 100, 100, speed, moves);
    }

    private static Scanner scanner(String input)
    {
        return new Scanner(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8);
    }

    private static String script(String... lines)
    {
        return String.join("\n", lines) + "\n";
    }

    private static String captureStdOut(Runnable action)
    {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try
        {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            action.run();
        }
        finally
        {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void singleMemberPartyConstructorRunsTheSame1v1BattleAsTheTwoBattlerConstructor()
    {
        String input = "1\n".repeat(200);

        BattleEngine twoBattlers = new BattleEngine(
            battler("Pikachu", 300, 120, List.of(guaranteedStrike("Zap", 60))),
            battler("Gengar", 300, 100, List.of(guaranteedStrike("Shadow", 60))),
            scanner(input), 42L);

        BattleEngine singleMemberParties = new BattleEngine(
            new Party(battler("Pikachu", 300, 120, List.of(guaranteedStrike("Zap", 60)))),
            new Party(battler("Gengar", 300, 100, List.of(guaranteedStrike("Shadow", 60)))),
            scanner(input), 42L);

        String legacyOutput = captureStdOut(twoBattlers::run);
        String partyOutput  = captureStdOut(singleMemberParties::run);

        assertEquals(legacyOutput, partyOutput,
            "A 1-member party must reproduce the legacy 1v1 battle exactly");
    }

    @Test
    void voluntarySwitchSwapsActiveBattlerAndClearsVolatileRoundState()
    {
        Battler first  = battler("First", 300, 100, List.of(waitMove()));
        Battler second = battler("Second", 300, 100, List.of(waitMove()));
        Battler foe    = battler("Foe", 300, 100, List.of(waitMove()));
        Party playerParty = new Party(List.of(first, second));
        Party cpuParty    = new Party(List.of(foe));

        Battle battle = new Battle(playerParty, cpuParty, 42L);
        battle.setLastAttemptHit(true);
        battle.setLastDamageDealt(77.0);
        battle.setCurrentMoveElement(Element.FIRE);

        battle.switchAttacker(1);

        assertSame(second, battle.getAttacker(), "Switch must expose the new active battler");
        assertSame(second, playerParty.getActive());
        assertEquals(1, playerParty.getActiveIndex());
        assertFalse(battle.isLastAttemptHit(), "Switch must clear lastAttemptHit");
        assertEquals(0.0, battle.getLastDamageDealt(), "Switch must clear lastDamageDealt");
        assertNull(battle.getCurrentMoveElement(), "Switch must clear currentMoveElement");

        battle.flipTurn();
        assertSame(foe, battle.getAttacker(), "flipTurn must follow the switched-in battler");
        assertSame(second, battle.getDefender());
        battle.flipTurn();
        assertSame(second, battle.getAttacker());
    }

    @Test
    void partyRejectsSwitchingToTheActiveMemberAFaintedMemberOrAnInvalidIndex()
    {
        Battler active  = battler("Active", 300, 100, List.of(waitMove()));
        Battler fainted = battler("Fainted", 300, 100, List.of(waitMove()));
        fainted.takeDamage(999);
        Party party = new Party(List.of(active, fainted));

        assertThrows(IllegalArgumentException.class, () -> party.switchTo(0));
        assertThrows(IllegalArgumentException.class, () -> party.switchTo(1));
        assertThrows(IllegalArgumentException.class, () -> party.switchTo(9));
    }

    @Test
    void partyIsDefeatedOnlyWhenEveryMemberHasFainted()
    {
        Battler first  = battler("First", 300, 100, List.of(waitMove()));
        Battler second = battler("Second", 300, 100, List.of(waitMove()));
        Party party = new Party(List.of(first, second));

        assertFalse(party.isDefeated());
        first.takeDamage(999);
        assertFalse(party.isDefeated(), "One fainted member must not defeat a two-member party");
        second.takeDamage(999);
        assertTrue(party.isDefeated());
    }

    @Test
    void voluntarySwitchResolvesBeforeTheOpponentsMoveAndBattleContinuesToAWinner()
    {
        Battler fragile = battler("First", 1, 10, List.of(waitMove()));
        Battler healthy = battler("Second", 500, 400, List.of(guaranteedStrike("Strike", 200)));
        Battler cpuFirst = battler("CpuOne", 1, 200, List.of(guaranteedStrike("CpuStrike", 100)));
        Battler cpuBench = battler("CpuTwo", 1, 200, List.of(guaranteedStrike("CpuStrike", 100)));

        Party playerParty = new Party(List.of(fragile, healthy));
        Party cpuParty    = new Party(List.of(cpuFirst, cpuBench));

        // [2] Switch Pokémon -> [2] Second -> one Strike on each following round.
        Scanner input = scanner(script("2", "2", "1", "1", "1", "1"));
        String output = captureStdOut(() -> new BattleEngine(playerParty, cpuParty, input, 42L).run());

        assertTrue(output.contains("First, come back!"), output);
        assertTrue(output.contains("Go, Second!"), output);
        assertTrue(output.contains("Second is hurt") || healthy.getCurrentHp() < healthy.getMaxHp(),
            "The opponent's move must resolve against the switched-in battler: " + output);
        assertTrue(cpuParty.isDefeated(), output);
        assertFalse(playerParty.isDefeated(), output);
    }

    @Test
    void forcedFaintReplacementKeepsTheBattleGoingUntilTheWholeSideFaints()
    {
        Battler fragile = battler("Frail", 1, 10, List.of(waitMove()));
        Battler bench   = battler("Bench", 500, 400, List.of(guaranteedStrike("Strike", 300)));
        Battler cpuFirst = battler("CpuFirst", 1, 300, List.of(guaranteedStrike("CpuStrike", 100)));
        Battler cpuBench = battler("CpuBench", 1, 50, List.of(guaranteedStrike("CpuStrike", 100)));

        Party playerParty = new Party(List.of(fragile, bench));
        Party cpuParty    = new Party(List.of(cpuFirst, cpuBench));

        // Frail is KO'd, replacement [2] Bench, then one Strike per remaining round.
        Scanner input = scanner(script("1", "2", "1", "1", "1"));
        String output = captureStdOut(() -> new BattleEngine(playerParty, cpuParty, input, 42L).run());

        assertTrue(output.contains("Frail fainted!"), output);
        assertTrue(output.contains("Go, Bench!"), output);
        assertTrue(output.contains("CpuFirst fainted!"), output);
        assertTrue(output.contains("CpuBench was sent out!"), output);
        assertTrue(cpuParty.isDefeated(), "Battle must end only after the whole CPU party faints");
        assertFalse(playerParty.isDefeated());
    }
}
