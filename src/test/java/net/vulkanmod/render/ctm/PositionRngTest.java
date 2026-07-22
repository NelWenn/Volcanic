package net.vulkanmod.render.ctm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PositionRngTest {
    @Test
    void sameInputSameSeed() {
        assertEquals(PositionRng.seed(10, 64, -30, 2), PositionRng.seed(10, 64, -30, 2));
    }
    @Test
    void differentPositionsDiffer() {
        assertNotEquals(PositionRng.seed(10, 64, -30, 2), PositionRng.seed(11, 64, -30, 2));
    }
}
