package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FocusModelTest {

    private static FocusModel twoRegions() {
        FocusModel model = new FocusModel();
        model.addRegion("sidebar");
        model.ring("sidebar").register("video", true);
        model.ring("sidebar").register("shaders", true);
        model.addRegion("content");
        model.ring("content").register("card", true);
        return model;
    }

    @Test
    void startsWithNoActiveRegion() {
        assertNull(twoRegions().activeRegion());
        assertNull(twoRegions().focused());
    }

    @Test
    void nextActivatesTheFirstRegionAndItsFirstEntry() {
        FocusModel model = twoRegions();
        assertTrue(model.apply(KeyAction.NEXT));
        assertEquals("sidebar", model.activeRegion());
        assertEquals("video", model.focused());
    }

    @Test
    void previousFromNothingActivatesTheLastRegionAndItsLastEntry() {
        FocusModel model = twoRegions();
        assertTrue(model.apply(KeyAction.PREVIOUS));
        assertEquals("content", model.activeRegion());
        assertEquals("card", model.focused());
    }

    @Test
    void nextMovesBetweenRegionsAndWraps() {
        FocusModel model = twoRegions();
        model.apply(KeyAction.NEXT);
        assertTrue(model.apply(KeyAction.NEXT));
        assertEquals("content", model.activeRegion());
        assertTrue(model.apply(KeyAction.NEXT));
        assertEquals("sidebar", model.activeRegion());
    }

    @Test
    void returningToARegionRestoresItsSelectionInsteadOfAdvancingIt() {
        FocusModel model = twoRegions();
        model.apply(KeyAction.NEXT);
        model.apply(KeyAction.DOWN);
        assertEquals("shaders", model.focused());

        model.apply(KeyAction.NEXT);
        assertEquals("content", model.activeRegion());

        model.apply(KeyAction.NEXT);
        assertEquals("sidebar", model.activeRegion());
        assertEquals("shaders", model.focused());
    }

    @Test
    void previousIntoAVisitedRegionDoesNotJumpToItsLastEntry() {
        FocusModel model = twoRegions();
        model.apply(KeyAction.NEXT);
        assertEquals("video", model.focused());

        model.apply(KeyAction.NEXT);
        assertEquals("content", model.activeRegion());

        model.apply(KeyAction.PREVIOUS);
        assertEquals("sidebar", model.activeRegion());
        assertEquals("video", model.focused());
    }

    @Test
    void cyclingThroughEveryRegionLeavesEverySelectionUntouched() {
        FocusModel model = new FocusModel();
        model.addRegion("sidebar");
        model.ring("sidebar").register("video", true);
        model.ring("sidebar").register("shaders", true);
        model.ring("sidebar").register("mods", true);
        model.addRegion("content");
        model.ring("content").register("card", true);

        model.apply(KeyAction.NEXT);
        model.apply(KeyAction.DOWN);
        model.apply(KeyAction.NEXT);
        String sidebarBefore = model.ring("sidebar").focused();
        String contentBefore = model.ring("content").focused();

        for (int step = 0; step < 4; step++) {
            model.apply(KeyAction.NEXT);
        }

        assertEquals(sidebarBefore, model.ring("sidebar").focused());
        assertEquals(contentBefore, model.ring("content").focused());
    }

    @Test
    void arrowsMoveWithinTheActiveRegionOnly() {
        FocusModel model = twoRegions();
        model.apply(KeyAction.NEXT);
        assertTrue(model.apply(KeyAction.DOWN));
        assertEquals("sidebar", model.activeRegion());
        assertEquals("shaders", model.focused());
    }

    @Test
    void regionsWithNoEnabledEntryAreSkipped() {
        FocusModel model = twoRegions();
        model.ring("content").setEnabled("card", false);
        model.apply(KeyAction.NEXT);
        assertFalse(model.apply(KeyAction.NEXT));
        assertEquals("sidebar", model.activeRegion());
    }

    @Test
    void emptyModelIgnoresEveryAction() {
        FocusModel model = new FocusModel();
        assertFalse(model.apply(KeyAction.NEXT));
        assertFalse(model.apply(KeyAction.DOWN));
        assertNull(model.activeRegion());
    }

    @Test
    void focusRegionRefusesAnUnknownRegion() {
        assertFalse(twoRegions().focusRegion("nope"));
    }

    @Test
    void ringOfAnUnknownRegionThrows() {
        assertThrows(IllegalArgumentException.class, () -> twoRegions().ring("nope"));
    }

    @Test
    void duplicateRegionIsRejected() {
        FocusModel model = twoRegions();
        assertThrows(IllegalArgumentException.class, () -> model.addRegion("sidebar"));
    }

    @Test
    void nonNavigationActionsChangeNothing() {
        FocusModel model = twoRegions();
        model.apply(KeyAction.NEXT);
        assertFalse(model.apply(KeyAction.ACTIVATE));
        assertEquals("video", model.focused());
    }

    @Test
    void clearResetsRegionsAndFocus() {
        FocusModel model = twoRegions();
        model.apply(KeyAction.NEXT);
        model.clear();
        assertEquals(0, model.regionCount());
        assertNull(model.activeRegion());
    }
}
