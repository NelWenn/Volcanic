package net.vulkanmod.config.ui.shell;

import net.vulkanmod.config.ui.core.KeyAction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiKeysTest {
    @Test
    void mapsVerticalNavigation() {
        assertEquals(KeyAction.UP, UiKeys.actionFor(UiKeys.KEY_UP, 0));
        assertEquals(KeyAction.DOWN, UiKeys.actionFor(UiKeys.KEY_DOWN, 0));
    }

    @Test
    void mapsValueAdjustment() {
        assertEquals(KeyAction.DECREASE, UiKeys.actionFor(UiKeys.KEY_LEFT, 0));
        assertEquals(KeyAction.INCREASE, UiKeys.actionFor(UiKeys.KEY_RIGHT, 0));
    }

    @Test
    void mapsTabToLinearTraversal() {
        assertEquals(KeyAction.NEXT, UiKeys.actionFor(UiKeys.KEY_TAB, 0));
    }

    @Test
    void mapsActivationAndBack() {
        assertEquals(KeyAction.ACTIVATE, UiKeys.actionFor(UiKeys.KEY_ENTER, 0));
        assertEquals(KeyAction.BACK, UiKeys.actionFor(UiKeys.KEY_BACKSPACE, 0));
    }

    @Test
    void escapeClosesRatherThanWalkingBackThroughHistory() {
        assertEquals(KeyAction.CLOSE, UiKeys.actionFor(UiKeys.KEY_ESCAPE, 0));
        assertNotEquals(KeyAction.CLOSE, UiKeys.actionFor(UiKeys.KEY_BACKSPACE, 0));
    }

    @Test
    void mapsHomeAndEnd() {
        assertEquals(KeyAction.HOME, UiKeys.actionFor(UiKeys.KEY_HOME, 0));
        assertEquals(KeyAction.END, UiKeys.actionFor(UiKeys.KEY_END, 0));
    }

    @Test
    void controlKOpensSearch() {
        assertEquals(KeyAction.SEARCH, UiKeys.actionFor(UiKeys.KEY_K, UiKeys.MOD_CONTROL));
        assertEquals(KeyAction.NONE, UiKeys.actionFor(UiKeys.KEY_K, 0));
    }

    @Test
    void unknownKeysAreNone() {
        assertEquals(KeyAction.NONE, UiKeys.actionFor(-1, 0));
        assertEquals(KeyAction.NONE, UiKeys.actionFor(81, 0));
    }

    @Test
    void shiftTabIsPreviousAndPlainTabIsNext() {
        assertEquals(KeyAction.PREVIOUS, UiKeys.actionFor(UiKeys.KEY_TAB, UiKeys.MOD_SHIFT));
        assertEquals(KeyAction.NEXT, UiKeys.actionFor(UiKeys.KEY_TAB, 0));
    }

    @Test
    void controlSurvivesExtraModifierBits() {
        assertEquals(KeyAction.SEARCH,
                UiKeys.actionFor(UiKeys.KEY_K, UiKeys.MOD_CONTROL | UiKeys.MOD_SHIFT));
    }

    @Test
    void shiftDoesNotChangeNonTabKeys() {
        assertEquals(KeyAction.UP, UiKeys.actionFor(UiKeys.KEY_UP, UiKeys.MOD_SHIFT));
        assertEquals(KeyAction.DOWN, UiKeys.actionFor(UiKeys.KEY_DOWN, UiKeys.MOD_SHIFT));
        assertEquals(KeyAction.DECREASE, UiKeys.actionFor(UiKeys.KEY_LEFT, UiKeys.MOD_SHIFT));
        assertEquals(KeyAction.INCREASE, UiKeys.actionFor(UiKeys.KEY_RIGHT, UiKeys.MOD_SHIFT));
        assertEquals(KeyAction.ACTIVATE, UiKeys.actionFor(UiKeys.KEY_ENTER, UiKeys.MOD_SHIFT));
        assertEquals(KeyAction.CLOSE, UiKeys.actionFor(UiKeys.KEY_ESCAPE, UiKeys.MOD_SHIFT));
        assertEquals(KeyAction.BACK, UiKeys.actionFor(UiKeys.KEY_BACKSPACE, UiKeys.MOD_SHIFT));
        assertEquals(KeyAction.HOME, UiKeys.actionFor(UiKeys.KEY_HOME, UiKeys.MOD_SHIFT));
        assertEquals(KeyAction.END, UiKeys.actionFor(UiKeys.KEY_END, UiKeys.MOD_SHIFT));
    }

    @Test
    void controlShortCircuitsOverShift() {
        assertEquals(KeyAction.NONE, UiKeys.actionFor(UiKeys.KEY_TAB, UiKeys.MOD_CONTROL | UiKeys.MOD_SHIFT));
        assertEquals(KeyAction.NONE, UiKeys.actionFor(UiKeys.KEY_UP, UiKeys.MOD_CONTROL | UiKeys.MOD_SHIFT));
    }

    @Test
    void altHeldMeansTheShellOwnsNothing() {
        assertEquals(KeyAction.NONE, UiKeys.actionFor(UiKeys.KEY_TAB, UiKeys.MOD_ALT));
        assertEquals(KeyAction.NONE, UiKeys.actionFor(UiKeys.KEY_ENTER, UiKeys.MOD_ALT));
        assertEquals(KeyAction.NONE, UiKeys.actionFor(UiKeys.KEY_DOWN, UiKeys.MOD_ALT));
        assertEquals(KeyAction.NONE, UiKeys.actionFor(UiKeys.KEY_ESCAPE, UiKeys.MOD_ALT));
        assertEquals(KeyAction.NONE, UiKeys.actionFor(UiKeys.KEY_K, UiKeys.MOD_CONTROL | UiKeys.MOD_ALT));
    }

    @Test
    void superIsTheMacAliasOfControl() {
        assertEquals(KeyAction.SEARCH, UiKeys.actionFor(UiKeys.KEY_K, UiKeys.MOD_SUPER));
        assertEquals(KeyAction.NONE, UiKeys.actionFor(UiKeys.KEY_ENTER, UiKeys.MOD_SUPER));
        assertEquals(KeyAction.NONE, UiKeys.actionFor(UiKeys.KEY_TAB, UiKeys.MOD_SUPER));
        assertEquals(KeyAction.NONE, UiKeys.actionFor(UiKeys.KEY_BACKSPACE, UiKeys.MOD_SUPER));
    }

    @Test
    void lockModifiersDoNotSuppressMappings() {
        int capsLock = 0x0010;
        int numLock = 0x0020;
        assertEquals(KeyAction.DOWN, UiKeys.actionFor(UiKeys.KEY_DOWN, capsLock));
        assertEquals(KeyAction.ACTIVATE, UiKeys.actionFor(UiKeys.KEY_ENTER, numLock));
        assertEquals(KeyAction.PREVIOUS, UiKeys.actionFor(UiKeys.KEY_TAB, capsLock | UiKeys.MOD_SHIFT));
    }
}
