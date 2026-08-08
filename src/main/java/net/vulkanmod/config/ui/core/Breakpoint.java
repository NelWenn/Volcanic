package net.vulkanmod.config.ui.core;

public enum Breakpoint {
    COMPACT,
    MEDIUM,
    WIDE;

    public static Breakpoint forWidth(int guiWidth) {
        if (guiWidth > 800) {
            return WIDE;
        }
        if (guiWidth >= 520) {
            return MEDIUM;
        }
        return COMPACT;
    }
}
