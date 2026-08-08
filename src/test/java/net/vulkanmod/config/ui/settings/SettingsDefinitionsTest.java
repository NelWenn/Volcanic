package net.vulkanmod.config.ui.settings;

import net.vulkanmod.config.ui.core.ApplyScope;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingSource;
import net.vulkanmod.config.ui.core.SettingType;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SettingsDefinitionsTest {
    @Test
    void displayGeneralHasItsFiveSettingsInSpecOrder() {
        assertEquals(List.of("vulkanmod:display.window_mode", "vulkanmod:display.resolution",
                        "vulkanmod:display.refresh_rate", "minecraft:display.vsync",
                        "minecraft:display.framerate_limit"),
                SettingsDefinitions.displayGeneral().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void everyDisplayGeneralSettingSitsOnThatRoute() {
        RouteId route = RouteId.parse("display.general");
        assertTrue(SettingsDefinitions.displayGeneral().stream().allMatch(meta -> route.equals(meta.route())));
    }

    @Test
    void typesSourcesAndScopesMatchTheSettingsMap() {
        List<SettingMeta> settings = SettingsDefinitions.displayGeneral();

        assertEquals(List.of(SettingType.ENUM, SettingType.ENUM, SettingType.ENUM,
                SettingType.BOOL, SettingType.INT), settings.stream().map(SettingMeta::type).toList());
        assertEquals(List.of(SettingSource.VOLCANIC, SettingSource.VOLCANIC, SettingSource.VOLCANIC,
                SettingSource.MINECRAFT, SettingSource.MINECRAFT), settings.stream().map(SettingMeta::source).toList());
        assertEquals(List.of(ApplyScope.WINDOW, ApplyScope.WINDOW, ApplyScope.WINDOW,
                ApplyScope.INSTANT, ApplyScope.INSTANT), settings.stream().map(SettingMeta::scope).toList());
    }

    @Test
    void everySettingCarriesATitleKey() {
        assertTrue(SettingsDefinitions.displayGeneral().stream()
                .allMatch(meta -> meta.titleKey() != null && !meta.titleKey().isBlank()));
    }
}
