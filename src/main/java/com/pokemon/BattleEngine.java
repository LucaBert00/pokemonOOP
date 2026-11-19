package com.pokemon;

import com.pokemon.api.IMove;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;
import com.pokemon.battle.TurnOrder;
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
 * Handles turn order, paralysis checks, move selection, and win detection.
 */

public class BattleEngine
{
    private final Battler player;
    private final Battler cpu;
    private final Scanner scanner;
    private final Random  random;

    public BattleEngine(Battler player, Battler cpu, Scanner scanner)
    {
        this(player, cpu, scanner, new Random());
    }

    public BattleEngine(Battler player, Battler cpu, Scanner scanner, long seed)
    {
        this(player, cpu, scanner, new Random(seed));
    }

    public BattleEngine(Battler player, Battler cpu, Scanner scanner, Random random)
    {
        this.player  = player;
        this.cpu     = cpu;
        this.scanner = scanner;
        this.random  = Objects.requireNonNull(random, "random must not be null");
    }

    // ── Public entry point ────────────────────────────────────────────────────
    public void run()
    {
        printDivider('=', 56);
        System.out.println("  ⚔  BATTLE START");
        System.out.println("  " + player.getName() + " vs " + cpu.getName());
        printDivider('=', 56);

        int turnNumber = 1;

        while (!player.isFainted() && !cpu.isFainted())
        {
            System.out.println("\n  ─── Turn " + turnNumber + " ───");

            // Simultaneous blind selection: collect both choices before executing either
            IMove playerMove = promptPlayerMove();
            IMove cpuMove    = pickCpuMove(cpu.getMoves());

            // Resolve round order by move priority, effective speed, and seeded tie-break
            TurnOrder.RoundPlan plan = TurnOrder.resolve(player, playerMove, cpu, cpuMove, random);

            // One Battle per round, oriented with first mover as attacker
            Battle battle = plan.createBattle(random);

            executeHalfTurn(battle, plan.firstMove());

            if (!player.isFainted() && !cpu.isFainted())
            {
                battle.flipTurn();
                executeHalfTurn(battle, plan.secondMove());
            }

            // Residual damage ticks only while the battle is undecided; a KO ends the round.
            if (!player.isFainted() && !cpu.isFainted())
            {
                applyEndOfRoundDamage();
            }

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

    // ── Player move prompt ────────────────────────────────────────────────────
    private IMove promptPlayerMove()
    {
        List<IMove> moves = player.getMoves();
        System.out.println("\n  What will " + player.getName() + " do?");

        for (int i = 0; i < moves.size(); i++)
        {
            System.out.printf("    [%d] %s%n", i + 1, moveName(moves.get(i)));
        }

        System.out.print("  > ");

        while (true)
        {
            try
            {
                String line = scanner.nextLine().trim();
                int choice  = Integer.parseInt(line);

                if (choice >= 1 && choice <= moves.size())
                {
                    return moves.get(choice - 1);
                }
            }
            catch (NumberFormatException ignored)
            {
                // Nothing
            }

            System.out.print("  Invalid choice. Enter 1–" + moves.size() + ": ");
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

        if (player.isFainted() && cpu.isFainted())
        {
            System.out.println("  It's a draw!");
        }
        else if (player.isFainted())
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
