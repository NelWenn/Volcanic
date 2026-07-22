package net.vulkanmod.render.ctm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BiomeMatcherTest {
    @Test
    void anyMatchesEverything() {
        assertTrue(BiomeMatcher.any().matches("minecraft:plains"));
    }
    @Test
    void positiveList() {
        BiomeMatcher m = BiomeMatcher.parse("taiga snowy_taiga");
        assertTrue(m.matches("minecraft:taiga"));
        assertFalse(m.matches("minecraft:plains"));
    }
    @Test
    void negation() {
        BiomeMatcher m = BiomeMatcher.parse("!plains");
        assertFalse(m.matches("minecraft:plains"));
        assertTrue(m.matches("minecraft:taiga"));
    }
}
