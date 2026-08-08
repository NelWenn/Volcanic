package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FocusRingTest {

    private static FocusRing threeEnabled() {
        FocusRing tree = new FocusRing();
        tree.register("a", true);
        tree.register("b", true);
        tree.register("c", true);
        return tree;
    }

    @Test
    void startsWithNothingFocused() {
        assertNull(threeEnabled().focused());
    }

    @Test
    void nextFocusesTheFirstEnabledEntry() {
        FocusRing tree = threeEnabled();
        assertTrue(tree.apply(KeyAction.NEXT));
        assertEquals("a", tree.focused());
    }

    @Test
    void nextAdvancesAndWraps() {
        FocusRing tree = threeEnabled();
        tree.focus("c");
        assertTrue(tree.apply(KeyAction.NEXT));
        assertEquals("a", tree.focused());
    }

    @Test
    void previousRetreatsAndWraps() {
        FocusRing tree = threeEnabled();
        tree.focus("a");
        assertTrue(tree.apply(KeyAction.PREVIOUS));
        assertEquals("c", tree.focused());
    }

    @Test
    void downAndUpBehaveLikeNextAndPrevious() {
        FocusRing tree = threeEnabled();
        tree.focus("a");
        tree.apply(KeyAction.DOWN);
        assertEquals("b", tree.focused());
        tree.apply(KeyAction.UP);
        assertEquals("a", tree.focused());
    }

    @Test
    void disabledEntriesAreSkipped() {
        FocusRing tree = threeEnabled();
        tree.setEnabled("b", false);
        tree.focus("a");
        tree.apply(KeyAction.NEXT);
        assertEquals("c", tree.focused());
    }

    @Test
    void focusingADisabledEntryIsRefused() {
        FocusRing tree = threeEnabled();
        tree.setEnabled("b", false);
        assertFalse(tree.focus("b"));
        assertNull(tree.focused());
    }

    @Test
    void focusingAnUnknownEntryIsRefused() {
        assertFalse(threeEnabled().focus("zzz"));
    }

    @Test
    void disablingTheFocusedEntryClearsFocus() {
        FocusRing tree = threeEnabled();
        tree.focus("b");
        tree.setEnabled("b", false);
        assertNull(tree.focused());
    }

    @Test
    void homeAndEndJumpToTheEdges() {
        FocusRing tree = threeEnabled();
        tree.focus("b");
        assertTrue(tree.apply(KeyAction.HOME));
        assertEquals("a", tree.focused());
        assertTrue(tree.apply(KeyAction.END));
        assertEquals("c", tree.focused());
    }

    @Test
    void navigationOnAnAllDisabledTreeDoesNothing() {
        FocusRing tree = threeEnabled();
        tree.setEnabled("a", false);
        tree.setEnabled("b", false);
        tree.setEnabled("c", false);
        assertFalse(tree.apply(KeyAction.NEXT));
        assertNull(tree.focused());
    }

    @Test
    void nonNavigationActionsDoNotMoveFocus() {
        FocusRing tree = threeEnabled();
        tree.focus("b");
        assertFalse(tree.apply(KeyAction.ACTIVATE));
        assertFalse(tree.apply(KeyAction.INCREASE));
        assertFalse(tree.apply(KeyAction.NONE));
        assertEquals("b", tree.focused());
    }

    @Test
    void reRegisteringTheSameIdIsRejected() {
        FocusRing tree = threeEnabled();
        assertThrows(IllegalArgumentException.class, () -> tree.register("a", true));
    }

    @Test
    void clearResetsFocusAndMembership() {
        FocusRing tree = threeEnabled();
        tree.focus("a");
        tree.clear();
        assertEquals(0, tree.size());
        assertNull(tree.focused());
    }

    @Test
    void singleEntryTreeReportsNoMovement() {
        FocusRing tree = new FocusRing();
        tree.register("a", true);
        tree.focus("a");
        assertFalse(tree.apply(KeyAction.NEXT));
        assertFalse(tree.apply(KeyAction.PREVIOUS));
        assertEquals("a", tree.focused());
    }

    @Test
    void wrappingOntoTheOnlyEnabledEntryReportsNoMovement() {
        FocusRing tree = new FocusRing();
        tree.register("a", true);
        tree.register("b", true);
        tree.register("c", true);
        tree.setEnabled("a", false);
        tree.setEnabled("c", false);
        tree.focus("b");
        assertFalse(tree.apply(KeyAction.NEXT));
        assertEquals("b", tree.focused());
    }

    @Test
    void focusingFromUnfocusedReportsMovement() {
        FocusRing tree = threeEnabled();
        assertTrue(tree.apply(KeyAction.NEXT));
    }

    @Test
    void previousFromUnfocusedLandsOnTheLastEntry() {
        FocusRing tree = threeEnabled();
        assertTrue(tree.apply(KeyAction.PREVIOUS));
        assertEquals("c", tree.focused());
    }
}
