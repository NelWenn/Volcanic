package net.vulkanmod.config.ui.core;

public enum SettingType {
    BOOL,
    INT,
    ENUM;

    public boolean slider() {
        return this == INT;
    }
}
