package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShellLayoutTest {
    @Test
    void breakpointsMatchTheDesignSystem() {
        assertEquals(Breakpoint.COMPACT, Breakpoint.forWidth(480));
        assertEquals(Breakpoint.COMPACT, Breakpoint.forWidth(519));
        assertEquals(Breakpoint.MEDIUM, Breakpoint.forWidth(520));
        assertEquals(Breakpoint.MEDIUM, Breakpoint.forWidth(640));
        assertEquals(Breakpoint.MEDIUM, Breakpoint.forWidth(800));
        assertEquals(Breakpoint.WIDE, Breakpoint.forWidth(801));
        assertEquals(Breakpoint.WIDE, Breakpoint.forWidth(854));
    }

    @Test
    void wideLayoutHasThreeColumns() {
        ShellLayout layout = ShellLayout.of(854, 480);
        assertTrue(layout.hasDetailsPanel());
        assertEquals(150, layout.sidebar().width());
        assertEquals(196, layout.details().width());
        assertEquals(854 - 150 - 196, layout.content().width());
    }

    @Test
    void mediumLayoutDropsTheDetailsPanel() {
        ShellLayout layout = ShellLayout.of(640, 360);
        assertFalse(layout.hasDetailsPanel());
        assertTrue(layout.details().isEmpty());
        assertEquals(150, layout.sidebar().width());
        assertEquals(640 - 150, layout.content().width());
    }

    @Test
    void compactLayoutCollapsesTheSidebarToARail() {
        ShellLayout layout = ShellLayout.of(480, 270);
        assertFalse(layout.hasDetailsPanel());
        assertEquals(28, layout.sidebar().width());
        assertEquals(480 - 28, layout.content().width());
    }

    @Test
    void barsSpanTheFullWidthAndReserveTheirHeight() {
        ShellLayout layout = ShellLayout.of(854, 480);
        assertEquals(new Rect(0, 0, 854, 32), layout.topBar());
        assertEquals(new Rect(0, 480 - 34, 854, 34), layout.bottomBar());
        assertEquals(32, layout.sidebar().y());
        assertEquals(480 - 32 - 34, layout.sidebar().height());
        assertEquals(layout.sidebar().height(), layout.content().height());
    }

    @Test
    void columnsTileWithoutGapOrOverlap() {
        ShellLayout layout = ShellLayout.of(854, 480);
        assertEquals(layout.sidebar().right(), layout.content().x());
        assertEquals(layout.content().right(), layout.details().x());
        assertEquals(854, layout.details().right());
    }

    @Test
    void degenerateViewportDoesNotProduceNegativeContent() {
        ShellLayout layout = ShellLayout.of(40, 20);
        assertFalse(layout.content().isEmpty() && layout.content().width() < 0);
        assertTrue(layout.content().width() >= 0);
        assertTrue(layout.content().height() >= 0);
    }
}
