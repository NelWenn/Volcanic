package net.vulkanmod.config.ui.shell;

import net.vulkanmod.config.ui.core.KeyAction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiKeysTest {
    @Test
    void mapsVerticalNavigation() {
        assertEquals(KeyAction.UP, UiKeys.actionFor(UiKeys.KEY_UP, false));
        assertEquals(KeyAction.DOWN, UiKeys.actionFor(UiKeys.KEY_DOWN, false));
    }

    @Test
    void mapsValueAdjustment() {
        assertEquals(KeyAction.DECREASE, UiKeys.actionFor(UiKeys.KEY_LEFT, false));
        assertEquals(KeyAction.INCREASE, UiKeys.actionFor(UiKeys.KEY_RIGHT, false));
    }

    @Test
    void mapsTabToLinearTraversal() {
        assertEquals(KeyAction.NEXT, UiKeys.actionFor(UiKeys.KEY_TAB, false));
    }

    @Test
    void mapsActivationAndBack() {
        assertEquals(KeyAction.ACTIVATE, UiKeys.actionFor(UiKeys.KEY_ENTER, false));
        assertEquals(KeyAction.BACK, UiKeys.actionFor(UiKeys.KEY_ESCAPE, false));
    }

    @Test
    void mapsHomeAndEnd() {
        assertEquals(KeyAction.HOME, UiKeys.actionFor(UiKeys.KEY_HOME, false));
        assertEquals(KeyAction.END, UiKeys.actionFor(UiKeys.KEY_END, false));
    }

    @Test
    void controlKOpensSearch() {
        assertEquals(KeyAction.SEARCH, UiKeys.actionFor(UiKeys.KEY_K, true));
        assertEquals(KeyAction.NONE, UiKeys.actionFor(UiKeys.KEY_K, false));
    }

    @Test
    void unknownKeysAreNone() {
        assertEquals(KeyAction.NONE, UiKeys.actionFor(-1, false));
        assertEquals(KeyAction.NONE, UiKeys.actionFor(UiKeys.KEY_Q, false));
    }
}
