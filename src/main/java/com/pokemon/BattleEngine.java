package com.pokemon;

import com.pokemon.api.IMove;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;
import com.pokemon.battle.Party;
import com.pokemon.battle.TurnOrder;
import com.pokemon.battle.Weather;
import com.pokemon.conditions.Probability;
import com.pokemon.moves.Move;
import com.pokemon.moves.WithApplicability;
import com.pokemon.moves.WithPrecondition;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;

/**
 * Drives the terminal battle loop.
 * Handles turn order, paralysis checks, move or switch selection, forced
 * replacement after a faint, and win detection.
 */

public class BattleEngine
{
    private final Party   playerParty;
    private final Party   cpuParty;
    private final Scanner scanner;
    private final Random  random;
    private Weather weather = Weather.NONE;

    /** Active battlers, kept in sync with the parties after switches and replacements. */
    private Battler player;
    private Battler cpu;

    public BattleEngine(Battler player, Battler cpu, Scanner scanner)
    {
        this(new Party(player), new Party(cpu), scanner, new Random());
    }

    public BattleEngine(Battler player, Battler cpu, Scanner scanner, long seed)
    {
        this(new Party(player), new Party(cpu), scanner, new Random(seed));
    }

    public BattleEngine(Battler player, Battler cpu, Scanner scanner, Random random)
    {
        this(new Party(player), new Party(cpu), scanner, random);
    }

    public BattleEngine(Party playerParty, Party cpuParty, Scanner scanner)
    {
        this(playerParty, cpuParty, scanner, new Random());
    }

    public BattleEngine(Party playerParty, Party cpuParty, Scanner scanner, long seed)
    {
        this(playerParty, cpuParty, scanner, new Random(seed));
    }

    public BattleEngine(Party playerParty, Party cpuParty, Scanner scanner, Random random)
    {
        this.playerParty = Objects.requireNonNull(playerParty, "playerParty must not be null");
        this.cpuParty    = Objects.requireNonNull(cpuParty, "cpuParty must not be null");
        this.scanner     = Objects.requireNonNull(scanner, "scanner must not be null");
        this.random      = Objects.requireNonNull(random, "random must not be null");
        syncActives();
    }

    private void syncActives()
    {
        player = playerParty.getActive();
        cpu    = cpuParty.getActive();
    }

    // ── Public entry point ────────────────────────────────────────────────────
    public void run()
    {
        printDivider('=', 56);
        System.out.println("  ⚔  BATTLE START");
        System.out.println("  " + player.getName() + " vs " + cpu.getName());
        printDivider('=', 56);

        int turnNumber = 1;

        while (!playerParty.isDefeated() && !cpuParty.isDefeated())
        {
            System.out.println("\n  ─── Turn " + turnNumber + " ───");

            // Simultaneous blind selection: collect both choices before executing either
            RoundAction playerAction = promptPlayerAction();
            IMove cpuMove            = pickCpuMove(cpu.getMoves());

            Battle battle;

            if (playerAction.isSwitch())
            {
                // Switches resolve before any move; the opponent still acts this round.
                battle = new Battle(playerParty, cpuParty, random);
                battle.setWeather(weather);
                System.out.println();
                System.out.println("  " + player.getName() + ", come back!");
                battle.switchAttacker(playerAction.switchIndex());
                syncActives();
                System.out.println("  Go, " + player.getName() + "!");

                battle.flipTurn();
                executeHalfTurn(battle, cpuMove);
            }
            else
            {
                // Resolve round order by move priority, effective speed, and seeded tie-break
                TurnOrder.RoundPlan plan = TurnOrder.resolve(player, playerAction.move(), cpu, cpuMove, random);
                boolean playerFirst      = plan.first() == player;

                battle = new Battle(playerFirst ? playerParty : cpuParty,
                                    playerFirst ? cpuParty : playerParty,
                                    random);
                battle.setWeather(weather);

                executeHalfTurn(battle, plan.firstMove());

                if (!plan.first().isFainted() && !plan.second().isFainted()
                    && !playerParty.isDefeated() && !cpuParty.isDefeated())
                {
                    battle.flipTurn();
                    executeHalfTurn(battle, plan.secondMove());
                }
            }

            // Residual damage ticks only while the battle is undecided; a KO ends the round.
            if (!playerParty.isDefeated() && !cpuParty.isDefeated())
            {
                applyEndOfRoundDamage();
            }

            weather = battle.getWeather();
            resolveFaints();
            printStatus();
            turnNumber++;
        }

        printResult();
    }

