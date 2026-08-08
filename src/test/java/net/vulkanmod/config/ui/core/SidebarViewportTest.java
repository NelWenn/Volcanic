package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SidebarViewportTest {
    private static final int NAV_TOP = 32;
    private static final int NAV_HEIGHT = 200;

    private static Rect nav() {
        return new Rect(0, NAV_TOP, 150, NAV_HEIGHT);
    }

    private static ListModel sidebarRows() {
        ListModel model = new ListModel(0);
        model.add(16);
        for (int row = 0; row < 9; row++) {
            model.add(25);
        }
        return model;
    }

    @Test
    void contentYRemovesTheViewportOriginAndAddsScroll() {
        SidebarViewport viewport = new SidebarViewport(nav(), 40);
        assertEquals(40, viewport.contentY(NAV_TOP));
        assertEquals(58, viewport.contentY(NAV_TOP + 18));
    }

    @Test
    void screenTopIsTheExactInverseOfContentY() {
        for (int scroll : new int[]{0, 7, 41}) {
            SidebarViewport viewport = new SidebarViewport(nav(), scroll);
            for (int screenY = NAV_TOP; screenY < NAV_TOP + NAV_HEIGHT; screenY++) {
                assertEquals(screenY, viewport.screenTop(viewport.contentY(screenY)));
            }
        }
    }

    @Test
    void everyPixelOfADrawnRowHitTestsBackToThatRow() {
        ListModel rows = sidebarRows();
        for (int scroll = 0; scroll <= rows.maxScroll(NAV_HEIGHT); scroll++) {
            SidebarViewport viewport = new SidebarViewport(nav(), scroll);
            for (int index = 0; index < rows.count(); index++) {
                int top = viewport.screenTop(rows.offsetOf(index));
                for (int screenY = top; screenY < top + rows.heightOf(index); screenY++) {
                    assertEquals(index, rows.indexAt(viewport.contentY(screenY)),
                            "row " + index + " at scroll " + scroll + " screenY " + screenY);
                }
            }
        }
    }

    @Test
    void containsFollowsTheViewportBounds() {
        SidebarViewport viewport = new SidebarViewport(nav(), 40);
        assertTrue(viewport.contains(0, NAV_TOP));
        assertTrue(viewport.contains(149, NAV_TOP + NAV_HEIGHT - 1));
        assertFalse(viewport.contains(150, NAV_TOP));
        assertFalse(viewport.contains(0, NAV_TOP - 1));
        assertFalse(viewport.contains(0, NAV_TOP + NAV_HEIGHT));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> new SidebarViewport(null, 0));
        assertThrows(IllegalArgumentException.class, () -> new SidebarViewport(nav(), -1));
    }
}
