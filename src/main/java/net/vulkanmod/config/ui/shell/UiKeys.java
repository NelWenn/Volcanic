package net.vulkanmod.config.ui.shell;

import net.vulkanmod.config.ui.core.KeyAction;

public final class UiKeys {
    public static final int KEY_ENTER = 257;
    public static final int KEY_TAB = 258;
    public static final int KEY_ESCAPE = 256;
    public static final int KEY_RIGHT = 262;
    public static final int KEY_LEFT = 263;
    public static final int KEY_DOWN = 264;
    public static final int KEY_UP = 265;
    public static final int KEY_HOME = 268;
    public static final int KEY_END = 269;
    public static final int KEY_K = 75;
    public static final int KEY_Q = 81;

    private UiKeys() {
    }

    public static KeyAction actionFor(int keyCode, boolean control) {
        if (control) {
            return keyCode == KEY_K ? KeyAction.SEARCH : KeyAction.NONE;
        }
        return switch (keyCode) {
            case KEY_UP -> KeyAction.UP;
            case KEY_DOWN -> KeyAction.DOWN;
            case KEY_LEFT -> KeyAction.DECREASE;
            case KEY_RIGHT -> KeyAction.INCREASE;
            case KEY_TAB -> KeyAction.NEXT;
            case KEY_ENTER -> KeyAction.ACTIVATE;
            case KEY_ESCAPE -> KeyAction.BACK;
            case KEY_HOME -> KeyAction.HOME;
            case KEY_END -> KeyAction.END;
            default -> KeyAction.NONE;
        };
    }
}
