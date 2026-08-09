package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SettingMetaTest {
    private static SettingMeta.Builder valid() {
        return new SettingMeta.Builder(
                SettingId.parse("vulkanmod:display.window_mode"),
                RouteId.parse("display.general"),
                "vulkanmod.setting.window_mode",
                SettingType.ENUM,
                SettingSource.VOLCANIC);
    }

    @Test
    void carriesEveryFieldItWasBuiltWith() {
        SettingMeta meta = valid().scope(ApplyScope.WINDOW).advanced(true).build();
        assertEquals("vulkanmod", meta.id().namespace());
        assertEquals(RouteId.parse("display.general"), meta.route());
        assertEquals(SettingType.ENUM, meta.type());
        assertEquals(ApplyScope.WINDOW, meta.scope());
        assertTrue(meta.advanced());
        assertFalse(meta.experimental());
    }

    @Test
    void defaultsToInstantAndNoFlags() {
        SettingMeta meta = valid().build();
        assertEquals(ApplyScope.INSTANT, meta.scope());
        assertFalse(meta.advanced());
        assertFalse(meta.experimental());
        assertFalse(meta.recommended());
        assertNull(meta.descriptionKey());
        assertNull(meta.performance());
        assertNull(meta.visual());
    }

    @Test
    void rejectsAnUnratedImpactInsteadOfStoringNull() {
        assertThrows(IllegalArgumentException.class, () -> valid().performance(null));
        assertThrows(IllegalArgumentException.class, () -> valid().visual(null));
        SettingMeta meta = valid().performance(ImpactLevel.HIGH).visual(ImpactLevel.NONE).build();
        assertEquals(ImpactLevel.HIGH, meta.performance());
        assertEquals(ImpactLevel.NONE, meta.visual());
    }

    @Test
    void rejectsEveryMissingRequiredField() {
        assertThrows(IllegalArgumentException.class, () -> new SettingMeta.Builder(
                null, RouteId.parse("display.general"), "k", SettingType.BOOL, SettingSource.VOLCANIC));
        assertThrows(IllegalArgumentException.class, () -> new SettingMeta.Builder(
                SettingId.parse("a:b"), null, "k", SettingType.BOOL, SettingSource.VOLCANIC));
        assertThrows(IllegalArgumentException.class, () -> new SettingMeta.Builder(
                SettingId.parse("a:b"), RouteId.parse("display.general"), "  ",
                SettingType.BOOL, SettingSource.VOLCANIC));
        assertThrows(IllegalArgumentException.class, () -> new SettingMeta.Builder(
                SettingId.parse("a:b"), RouteId.parse("display.general"), "k", null, SettingSource.VOLCANIC));
    }

    @Test
    void rejectsANullScope() {
        assertThrows(IllegalArgumentException.class, () -> valid().scope(null));
    }

    @Test
    void aBlankDescriptionKeyIsRejectedRatherThanStoredAsBlank() {
        assertThrows(IllegalArgumentException.class, () -> valid().descriptionKey(""));
    }
}
