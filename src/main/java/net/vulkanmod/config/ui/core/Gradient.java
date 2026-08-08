package net.vulkanmod.config.ui.core;

public record Gradient(int topArgb, int bottomArgb) {
    public boolean isFlat() {
        return topArgb == bottomArgb;
    }
}