    // ── One combatant's half-turn ─────────────────────────────────────────────
    private void executeHalfTurn(Battle battle, IMove move)
    {
        Battler attacker = battle.getAttacker();
        System.out.println();

        // Sleep check — decrements remaining turns; wakes and acts at 0
        if (attacker.isAsleep())
        {
            if (attacker.getSleepTurnsRemaining() > 0)
            {
                attacker.decrementSleepTurns();
                System.out.println(attacker.getName() + " is fast asleep!");
                return;
            }
            attacker.clearMajorStatus();
            System.out.println(attacker.getName() + " woke up!");
        }

        // Freeze check — 20% chance to thaw each turn, otherwise skip
        if (attacker.isFrozen())
        {
            boolean thawed = new Probability<>(0.20).check(battle);
            if (thawed)
            {
                attacker.clearMajorStatus();
                System.out.println(attacker.getName() + " thawed out!");
            }
            else
            {
                System.out.println(attacker.getName() + " is frozen solid and can't move!");
                return;
            }
        }

        // Paralysis check — 25% chance to be fully paralyzed
        if (attacker.isParalyzed())
        {
            boolean cantMove = new Probability<>(0.25).check(battle);
            if (cantMove)
            {
                System.out.println(attacker.getName() + " is paralyzed and can't move!");
                return;
            }
        }

        System.out.println(attacker.getName() + " used " + moveName(move) + "!");
        move.execute(battle);
    }

    // ── Forced replacement after a faint ──────────────────────────────────────
    private void resolveFaints()
    {
        if (playerParty.isDefeated() || cpuParty.isDefeated())
        {
            return;
        }

        if (player.isFainted() && playerParty.hasHealthyBench())
        {
            System.out.println();
            System.out.println("  " + player.getName() + " fainted!");
            promptReplacement();
            System.out.println("  Go, " + player.getName() + "!");
        }

        if (cpu.isFainted() && cpuParty.hasHealthyBench())
        {
            System.out.println();
            System.out.println("  " + cpu.getName() + " fainted!");
            cpuParty.replaceFaintedActive();
            syncActives();
            System.out.println("  " + cpu.getName() + " was sent out!");
        }
    }

