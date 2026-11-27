package com.pokemon;

import com.pokemon.battle.Battler;
import com.pokemon.battle.Party;
import com.pokemon.data.PokemonLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Entry point. Loads the Pokémon roster from pokemon.csv,
 * lets the player choose 1v1 or 2v2, pick their team, assigns a random
 * opponent team, then starts the battle engine.
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
        int partySize = promptPartySize(scanner);
        List<Battler> playerTeam = pickPlayerTeam(scanner, roster, partySize);
        List<Battler> cpuTeam    = pickCpuTeam(roster, playerTeam, partySize, random);

        String cpuNames = cpuTeam.stream().map(Battler::getName).collect(Collectors.joining(" & "));
        System.out.println("\n  The CPU chose: " + cpuNames + "!");
        for (Battler battler : cpuTeam)
        {
            System.out.println("  " + battler);
        }

        new BattleEngine(new Party(playerTeam), new Party(cpuTeam), scanner, random).run();
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

    // ── Party size selection ──
    private static int promptPartySize(Scanner scanner)
    {
        System.out.println("\n  Choose battle mode:");
        System.out.println("    [1] 1v1");
        System.out.println("    [2] 2v2");
        System.out.print("\n  > ");

        while (true)
        {
            if (!scanner.hasNextLine())
            {
                throw new IllegalStateException("No battle mode input available");
            }
            try
            {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice == 1 || choice == 2)
                {
                    return choice;
                }
            }
            catch (NumberFormatException ignored)
            {
                // Nothing
            }
            System.out.print("  Enter 1 or 2: ");
        }
    }

    // ── Player selection ──
    private static List<Battler> pickPlayerTeam(Scanner scanner, List<Battler> roster, int partySize)
    {
        List<Battler> team = new ArrayList<>();
        for (int slot = 1; slot <= partySize; slot++)
        {
            List<Battler> candidates = roster.stream()
                    .filter(b -> !team.contains(b))
                    .collect(Collectors.toList());
            String header = partySize == 1
                    ? "Choose your Pokémon:"
                    : "Choose Pokémon " + slot + " of " + partySize + ":";
            team.add(pickPokemon(scanner, candidates, header));
        }
        return team;
    }

    private static Battler pickPokemon(Scanner scanner, List<Battler> candidates, String header)
    {
        System.out.println("\n  " + header);
        for (int i = 0; i < candidates.size(); i++)
        {
            System.out.printf("    [%d] %s%n", i + 1, candidates.get(i));
        }
        System.out.print("\n  > ");
        while (true)
        {
            try
            {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice >= 1 && choice <= candidates.size())
                {
                    Battler pick = candidates.get(choice - 1);
                    System.out.println("\n  You chose: " + pick.getName() + "!");
                    return pick;
                }
            }
            catch (NumberFormatException ignored)
            {
            }
            System.out.print("  Enter 1-" + candidates.size() + ": ");
        }
    }

    // ── CPU selection (random, distinct from the player's team) ──
    private static List<Battler> pickCpuTeam(List<Battler> roster, List<Battler> playerTeam,
                                             int partySize, Random random)
    {
        List<Battler> candidates = roster.stream()
                .filter(b -> !playerTeam.contains(b))
                .collect(Collectors.toList());

        List<Battler> team = new ArrayList<>();
        for (int i = 0; i < partySize; i++)
        {
            team.add(candidates.remove(random.nextInt(candidates.size())));
        }
        return team;
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
