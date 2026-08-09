package net.vulkanmod.config.ui.settings;

import net.vulkanmod.api.MenuSetting;
import net.vulkanmod.config.ui.core.ApplyScope;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingSource;
import net.vulkanmod.config.ui.core.SettingType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PluginSettingsTest {

    private static List<MenuSetting> calderaShaped(AtomicBoolean lod, AtomicInteger distance) {
        return List.of(
                MenuSetting.toggle("lod_enabled", "terrain", "caldera.option.lod_enabled",
                        lod::get, lod::set),
                MenuSetting.slider("render_distance", "terrain", "caldera.option.render_distance",
                        4, 128, 4, distance::get, distance::set),
                MenuSetting.toggle("ambient_occlusion", "quality", "caldera.option.ao",
                        () -> true, v -> { }).requiringRestart());
    }

    @Test
    void eachGroupBecomesItsOwnRouteUnderThePlugin() {
        PluginSettings.Converted converted = PluginSettings.convert("caldera",
                calderaShaped(new AtomicBoolean(true), new AtomicInteger(64)));

        assertEquals(3, converted.metas().size());
        assertEquals(RouteId.parse("plugins.caldera.terrain"), converted.metas().get(0).route());
        assertEquals(RouteId.parse("plugins.caldera.quality"), converted.metas().get(2).route());
    }

    @Test
    void theKindDecidesTheTypeAndTheBinding() {
        AtomicInteger distance = new AtomicInteger(64);
        PluginSettings.Converted converted = PluginSettings.convert("caldera",
                calderaShaped(new AtomicBoolean(true), distance));

        assertEquals(SettingType.BOOL, converted.metas().get(0).type());
        assertEquals(SettingType.INT, converted.metas().get(1).type());

        SettingBinding slider = converted.bindings().get(converted.metas().get(1).id());
        assertEquals(4, slider.min());
        assertEquals(128, slider.max());
        assertEquals(4, slider.step());
        slider.set(32);
        assertEquals(32, distance.get(), "the binding must write through to the plugin");
    }

    @Test
    void aChoiceKeepsItsOptions() {
        PluginSettings.Converted converted = PluginSettings.convert("x", List.of(
                MenuSetting.choice("mode", "general", "x.mode", List.of("a", "b"), () -> "a", v -> { })));

        assertEquals(SettingType.ENUM, converted.metas().get(0).type());
        assertEquals(List.of("a", "b"),
                converted.bindings().get(converted.metas().get(0).id()).choices());
    }

    @Test
    void restartOnlyWhereThePluginAskedForIt() {
        PluginSettings.Converted converted = PluginSettings.convert("caldera",
                calderaShaped(new AtomicBoolean(true), new AtomicInteger(64)));

        assertEquals(ApplyScope.INSTANT, converted.metas().get(0).scope());
        assertEquals(ApplyScope.RESTART, converted.metas().get(2).scope());
    }

    @Test
    void theIdCannotCollideWithTheSameModsOwnSettings() {
        PluginSettings.Converted converted = PluginSettings.convert("caldera",
                calderaShaped(new AtomicBoolean(true), new AtomicInteger(64)));

        SettingId id = converted.metas().get(0).id();
        assertEquals("caldera", id.namespace());
        assertTrue(id.toString().contains("plugin/"),
                "the prefix is what keeps it clear of caldera:client/... from the config reader");
        assertNotEquals(SettingId.parse("caldera:client/lod_enabled"), id);
    }

    @Test
    void everySettingCarriesThePluginSourceSoSearchCanGroupIt() {
        for (SettingMeta meta : PluginSettings.convert("caldera",
                calderaShaped(new AtomicBoolean(true), new AtomicInteger(64))).metas()) {
            assertEquals(SettingSource.PLUGINS, meta.source());
        }
    }

    @Test
    void theStartingValueBecomesTheResetTarget() {
        AtomicInteger distance = new AtomicInteger(64);
        PluginSettings.Converted converted = PluginSettings.convert("caldera",
                calderaShaped(new AtomicBoolean(true), distance));

        SettingBinding slider = converted.bindings().get(converted.metas().get(1).id());
        slider.set(8);
        assertEquals(64, slider.defaultValue(), "reset must go back to what the plugin started at");
    }

    @Test
    void aPluginThatDeclaresNothingProducesNothingRatherThanFailing() {
        PluginSettings.Converted converted = PluginSettings.convert("bare", List.of());

        assertTrue(converted.metas().isEmpty());
        assertTrue(converted.bindings().isEmpty());
    }

    @Test
    void aDuplicateKeyIsDroppedRatherThanCrashingTheMenu() {
        PluginSettings.Converted converted = PluginSettings.convert("x", List.of(
                MenuSetting.toggle("same", "g", "x.a", () -> true, v -> { }),
                MenuSetting.toggle("same", "g", "x.b", () -> false, v -> { })));

        assertEquals(1, converted.metas().size());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> PluginSettings.convert(" ", List.of()));
        assertThrows(IllegalArgumentException.class, () -> PluginSettings.convert("x", null));
    }
}
