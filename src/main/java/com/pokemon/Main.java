package com.pokemon;

import com.pokemon.battle.Battler;
import com.pokemon.data.PokemonLoader;

import java.util.List;
import java.util.Scanner;

/**
 * Entry point. Loads the Pokémon roster from pokemon.csv,
 * lets the player pick their fighter, assigns a random opponent,
 * then starts the battle engine.
 */

public class Main
{
    public static void main(String[] args)
    {
        Scanner scanner = new Scanner(System.in);
        List<Battler> roster = PokemonLoader.loadAll();

        if (roster.isEmpty())
        {
            System.out.println("No Pokémon found! Check pokemon.csv in resources.");
            return;
        }

        printBanner();
        Battler player = pickPokemon(scanner, roster);
        Battler cpu    = pickCpuPokemon(roster, player);

        System.out.println("\n  The CPU chose: " + cpu.getName() + "!");
        System.out.println("  " + cpu);

        new BattleEngine(player, cpu, scanner).run();
        scanner.close();
    }

    // ── Player selection ──────────────────────────────────────────────────────
    private static Battler pickPokemon(Scanner scanner, List<Battler> roster)
    {
        System.out.println("\n  Choose your Pokémon:");

        for (int i = 0; i < roster.size(); i++)
        {
            System.out.printf("    [%d] %s%n", i + 1, roster.get(i));
        }

        System.out.print("\n  > ");

        while (true)
        {
            try
            {
                int choice = Integer.parseInt(scanner.nextLine().trim());

                if (choice >= 1 && choice <= roster.size())
                {
                    Battler pick = roster.get(choice - 1);
                    System.out.println("\n  You chose: " + pick.getName() + "!");
                    return pick;
                }
            }
            catch (NumberFormatException ignored)
            {

            }
            System.out.print("  Enter 1–" + roster.size() + ": ");
        }
    }

    // ── CPU selection (random, not same as player) ────────────────────────────
    private static Battler pickCpuPokemon(List<Battler> roster, Battler player)
    {
        List<Battler> choices = roster.stream()
            .filter(b -> b != player)
            .collect(java.util.stream.Collectors.toList());
        return choices.get((int) (Math.random() * choices.size()));
    }

    // ── Banner ────────────────────────────────────────────────────────────────
    private static void printBanner()
    {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════════════╗");
        System.out.println("  ║      REBUILDING POKÉMON WITH OOP — Terminal         ║");
        System.out.println("  ║      Faithful to the video's DSL architecture       ║");
        System.out.println("  ╚══════════════════════════════════════════════════════╝");
    }
}
