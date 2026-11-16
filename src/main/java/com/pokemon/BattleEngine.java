package com.pokemon;

import com.pokemon.api.IMove;
import com.pokemon.battle.Battle;
import com.pokemon.battle.Battler;
import com.pokemon.conditions.Probability;
import com.pokemon.moves.Move;
import com.pokemon.moves.WithApplicability;
import com.pokemon.moves.WithPrecondition;

import java.util.List;
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

    public BattleEngine(Battler player, Battler cpu, Scanner scanner)
    {
        this.player  = player;
        this.cpu     = cpu;
        this.scanner = scanner;
    }

    // ── Public entry point ────────────────────────────────────────────────────
    public void run()
    {
        printDivider('=', 56);
        System.out.println("  ⚔  BATTLE START");
        System.out.println("  " + player.getName() + " vs " + cpu.getName());
        printDivider('=', 56);

        // Speed determines who goes first
        boolean playerFirst = player.getSpeed() >= cpu.getSpeed();

        int turnNumber = 1;

        while (!player.isFainted() && !cpu.isFainted())
        {
            System.out.println("\n  ─── Turn " + turnNumber + " ───");
            printStatus();

            IMove playerMove = promptPlayerMove();
            IMove cpuMove    = pickCpuMove(cpu.getMoves());

            // Execute in speed order
            if (playerFirst)
            {
                executeTurn(player, cpu, playerMove, "  [You] ");

                if (!cpu.isFainted())
                {
                    executeTurn(cpu, player, cpuMove, "  [CPU] ");
                }
            }
            else
            {
                executeTurn(cpu, player, cpuMove, "  [CPU] ");

                if (!player.isFainted())
                {
                    executeTurn(player, cpu, playerMove, "  [You] ");
                }
            }
            turnNumber++;
        }

        printResult();
    }

    // ── One combatant's half-turn ─────────────────────────────────────────────
    private void executeTurn(Battler attacker, Battler defender, IMove move, String prefix)
    {
        System.out.println();

        // Sleep check — skip turn; flag cleared when it wears off (external cure)
        if (attacker.isAsleep())
        {
            System.out.println(prefix + attacker.getName() + " is fast asleep!");
            return;
        }

        // Freeze check — 20% chance to thaw each turn, otherwise skip
        if (attacker.isFrozen())
        {
            boolean thawed = new Probability<>(0.20).check(new Battle(attacker, defender));

            if (thawed)
            {
                attacker.setFrozen(false);
                System.out.println(prefix + attacker.getName() + " thawed out!");
            }
            else
            {
                System.out.println(prefix + attacker.getName() + " is frozen solid and can't move!");
                return;
            }
        }

        // Paralysis check — 25% chance to be fully paralyzed
        if (attacker.isParalyzed())
        {
            boolean cantMove = new Probability<>(0.25).check(new Battle(attacker, defender));

            if (cantMove)
            {
                System.out.println(prefix + attacker.getName() + " is paralyzed and can't move!");
                return;
            }
        }

        Battle battle = new Battle(attacker, defender);
        System.out.println(attacker.getName() + " used " + moveName(move) + "!");
        move.execute(battle);

        // End-of-turn status damage
        if (attacker.isBurned())
        {
            int dmg = Math.max(1, attacker.getMaxHp() / 16);
            attacker.takeDamage(dmg);

            System.out.println(prefix + attacker.getName() + " is hurt by its burn! (-" + dmg + " HP)");
        }
        else if (attacker.isPoisoned())
        {
            int dmg = Math.max(1, attacker.getMaxHp() / 8);
            attacker.takeDamage(dmg);

            System.out.println(prefix + attacker.getName() + " is hurt by poison! (-" + dmg + " HP)");
        }
        else if (attacker.isBadlyPoisoned())
        {
            int dmg = Math.max(1, attacker.getMaxHp() / 16);
            attacker.takeDamage(dmg);

            System.out.println(prefix + attacker.getName() + " is badly hurt by poison! (-" + dmg + " HP)");
        }
    }

    // ── CPU AI (random) ───────────────────────────────────────────────────────
    private IMove pickCpuMove(List<IMove> moves)
    {
        return moves.get((int) (Math.random() * moves.size()));
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
