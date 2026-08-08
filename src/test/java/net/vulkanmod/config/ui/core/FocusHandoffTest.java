package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FocusHandoffTest {
    private static final String SIDEBAR = "sidebar";
    private static final String CONTENT = "content";

    private static FocusModel shellFocus() {
        FocusModel focus = new FocusModel();
        focus.addRegion(SIDEBAR);
        focus.addRegion(CONTENT);
        focus.ring(SIDEBAR).register("rendering", true);
        focus.ring(SIDEBAR).register("quality", true);
        focus.ring(CONTENT).register("rendering.general", true);
        focus.ring(CONTENT).register("rendering.distance", true);
        return focus;
    }

    @Test
    void enteringARegionWithNothingFocusedAdoptsThePreferredEntry() {
        FocusModel focus = shellFocus();
        focus.focusRegion(SIDEBAR);
        focus.ring(SIDEBAR).focus("rendering");

        assertTrue(FocusHandoff.enter(focus, CONTENT, "rendering.general"));
        assertEquals(CONTENT, focus.activeRegion());
        assertEquals("rendering.general", focus.focused());
    }

    @Test
    void enteringARegionKeepsTheFocusItAlreadyHad() {
        FocusModel focus = shellFocus();
        focus.ring(CONTENT).focus("rendering.distance");

        assertTrue(FocusHandoff.enter(focus, CONTENT, "rendering.general"));
        assertEquals(CONTENT, focus.activeRegion());
        assertEquals("rendering.distance", focus.focused());
    }

    @Test
    void aRegionWithNoEntriesReportsThatNothingIsFocused() {
        FocusModel focus = shellFocus();
        focus.ring(CONTENT).clear();

        assertFalse(FocusHandoff.enter(focus, CONTENT, "rendering.general"));
        assertEquals(CONTENT, focus.activeRegion());
        assertNull(focus.focused());
    }

    @Test
    void anUnregisteredPreferredEntryLeavesTheRegionUnfocused() {
        FocusModel focus = shellFocus();

        assertFalse(FocusHandoff.enter(focus, CONTENT, "quality.general"));
        assertEquals(CONTENT, focus.activeRegion());
        assertNull(focus.focused());
    }

    @Test
    void aDisabledPreferredEntryIsNotFocused() {
        FocusModel focus = shellFocus();
        focus.ring(CONTENT).setEnabled("rendering.general", false);

        assertFalse(FocusHandoff.enter(focus, CONTENT, "rendering.general"));
        assertNull(focus.focused());
    }

    @Test
    void rejectsInvalidInput() {
        FocusModel focus = shellFocus();
        assertThrows(IllegalArgumentException.class, () -> FocusHandoff.enter(null, CONTENT, "rendering.general"));
        assertThrows(IllegalArgumentException.class, () -> FocusHandoff.enter(focus, " ", "rendering.general"));
        assertThrows(IllegalArgumentException.class, () -> FocusHandoff.enter(focus, "nope", "rendering.general"));
        assertThrows(IllegalArgumentException.class, () -> FocusHandoff.enter(focus, CONTENT, null));
    }
}
