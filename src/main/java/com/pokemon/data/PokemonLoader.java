package com.pokemon.data;

import com.pokemon.api.IMove;
import com.pokemon.battle.Battler;
import com.pokemon.battle.Element;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Loads Battlers from a CSV resource file.
 *
 * CSV format (one Pokémon per line, no header):
 *   name, element1, element2(or -), maxHp, level, attack, defense,
 *   spAttack, spDefense, speed, move1, move2, move3, move4
 *
 * Elements must match the Element enum names (FIRE, WATER, etc.) or "-".
 * Move names must exist in MoveRegistry.
 */

public class PokemonLoader
{
    public static List<Battler> loadAll()
    {
        List<Battler> roster = new ArrayList<>();
        InputStream resource = PokemonLoader.class.getClassLoader().getResourceAsStream("pokemon.csv");

        if (resource == null)
        {
            System.err.println("[PokemonLoader] pokemon.csv not found in resources!");
            return roster;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8)))
        {
            String line;

            while ((line = br.readLine()) != null)
            {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#"))
                {
                    continue;
                }

                String[] parts = line.split(",");

                if (parts.length < 14)
                {
                    System.err.println("[PokemonLoader] Skipping malformed line: " + line);
                    continue;
                }

                Battler b = parse(parts);

                if (b != null)
                {
                    roster.add(b);
                }
            }
        }
        catch (IOException e)
        {
            System.err.println("[PokemonLoader] Error reading pokemon.csv: " + e.getMessage());
        }
        return roster;
    }

    private static Battler parse(String[] p)
    {
        try
        {
            String name     = p[0].trim();
            List<Element> elements = new ArrayList<>();
            elements.add(Element.valueOf(p[1].trim().toUpperCase()));

            if (!p[2].trim().equals("-"))
            {
                elements.add(Element.valueOf(p[2].trim().toUpperCase()));
            }

            int baseHp      = Integer.parseInt(p[3].trim());
            int level       = Integer.parseInt(p[4].trim());
            int baseAttack  = Integer.parseInt(p[5].trim());
            int baseDefense = Integer.parseInt(p[6].trim());
            int baseSpAttack  = Integer.parseInt(p[7].trim());
            int baseSpDefense = Integer.parseInt(p[8].trim());
            int baseSpeed   = Integer.parseInt(p[9].trim());

            int maxHp     = scaleHp(baseHp, level);
            int attack    = scaleStat(baseAttack, level);
            int defense   = scaleStat(baseDefense, level);
            int spAttack  = scaleStat(baseSpAttack, level);
            int spDefense = scaleStat(baseSpDefense, level);
            int speed     = scaleStat(baseSpeed, level);

            List<IMove> moves = new ArrayList<>();

            for (int i = 10; i < Math.min(p.length, 14); i++)
            {
                String key = p[i].trim();

                if (!key.isEmpty())
                {
                    moves.add(MoveRegistry.get(key));
                }
            }

            return new Battler(name, elements, maxHp, level, attack, defense,
                               spAttack, spDefense, speed, moves);
        }
        catch (Exception e)
        {
            System.err.println("[PokemonLoader] Parse error: " + e.getMessage());
            return null;
        }
    }

    static int scaleHp(int baseHp, int level)
    {
        return (2 * baseHp * level) / 100 + level + 10;
    }

    static int scaleStat(int baseStat, int level)
    {
        return (2 * baseStat * level) / 100 + 5;
    }
}