    private void promptReplacement()
    {
        List<Battler> members = playerParty.getMembers();
        System.out.println("  Choose your next Pokémon:");
        for (int i = 0; i < members.size(); i++)
        {
            Battler member = members.get(i);
            if (i != playerParty.getActiveIndex() && !member.isFainted())
            {
                System.out.printf("    [%d] %s%n", i + 1, member);
            }
        }
        System.out.print("  > ");

        while (true)
        {
            if (!scanner.hasNextLine())
            {
                throw new IllegalStateException("No replacement input available");
            }
            try
            {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice >= 1 && choice <= members.size()
                    && choice - 1 != playerParty.getActiveIndex()
                    && !members.get(choice - 1).isFainted())
                {
                    playerParty.switchTo(choice - 1);
                    syncActives();
                    return;
                }
            }
            catch (NumberFormatException ignored)
            {
                // Nothing
            }
            System.out.print("  Choose a healthy bench Pokémon: ");
        }
    }

    // ── End-of-round residual status damage ───────────────────────────────────
    private void applyEndOfRoundDamage()
    {
        if (!player.isFainted())
        {
            applyResidualDamage(player);
        }
        if (!cpu.isFainted())
        {
            applyResidualDamage(cpu);
        }
    }

    private void applyResidualDamage(Battler battler)
    {
        if (battler.isBurned())
        {
            int dmg = Math.max(1, battler.getMaxHp() / 16);
            battler.takeDamage(dmg);
            System.out.println(battler.getName() + " is hurt by its burn! (-" + dmg + " HP)");
        }
        else if (battler.isPoisoned())
        {
            int dmg = Math.max(1, battler.getMaxHp() / 16);
            battler.takeDamage(dmg);
            System.out.println(battler.getName() + " is hurt by poison! (-" + dmg + " HP)");
        }
        else if (battler.isBadlyPoisoned())
        {
            int dmg = Math.max(1, battler.getMaxHp() * battler.getToxicCounter() / 16);
            battler.takeDamage(dmg);
            battler.incrementToxicCounter();
            System.out.println(battler.getName() + " is badly hurt by poison! (-" + dmg + " HP)");
        }
    }

    // ── CPU AI (random) ───────────────────────────────────────────────────────
    private IMove pickCpuMove(List<IMove> moves)
    {
        return moves.get(random.nextInt(moves.size()));
    }

    // ── Player action prompt (move or voluntary switch) ───────────────────────
    private RoundAction promptPlayerAction()
    {
        List<IMove> moves = player.getMoves();
        boolean canSwitch = playerParty.hasHealthyBench();
        int switchChoice  = moves.size() + 1;

        System.out.println("\n  What will " + player.getName() + " do?");

        for (int i = 0; i < moves.size(); i++)
        {
            System.out.printf("    [%d] %s%n", i + 1, moveName(moves.get(i)));
        }
        if (canSwitch)
        {
            System.out.printf("    [%d] Switch Pokémon%n", switchChoice);
        }

        System.out.print("  > ");

        while (true)
        {
            if (!scanner.hasNextLine())
            {
                throw new IllegalStateException("No action input available");
            }
            try
            {
                String line = scanner.nextLine().trim();
                int choice  = Integer.parseInt(line);

                if (choice >= 1 && choice <= moves.size())
                {
                    return RoundAction.ofMove(moves.get(choice - 1));
                }
                if (canSwitch && choice == switchChoice)
                {
                    return promptSwitchTarget();
                }
            }
            catch (NumberFormatException ignored)
            {
                // Nothing
            }

            int max = canSwitch ? switchChoice : moves.size();
            System.out.print("  Invalid choice. Enter 1–" + max + ": ");
        }
    }

    private RoundAction promptSwitchTarget()
    {
        List<Battler> members = playerParty.getMembers();

        System.out.println("\n  Choose a Pokémon to switch in:");
        for (int i = 0; i < members.size(); i++)
        {
            Battler member = members.get(i);
            if (i != playerParty.getActiveIndex() && !member.isFainted())
            {
                System.out.printf("    [%d] %s%n", i + 1, member);
            }
        }
        System.out.print("  > ");

        while (true)
        {
            if (!scanner.hasNextLine())
            {
                throw new IllegalStateException("No switch input available");
            }
            try
            {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice >= 1 && choice <= members.size()
                    && choice - 1 != playerParty.getActiveIndex()
                    && !members.get(choice - 1).isFainted())
                {
                    return RoundAction.ofSwitch(choice - 1);
                }
            }
            catch (NumberFormatException ignored)
            {
                // Nothing
            }
            System.out.print("  Choose a healthy bench Pokémon: ");
        }
    }

    private record RoundAction(IMove move, int switchIndex)
    {
        static RoundAction ofMove(IMove move)
        {
            return new RoundAction(Objects.requireNonNull(move, "move must not be null"), -1);
        }

        static RoundAction ofSwitch(int switchIndex)
        {
            return new RoundAction(null, switchIndex);
        }

        boolean isSwitch()
        {
            return move == null;
        }
    }

    // ── Display ───────────────────────────────────────────────────────────────
    private void printStatus()
    {
        System.out.println();
        System.out.printf("  %-20s %s%s%n",
                player.getName(), player.hpBar(), statusTag(player));
        System.out.printf("  %-20s %s%s%n",
                cpu.getName(), cpu.hpBar(), statusTag(cpu));
        if (weather != Weather.NONE)
        {
            System.out.println("  Weather: " + weather.name());
        }
        if (playerParty.getMembers().size() > 1 || cpuParty.getMembers().size() > 1)
        {
            printBench(playerParty);
            printBench(cpuParty);
        }
    }

    private static void printBench(Party party)
    {
        for (int i = 0; i < party.getMembers().size(); i++)
        {
            if (i == party.getActiveIndex())
            {
                continue;
            }
            Battler member = party.getMembers().get(i);
            System.out.printf("  bench: %-14s %s%n", member.getName(), member.hpBar());
        }
    }

    private static String statusTag(Battler b)
    {
        if (b.isParalyzed())
        {
            return " [PAR]";
        }
        if (b.isBurned())
        {
            return " [BRN]";
        }
        if (b.isFrozen())
        {
            return " [FRZ]";
        }
        if (b.isPoisoned())
        {
            return " [PSN]";
        }
        if (b.isBadlyPoisoned())
        {
            return " [TOX]";
        }
        if (b.isAsleep())
        {
            return " [SLP]";
        }
        return "";
    }

    private void printResult()
    {
        printDivider('=', 56);

        if (playerParty.isDefeated() && cpuParty.isDefeated())
        {
            System.out.println("  It's a draw!");
        }
        else if (playerParty.isDefeated())
        {
            System.out.println("  " + cpu.getName() + " wins! You lost...");
        }
        else
        {
            System.out.println("  " + player.getName() + " wins! You won!");
        }
        printDivider('=', 56);
    }

    private static void printDivider(char ch, int len)
    {
        System.out.println("  " + String.valueOf(ch).repeat(len));
    }


    /** Best-effort: extract name from IMove regardless of wrapping decorators. */

    private static String moveName(IMove move)
    {
        if (move instanceof Move)
        {
            return ((Move) move).getName();
        }
        if (move instanceof WithPrecondition)
        {
            return moveName(((WithPrecondition) move).getWrapped());
        }
        if (move instanceof WithApplicability)
        {
            return moveName(((WithApplicability) move).getWrapped());
        }
        return move.getClass().getSimpleName();
    }
}
