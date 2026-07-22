package net.vulkanmod.render.ctm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UvRemapTest {
    @Test
    void midpointMapsToMidpoint() {
        assertEquals(0.75f, UvRemap.remap(0.25f, 0.0f, 0.5f, 0.5f, 1.0f), 1e-6f);
    }
    @Test
    void edgesMapToEdges() {
        assertEquals(0.5f, UvRemap.remap(0.0f, 0.0f, 0.5f, 0.5f, 1.0f), 1e-6f);
        assertEquals(1.0f, UvRemap.remap(0.5f, 0.0f, 0.5f, 0.5f, 1.0f), 1e-6f);
    }
    @Test
    void degenerateOrigReturnsNewStart() {
        assertEquals(0.5f, UvRemap.remap(0.3f, 0.5f, 0.5f, 0.5f, 1.0f), 1e-6f);
    }
}
