package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingTypeTest {
    @Test
    void onlyWholeNumberSettingsGetASlider() {
        assertTrue(SettingType.INT.slider());
        for (SettingType type : SettingType.values()) {
            if (type != SettingType.INT) {
                assertFalse(type.slider(), type + " must not be drawn as a slider");
            }
        }
    }

    @Test
    void aKeyBindIsItsOwnTypeAndNotACycler() {
        assertFalse(SettingType.KEY.slider());
        assertTrue(SettingType.valueOf("KEY") == SettingType.KEY);
    }
}
