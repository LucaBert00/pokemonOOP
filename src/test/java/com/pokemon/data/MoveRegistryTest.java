package com.pokemon.data;

import com.pokemon.api.IMove;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoveRegistryTest
{
    @Test
    void unknownKeyThrowsIllegalArgumentExceptionWithExactMessage()
    {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> MoveRegistry.get("NotARealMove")
        );
        assertEquals("Unknown move: NotARealMove", ex.getMessage());
    }

    @Test
    void knownKeyResolvesToRegisteredMove()
    {
        IMove tackle = MoveRegistry.get("Tackle");
        assertNotNull(tackle);
        assertEquals(0, tackle.priority());
    }
}
