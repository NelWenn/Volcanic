package net.vulkanmod.config.ui.core;

public enum SettingType {
    BOOL,
    INT,
    FLOAT,
    ENUM;

    public boolean slider() {
        return this == INT || this == FLOAT;
    }
}
