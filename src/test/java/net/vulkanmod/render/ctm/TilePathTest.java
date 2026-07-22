package net.vulkanmod.render.ctm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TilePathTest {
    @Test
    void numericTokenResolvesSibling() {
        assertEquals("minecraft:optifine/ctm/leaves/0",
            TilePath.resolve("0", "optifine/ctm/leaves", "minecraft"));
    }
    @Test
    void tildeResolvesToOptifineRoot() {
        assertEquals("minecraft:optifine/x/y",
            TilePath.resolve("~/x/y", "optifine/ctm/leaves", "minecraft"));
    }
    @Test
    void namespacedTokenIsAbsolute() {
        assertEquals("nx:blocks/thing",
            TilePath.resolve("nx:blocks/thing", "optifine/ctm/leaves", "minecraft"));
    }
}
