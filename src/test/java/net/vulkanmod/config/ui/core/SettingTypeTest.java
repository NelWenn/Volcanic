package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SettingTypeTest {
    @Test
    void theTypesDrawnAsASliderAreTheOnesTheSliderPathAccepts() {
        assertTrue(SettingType.INT.slider());
        assertFalse(SettingType.BOOL.slider());
        assertFalse(SettingType.ENUM.slider());
    }
}
