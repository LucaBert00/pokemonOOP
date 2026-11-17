package com.pokemon;

import com.pokemon.battle.Battler;
import com.pokemon.data.PokemonLoader;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * Entry point. Loads the Pokémon roster from pokemon.csv,
 * lets the player pick their fighter, assigns a random opponent,
 * then starts the battle engine.
 *
 * Supports optional CLI flag: {@code --seed <long>} for reproducible runs.
 */
public class Main
{
    public static void main(String[] args)
    {
        Long seed;
        try
        {
            seed = parseSeed(args);
        }
        catch (IllegalArgumentException e)
        {
            System.err.println(e.getMessage());
            return;
        }

        Random random = (seed != null) ? new Random(seed) : new Random();

        Scanner scanner = new Scanner(System.in);
        List<Battler> roster = PokemonLoader.loadAll();
        if (roster.isEmpty())
        {
            System.out.println("No Pokémon found! Check pokemon.csv in resources.");
            return;
        }

        printBanner();
        Battler player = pickPokemon(scanner, roster);
        Battler cpu    = pickCpuPokemon(roster, player, random);
        System.out.println("\n  The CPU chose: " + cpu.getName() + "!");
        System.out.println("  " + cpu);

        new BattleEngine(player, cpu, scanner, random).run();
        scanner.close();
    }

    static Long parseSeed(String[] args)
    {
        if (args == null || args.length == 0)
        {
            return null;
        }
        Long seed = null;
        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if ("--seed".equals(arg))
            {
                if (i + 1 >= args.length)
                {
                    throw new IllegalArgumentException(
                            "Missing value for --seed. Usage: java -jar PokemonOOP-1.0-SNAPSHOT.jar [--seed <long>]");
                }
                seed = parseLongSeed(args[++i]);
            }
            else if (arg.startsWith("--seed="))
            {
                seed = parseLongSeed(arg.substring("--seed=".length()));
            }
            else
            {
                throw new IllegalArgumentException(
                        "Unknown argument: " + arg + ". Usage: java -jar PokemonOOP-1.0-SNAPSHOT.jar [--seed <long>]");
            }
        }
        return seed;
    }

    private static long parseLongSeed(String raw)
    {
        try
        {
            return Long.parseLong(raw);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Invalid --seed value '" + raw + "': expected a 64-bit integer.");
        }
    }

    // ── Player selection ──
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
            System.out.print("  Enter 1-" + roster.size() + ": ");
        }
    }

    // ── CPU selection (random, not same as player) ──
    private static Battler pickCpuPokemon(List<Battler> roster, Battler player, Random random)
    {
        List<Battler> choices = roster.stream()
                .filter(b -> b != player)
                .collect(java.util.stream.Collectors.toList());
        return choices.get(random.nextInt(choices.size()));
    }

    // ── Banner ──
    private static void printBanner()
    {
        System.out.println();
        System.out.println("  ╔════════════════════════════════════════════════════╗");
        System.out.println("  ║          REBUILDING POKÉMON WITH OOP               ║");
        System.out.println("  ║        Faithful to the video's DSL architecture    ║");
        System.out.println("  ╚════════════════════════════════════════════════════╝");
    }
}