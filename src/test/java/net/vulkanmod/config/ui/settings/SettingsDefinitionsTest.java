package net.vulkanmod.config.ui.settings;

import net.vulkanmod.config.ui.core.ApplyScope;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingSource;
import net.vulkanmod.config.ui.core.SettingType;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SettingsDefinitionsTest {
    private static final Path LANG = Path.of("src/main/resources/assets/vulkanmod/lang/en_us.json");

    @Test
    void everyTooltipStringLeftInTheLanguageFileIsWiredToItsSetting() throws IOException {
        List<String> lang = Files.readAllLines(LANG);
        List<String> unwired = new ArrayList<>();
        for (SettingMeta meta : allSettings()) {
            String key = meta.titleKey() + ".tooltip";
            if (declares(lang, key) && !key.equals(meta.descriptionKey())) {
                unwired.add(key);
            }
        }
        assertEquals(List.of(), unwired);
    }

    @Test
    void theTwentySixSettingsThatKeptATooltipAreTheOnesThatDescribeThemselves() {
        assertEquals(26, allSettings().stream().filter(meta -> meta.descriptionKey() != null).count());
    }

    @Test
    void everyDescriptionKeyResolvesInTheLanguageFile() throws IOException {
        List<String> lang = Files.readAllLines(LANG);
        List<String> unresolved = new ArrayList<>();
        for (SettingMeta meta : allSettings()) {
            if (meta.descriptionKey() != null && !declares(lang, meta.descriptionKey())) {
                unresolved.add(meta.descriptionKey());
            }
        }
        assertEquals(List.of(), unresolved);
    }

    @Test
    void theResolutionAndRefreshRateSayTheyNeedExclusiveFullscreen() {
        assertEquals(Optional.of("vulkanmod.ui.disabled.exclusive_fullscreen"),
                SettingsDefinitions.disabledReasonKey(SettingsDefinitions.RESOLUTION));
        assertEquals(Optional.of("vulkanmod.ui.disabled.exclusive_fullscreen"),
                SettingsDefinitions.disabledReasonKey(SettingsDefinitions.REFRESH_RATE));
    }

    @Test
    void theShaderPackSaysAResourcePackOverridesIt() {
        assertEquals(Optional.of("vulkanmod.ui.disabled.core_shader_pack"),
                SettingsDefinitions.disabledReasonKey(SettingsDefinitions.SHADERS_SELECTED_PACK));
    }

    @Test
    void aSettingWithNothingToExplainNamesNoReason() {
        assertEquals(Optional.empty(), SettingsDefinitions.disabledReasonKey(SettingsDefinitions.VSYNC));
        assertEquals(Optional.empty(), SettingsDefinitions.disabledReasonKey(SettingsDefinitions.INDIRECT_DRAW));
        assertThrows(IllegalArgumentException.class, () -> SettingsDefinitions.disabledReasonKey(null));
    }

    @Test
    void everyDisabledReasonKeyResolvesInTheLanguageFile() throws IOException {
        List<String> lang = Files.readAllLines(LANG);
        for (SettingMeta meta : allSettings()) {
            SettingsDefinitions.disabledReasonKey(meta.id()).ifPresent(key ->
                    assertTrue(declares(lang, key), "missing language key " + key));
        }
    }

    private static boolean declares(List<String> lang, String key) {
        return lang.stream().anyMatch(line -> line.trim().startsWith("\"" + key + "\":"));
    }

    private static List<SettingMeta> allSettings() {
        List<SettingMeta> all = new ArrayList<>(SettingsDefinitions.displayGeneral());
        all.addAll(SettingsDefinitions.displayInterface());
        all.addAll(SettingsDefinitions.displayAdvanced());
        all.addAll(renderingSettings());
        all.addAll(performanceSettings());
        all.addAll(qualitySettings());
        all.addAll(advancedSettings());
        all.addAll(SettingsDefinitions.shadersCurrent());
        return all;
    }

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

    @Test
    void displayInterfaceHasItsFourSettingsInSpecOrder() {
        assertEquals(List.of("minecraft:display.gui_scale", "minecraft:display.brightness",
                        "minecraft:display.distortion_effects", "minecraft:display.fov_effects"),
                SettingsDefinitions.displayInterface().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void everyDisplayInterfaceSettingSitsOnThatRoute() {
        RouteId route = RouteId.parse("display.interface");
        assertTrue(SettingsDefinitions.displayInterface().stream().allMatch(meta -> route.equals(meta.route())));
    }

    @Test
    void displayInterfaceTypesSourcesAndScopesMatchTheSettingsMap() {
        List<SettingMeta> settings = SettingsDefinitions.displayInterface();

        assertEquals(List.of(SettingType.INT, SettingType.INT, SettingType.INT, SettingType.INT),
                settings.stream().map(SettingMeta::type).toList());
        assertEquals(List.of(SettingSource.MINECRAFT, SettingSource.MINECRAFT,
                SettingSource.MINECRAFT, SettingSource.MINECRAFT),
                settings.stream().map(SettingMeta::source).toList());
        assertEquals(List.of(ApplyScope.INSTANT, ApplyScope.INSTANT, ApplyScope.INSTANT, ApplyScope.INSTANT),
                settings.stream().map(SettingMeta::scope).toList());
    }

    @Test
    void noDisplayInterfaceSettingIsFlaggedAdvanced() {
        assertTrue(SettingsDefinitions.displayInterface().stream().noneMatch(SettingMeta::advanced));
    }

    @Test
    void displayAdvancedHoldsOnlyTheHiDpiToggle() {
        assertEquals(List.of("vulkanmod:display.disable_hidpi"),
                SettingsDefinitions.displayAdvanced().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void theHiDpiToggleIsAnAdvancedRestartSettingOnItsOwnRoute() {
        SettingMeta meta = SettingsDefinitions.displayAdvanced().get(0);

        assertEquals(RouteId.parse("display.advanced"), meta.route());
        assertEquals(SettingType.BOOL, meta.type());
        assertEquals(SettingSource.VOLCANIC, meta.source());
        assertEquals(ApplyScope.RESTART, meta.scope());
        assertTrue(meta.advanced());
        assertEquals("vulkanmod.options.disableHiDPI", meta.titleKey());
    }

    @Test
    void renderingGeneralHasItsFourSettingsInSpecOrder() {
        assertEquals(List.of("minecraft:rendering.render_distance", "minecraft:rendering.simulation_distance",
                        "minecraft:rendering.chunk_update_priority", "vulkanmod:rendering.chunk_fade_in"),
                SettingsDefinitions.renderingGeneral().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void renderingGeneralTypesSourcesAndScopesMatchTheSettingsMap() {
        List<SettingMeta> settings = SettingsDefinitions.renderingGeneral();

        assertEquals(List.of(SettingType.INT, SettingType.INT, SettingType.ENUM, SettingType.BOOL),
                settings.stream().map(SettingMeta::type).toList());
        assertEquals(List.of(SettingSource.MINECRAFT, SettingSource.MINECRAFT,
                        SettingSource.MINECRAFT, SettingSource.VOLCANIC),
                settings.stream().map(SettingMeta::source).toList());
        assertEquals(List.of(ApplyScope.INSTANT, ApplyScope.INSTANT, ApplyScope.INSTANT, ApplyScope.INSTANT),
                settings.stream().map(SettingMeta::scope).toList());
    }

    @Test
    void renderingResolutionHasItsFourSettingsInSpecOrder() {
        assertEquals(List.of("vulkanmod:vsr.preset", "vulkanmod:vsr.upscaler",
                        "vulkanmod:vsr.render_scale", "vulkanmod:vsr.sharpness"),
                SettingsDefinitions.renderingResolution().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void renderingResolutionTypesSourcesAndScopesMatchTheSettingsMap() {
        List<SettingMeta> settings = SettingsDefinitions.renderingResolution();

        assertEquals(List.of(SettingType.ENUM, SettingType.ENUM, SettingType.INT, SettingType.INT),
                settings.stream().map(SettingMeta::type).toList());
        assertEquals(List.of(SettingSource.VOLCANIC, SettingSource.VOLCANIC,
                        SettingSource.VOLCANIC, SettingSource.VOLCANIC),
                settings.stream().map(SettingMeta::source).toList());
        assertEquals(List.of(ApplyScope.INSTANT, ApplyScope.INSTANT, ApplyScope.INSTANT, ApplyScope.INSTANT),
                settings.stream().map(SettingMeta::scope).toList());
    }

    @Test
    void renderingCullingHasItsFiveSettingsInSpecOrder() {
        assertEquals(List.of("vulkanmod:culling.occlusion", "vulkanmod:culling.mode",
                        "vulkanmod:culling.block_entities", "vulkanmod:culling.particles",
                        "vulkanmod:culling.lod_gpu"),
                SettingsDefinitions.renderingCulling().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void renderingCullingTypesSourcesAndScopesMatchTheSettingsMap() {
        List<SettingMeta> settings = SettingsDefinitions.renderingCulling();

        assertEquals(List.of(SettingType.BOOL, SettingType.ENUM, SettingType.BOOL,
                        SettingType.ENUM, SettingType.BOOL),
                settings.stream().map(SettingMeta::type).toList());
        assertEquals(List.of(ApplyScope.CHUNK_REBUILD, ApplyScope.CHUNK_REBUILD, ApplyScope.CHUNK_REBUILD,
                        ApplyScope.INSTANT, ApplyScope.CHUNK_REBUILD),
                settings.stream().map(SettingMeta::scope).toList());
        assertEquals(List.of(false, true, false, false, true),
                settings.stream().map(SettingMeta::advanced).toList());
    }

    @Test
    void everyRenderingSettingSitsOnTheRouteItWasDeclaredFor() {
        assertTrue(SettingsDefinitions.renderingGeneral().stream()
                .allMatch(meta -> RouteId.parse("rendering.general").equals(meta.route())));
        assertTrue(SettingsDefinitions.renderingResolution().stream()
                .allMatch(meta -> RouteId.parse("rendering.resolution").equals(meta.route())));
        assertTrue(SettingsDefinitions.renderingCulling().stream()
                .allMatch(meta -> RouteId.parse("rendering.culling").equals(meta.route())));
    }

    @Test
    void noRenderingSettingIsFlaggedExperimental() {
        assertTrue(renderingSettings().stream().noneMatch(SettingMeta::experimental));
    }

    @Test
    void everyVolcanicRenderingTitleKeyResolvesInTheLanguageFile() throws IOException {
        List<String> lang = Files.readAllLines(
                Path.of("src/main/resources/assets/vulkanmod/lang/en_us.json"));
        for (SettingMeta meta : renderingSettings()) {
            if (!meta.titleKey().startsWith("vulkanmod.")) {
                continue;
            }
            String entry = "\"" + meta.titleKey() + "\":";
            assertTrue(lang.stream().anyMatch(line -> line.trim().startsWith(entry)),
                    "missing language key " + meta.titleKey());
        }
    }

    private static List<SettingMeta> renderingSettings() {
        List<SettingMeta> all = new ArrayList<>(SettingsDefinitions.renderingGeneral());
        all.addAll(SettingsDefinitions.renderingResolution());
        all.addAll(SettingsDefinitions.renderingCulling());
        return all;
    }

    @Test
    void performanceGeneralHoldsOnlyTheProfileSetting() {
        assertEquals(List.of("vulkanmod:performance.profile"),
                SettingsDefinitions.performanceGeneral().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void performanceGpuHasItsTwoSettingsInSpecOrder() {
        assertEquals(List.of("vulkanmod:performance.indirect_draw",
                        "vulkanmod:performance.unique_opaque_layer"),
                SettingsDefinitions.performanceGpu().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void performanceChunksHasItsTwoSettingsInSpecOrder() {
        assertEquals(List.of("vulkanmod:performance.adaptive_chunk_uploads",
                        "vulkanmod:performance.chunk_uploads_per_frame"),
                SettingsDefinitions.performanceChunks().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void performanceSynchronizationHoldsOnlyTheFrameQueueSetting() {
        assertEquals(List.of("vulkanmod:performance.frame_queue"),
                SettingsDefinitions.performanceSynchronization().stream()
                        .map(meta -> meta.id().toString()).toList());
    }

    @Test
    void performanceTypesSourcesAndScopesMatchTheSettingsMap() {
        List<SettingMeta> settings = performanceSettings();

        assertEquals(List.of(SettingType.ENUM, SettingType.BOOL, SettingType.BOOL,
                        SettingType.BOOL, SettingType.INT, SettingType.INT),
                settings.stream().map(SettingMeta::type).toList());
        assertEquals(List.of(SettingSource.VOLCANIC, SettingSource.VOLCANIC, SettingSource.VOLCANIC,
                        SettingSource.VOLCANIC, SettingSource.VOLCANIC, SettingSource.VOLCANIC),
                settings.stream().map(SettingMeta::source).toList());
        assertEquals(List.of(ApplyScope.CHUNK_REBUILD, ApplyScope.CHUNK_REBUILD, ApplyScope.CHUNK_REBUILD,
                        ApplyScope.INSTANT, ApplyScope.INSTANT, ApplyScope.SWAPCHAIN),
                settings.stream().map(SettingMeta::scope).toList());
    }

    @Test
    void onlyTheTwoTunablesAreFlaggedAdvancedOnThePerformancePages() {
        assertEquals(List.of(false, false, false, false, true, true),
                performanceSettings().stream().map(SettingMeta::advanced).toList());
    }

    @Test
    void noPerformanceSettingIsFlaggedExperimental() {
        assertTrue(performanceSettings().stream().noneMatch(SettingMeta::experimental));
    }

    @Test
    void everyPerformanceSettingSitsOnTheRouteItWasDeclaredFor() {
        assertTrue(SettingsDefinitions.performanceGeneral().stream()
                .allMatch(meta -> RouteId.parse("performance.general").equals(meta.route())));
        assertTrue(SettingsDefinitions.performanceGpu().stream()
                .allMatch(meta -> RouteId.parse("performance.gpu").equals(meta.route())));
        assertTrue(SettingsDefinitions.performanceChunks().stream()
                .allMatch(meta -> RouteId.parse("performance.chunks").equals(meta.route())));
        assertTrue(SettingsDefinitions.performanceSynchronization().stream()
                .allMatch(meta -> RouteId.parse("performance.synchronization").equals(meta.route())));
    }

    @Test
    void everyVolcanicPerformanceTitleKeyResolvesInTheLanguageFile() throws IOException {
        List<String> lang = Files.readAllLines(
                Path.of("src/main/resources/assets/vulkanmod/lang/en_us.json"));
        for (SettingMeta meta : performanceSettings()) {
            if (!meta.titleKey().startsWith("vulkanmod.")) {
                continue;
            }
            String entry = "\"" + meta.titleKey() + "\":";
            assertTrue(lang.stream().anyMatch(line -> line.trim().startsWith(entry)),
                    "missing language key " + meta.titleKey());
        }
    }

    @Test
    void theChunkUploadBudgetStaysOnTheRangeTheDispatcherClamps() {
        assertEquals(1, SettingsDefinitions.CHUNK_UPLOADS_PER_FRAME_MIN);
        assertEquals(16, SettingsDefinitions.CHUNK_UPLOADS_PER_FRAME_MAX);
        assertEquals(1, SettingsDefinitions.CHUNK_UPLOADS_PER_FRAME_STEP);
    }

    @Test
    void theFrameQueueStaysOnTheRangeTheOldMenuOffered() {
        assertEquals(2, SettingsDefinitions.FRAME_QUEUE_MIN);
        assertEquals(5, SettingsDefinitions.FRAME_QUEUE_MAX);
        assertEquals(1, SettingsDefinitions.FRAME_QUEUE_STEP);
        assertEquals(2, SettingsDefinitions.FRAME_QUEUE_DEFAULT);
    }

    private static List<SettingMeta> performanceSettings() {
        List<SettingMeta> all = new ArrayList<>(SettingsDefinitions.performanceGeneral());
        all.addAll(SettingsDefinitions.performanceGpu());
        all.addAll(SettingsDefinitions.performanceChunks());
        all.addAll(SettingsDefinitions.performanceSynchronization());
        return all;
    }

    @Test
    void qualityGeneralHoldsOnlyTheGraphicsMode() {
        assertEquals(List.of("minecraft:quality.graphics_mode"),
                SettingsDefinitions.qualityGeneral().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void qualityTexturesHasItsFourSettingsInSpecOrder() {
        assertEquals(List.of("minecraft:quality.mipmap_levels", "vulkanmod:quality.texture_animations",
                        "vulkanmod:quality.connected_textures", "vulkanmod:quality.custom_item_textures"),
                SettingsDefinitions.qualityTextures().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void qualityLightingHasItsTwoSettingsInSpecOrder() {
        assertEquals(List.of("vulkanmod:quality.ambient_occlusion", "minecraft:quality.biome_blend"),
                SettingsDefinitions.qualityLighting().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void qualityEnvironmentHoldsOnlyTheCloudSetting() {
        assertEquals(List.of("minecraft:quality.clouds"),
                SettingsDefinitions.qualityEnvironment().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void qualityParticlesHoldsOnlyTheParticleLevel() {
        assertEquals(List.of("minecraft:quality.particles"),
                SettingsDefinitions.qualityParticles().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void qualityEntitiesHasItsTwoSettingsInSpecOrder() {
        assertEquals(List.of("minecraft:quality.entity_shadows", "minecraft:quality.entity_distance"),
                SettingsDefinitions.qualityEntities().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void qualityTypesSourcesAndScopesMatchTheSettingsMap() {
        List<SettingMeta> settings = qualitySettings();

        assertEquals(List.of(SettingType.ENUM,
                        SettingType.ENUM, SettingType.BOOL, SettingType.BOOL, SettingType.BOOL,
                        SettingType.ENUM, SettingType.INT,
                        SettingType.ENUM,
                        SettingType.ENUM,
                        SettingType.BOOL, SettingType.INT),
                settings.stream().map(SettingMeta::type).toList());
        assertEquals(List.of(SettingSource.MINECRAFT,
                        SettingSource.MINECRAFT, SettingSource.VOLCANIC, SettingSource.VOLCANIC,
                        SettingSource.VOLCANIC,
                        SettingSource.VOLCANIC, SettingSource.MINECRAFT,
                        SettingSource.MINECRAFT,
                        SettingSource.MINECRAFT,
                        SettingSource.MINECRAFT, SettingSource.MINECRAFT),
                settings.stream().map(SettingMeta::source).toList());
        assertEquals(List.of(ApplyScope.INSTANT,
                        ApplyScope.TEXTURE_RELOAD, ApplyScope.INSTANT, ApplyScope.CHUNK_REBUILD,
                        ApplyScope.INSTANT,
                        ApplyScope.CHUNK_REBUILD, ApplyScope.CHUNK_REBUILD,
                        ApplyScope.INSTANT,
                        ApplyScope.INSTANT,
                        ApplyScope.INSTANT, ApplyScope.INSTANT),
                settings.stream().map(SettingMeta::scope).toList());
    }

    @Test
    void connectedTexturesRebuildsChunksWhileCustomItemTexturesDoesNot() {
        SettingMeta ctm = SettingsDefinitions.qualityTextures().get(2);
        SettingMeta cit = SettingsDefinitions.qualityTextures().get(3);

        assertEquals(ApplyScope.CHUNK_REBUILD, ctm.scope());
        assertEquals(ApplyScope.INSTANT, cit.scope());
    }

    @Test
    void noQualitySettingIsFlaggedAdvancedOrExperimental() {
        assertTrue(qualitySettings().stream().noneMatch(SettingMeta::advanced));
        assertTrue(qualitySettings().stream().noneMatch(SettingMeta::experimental));
    }

    @Test
    void everyQualitySettingSitsOnTheRouteItWasDeclaredFor() {
        assertTrue(SettingsDefinitions.qualityGeneral().stream()
                .allMatch(meta -> RouteId.parse("quality.general").equals(meta.route())));
        assertTrue(SettingsDefinitions.qualityTextures().stream()
                .allMatch(meta -> RouteId.parse("quality.textures").equals(meta.route())));
        assertTrue(SettingsDefinitions.qualityLighting().stream()
                .allMatch(meta -> RouteId.parse("quality.lighting").equals(meta.route())));
        assertTrue(SettingsDefinitions.qualityEnvironment().stream()
                .allMatch(meta -> RouteId.parse("quality.environment").equals(meta.route())));
        assertTrue(SettingsDefinitions.qualityParticles().stream()
                .allMatch(meta -> RouteId.parse("quality.particles").equals(meta.route())));
        assertTrue(SettingsDefinitions.qualityEntities().stream()
                .allMatch(meta -> RouteId.parse("quality.entities").equals(meta.route())));
    }

    @Test
    void everyVolcanicQualityTitleKeyResolvesInTheLanguageFile() throws IOException {
        List<String> lang = Files.readAllLines(
                Path.of("src/main/resources/assets/vulkanmod/lang/en_us.json"));
        for (SettingMeta meta : qualitySettings()) {
            if (!meta.titleKey().startsWith("vulkanmod.")) {
                continue;
            }
            String entry = "\"" + meta.titleKey() + "\":";
            assertTrue(lang.stream().anyMatch(line -> line.trim().startsWith(entry)),
                    "missing language key " + meta.titleKey());
        }
    }

    @Test
    void noQualityTitleKeyIsSharedWithAnotherQualitySetting() {
        List<String> keys = qualitySettings().stream().map(SettingMeta::titleKey).toList();
        assertEquals(keys.size(), keys.stream().distinct().count());
    }

    @Test
    void theBiomeBlendGridMatchesTheSettingsMap() {
        assertEquals(0, SettingsDefinitions.BIOME_BLEND_MIN);
        assertEquals(7, SettingsDefinitions.BIOME_BLEND_MAX);
        assertEquals(1, SettingsDefinitions.BIOME_BLEND_STEP);
        assertEquals(2, SettingsDefinitions.BIOME_BLEND_DEFAULT);
    }

    @Test
    void theEntityDistanceGridMatchesTheSettingsMap() {
        assertEquals(50, SettingsDefinitions.ENTITY_DISTANCE_MIN);
        assertEquals(500, SettingsDefinitions.ENTITY_DISTANCE_MAX);
        assertEquals(25, SettingsDefinitions.ENTITY_DISTANCE_STEP);
        assertEquals(100, SettingsDefinitions.ENTITY_DISTANCE_DEFAULT);
    }

    @Test
    void everyEntityDistanceStepLandsOnTheQuarterGridVanillaValidates() {
        for (int percent = SettingsDefinitions.ENTITY_DISTANCE_MIN;
             percent <= SettingsDefinitions.ENTITY_DISTANCE_MAX;
             percent += SettingsDefinitions.ENTITY_DISTANCE_STEP) {
            assertEquals(0, (percent * 4) % 100,
                    "entity distance " + percent + "% is not a quarter step");
        }
    }

    private static List<SettingMeta> qualitySettings() {
        List<SettingMeta> all = new ArrayList<>(SettingsDefinitions.qualityGeneral());
        all.addAll(SettingsDefinitions.qualityTextures());
        all.addAll(SettingsDefinitions.qualityLighting());
        all.addAll(SettingsDefinitions.qualityEnvironment());
        all.addAll(SettingsDefinitions.qualityParticles());
        all.addAll(SettingsDefinitions.qualityEntities());
        return all;
    }

    @Test
    void advancedRendererHoldsOnlyTheGraphicsDevice() {
        assertEquals(List.of("vulkanmod:advanced.gpu_device"),
                SettingsDefinitions.advancedRenderer().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void advancedSynchronizationHoldsOnlyTheMoltenVkProfile() {
        assertEquals(List.of("vulkanmod:advanced.moltenvk_aggressive"),
                SettingsDefinitions.advancedSynchronization().stream()
                        .map(meta -> meta.id().toString()).toList());
    }

    @Test
    void advancedCompatibilityHasItsFourSettingsInSpecOrder() {
        assertEquals(List.of("vulkanmod:advanced.external_lod", "vulkanmod:advanced.external_lod_draw",
                        "vulkanmod:advanced.gl_legacy_bridge", "vulkanmod:advanced.gl_fbo_viewport"),
                SettingsDefinitions.advancedCompatibility().stream()
                        .map(meta -> meta.id().toString()).toList());
    }

    @Test
    void advancedTypesAndSourcesMatchTheSettingsMap() {
        List<SettingMeta> settings = advancedSettings();

        assertEquals(List.of(SettingType.ENUM, SettingType.BOOL,
                        SettingType.ENUM, SettingType.BOOL, SettingType.BOOL, SettingType.BOOL),
                settings.stream().map(SettingMeta::type).toList());
        assertEquals(List.of(SettingSource.VOLCANIC, SettingSource.VOLCANIC, SettingSource.VOLCANIC,
                        SettingSource.VOLCANIC, SettingSource.VOLCANIC, SettingSource.VOLCANIC),
                settings.stream().map(SettingMeta::source).toList());
    }

    @Test
    void everyAdvancedSettingIsReadOnceAtStartupSoItAppliesOnlyAfterRestart() {
        assertEquals(List.of(ApplyScope.RESTART, ApplyScope.RESTART, ApplyScope.RESTART,
                        ApplyScope.RESTART, ApplyScope.RESTART, ApplyScope.RESTART),
                advancedSettings().stream().map(SettingMeta::scope).toList());
    }

    @Test
    void everyAdvancedSettingExceptTheDeviceSelectorCarriesTheAdvancedFlag() {
        assertEquals(List.of(false, true, true, true, true, true),
                advancedSettings().stream().map(SettingMeta::advanced).toList());
    }

    @Test
    void theMoltenVkProfileIsTheOnlyExperimentalSettingInTheMenu() {
        assertEquals(List.of("vulkanmod:advanced.moltenvk_aggressive"),
                allSettings().stream().filter(SettingMeta::experimental)
                        .map(meta -> meta.id().toString()).toList());
    }

    @Test
    void occlusionCullingIsTheOnlyRecommendedSettingInTheMenu() {
        assertEquals(List.of("vulkanmod:culling.occlusion"),
                allSettings().stream().filter(SettingMeta::recommended)
                        .map(meta -> meta.id().toString()).toList());
    }

    @Test
    void noSettingIsBothExperimentalAndRecommended() {
        assertTrue(allSettings().stream().noneMatch(meta -> meta.experimental() && meta.recommended()));
    }

    @Test
    void theTenAdvancedSettingsDoNotAllBecomeBadgedByBeingAdvanced() {
        List<SettingMeta> advanced = allSettings().stream().filter(SettingMeta::advanced).toList();
        assertEquals(10, advanced.size());
        assertTrue(advanced.stream().noneMatch(SettingMeta::recommended));
        assertEquals(1, advanced.stream().filter(SettingMeta::experimental).count());
    }

    @Test
    void everyAdvancedSettingSitsOnTheRouteItWasDeclaredFor() {
        assertTrue(SettingsDefinitions.advancedRenderer().stream()
                .allMatch(meta -> RouteId.parse("advanced.renderer").equals(meta.route())));
        assertTrue(SettingsDefinitions.advancedSynchronization().stream()
                .allMatch(meta -> RouteId.parse("advanced.synchronization").equals(meta.route())));
        assertTrue(SettingsDefinitions.advancedCompatibility().stream()
                .allMatch(meta -> RouteId.parse("advanced.compatibility").equals(meta.route())));
    }

    @Test
    void noAdvancedTitleKeyIsSharedWithAnotherAdvancedSetting() {
        List<String> keys = advancedSettings().stream().map(SettingMeta::titleKey).toList();
        assertEquals(keys.size(), keys.stream().distinct().count());
    }

    @Test
    void everyVolcanicAdvancedTitleKeyResolvesInTheLanguageFile() throws IOException {
        List<String> lang = Files.readAllLines(
                Path.of("src/main/resources/assets/vulkanmod/lang/en_us.json"));
        for (SettingMeta meta : advancedSettings()) {
            if (!meta.titleKey().startsWith("vulkanmod.")) {
                continue;
            }
            String entry = "\"" + meta.titleKey() + "\":";
            assertTrue(lang.stream().anyMatch(line -> line.trim().startsWith(entry)),
                    "missing language key " + meta.titleKey());
        }
    }

    private static List<SettingMeta> advancedSettings() {
        List<SettingMeta> all = new ArrayList<>(SettingsDefinitions.advancedRenderer());
        all.addAll(SettingsDefinitions.advancedSynchronization());
        all.addAll(SettingsDefinitions.advancedCompatibility());
        return all;
    }
}
