package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RectTest {
    @Test
    void computesEdges() {
        Rect rect = new Rect(10, 20, 100, 50);
        assertEquals(110, rect.right());
        assertEquals(70, rect.bottom());
    }

    @Test
    void containsIsInclusiveOfOriginExclusiveOfEdge() {
        Rect rect = new Rect(10, 20, 100, 50);
        assertTrue(rect.contains(10, 20));
        assertTrue(rect.contains(109, 69));
        assertFalse(rect.contains(110, 69));
        assertFalse(rect.contains(109, 70));
        assertFalse(rect.contains(9, 20));
    }

    @Test
    void insetShrinksOnAllSides() {
        assertEquals(new Rect(14, 24, 92, 42), new Rect(10, 20, 100, 50).inset(4));
    }

    @Test
    void insetCannotProduceNegativeSize() {
        Rect collapsed = new Rect(0, 0, 4, 4).inset(10);
        assertEquals(0, collapsed.width());
        assertEquals(0, collapsed.height());
        assertTrue(collapsed.isEmpty());
    }

    @Test
    void withHeightKeepsOriginAndWidth() {
        assertEquals(new Rect(10, 20, 100, 12), new Rect(10, 20, 100, 50).withHeight(12));
        assertEquals(0, new Rect(0, 0, 10, 10).withHeight(-5).height());
    }
}
