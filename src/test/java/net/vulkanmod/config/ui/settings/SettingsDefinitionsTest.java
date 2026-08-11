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
    void theSettingsThatKeptATooltipAreTheOnesThatDescribeThemselves() {
        long described = allSettings().stream().filter(meta -> meta.descriptionKey() != null).count();
        assertTrue(described > allSettings().size() / 2,
                "most rows should still explain themselves");
        assertEquals(List.of(), allSettings().stream()
                        .filter(meta -> meta.advanced() || meta.experimental())
                        .filter(meta -> meta.descriptionKey() == null)
                        .map(meta -> meta.id().toString()).toList(),
                "a row nobody can guess the effect of must say what it does");
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
    void everyTitleKeyWeOwnResolvesInTheLanguageFile() throws IOException {
        List<String> lang = Files.readAllLines(LANG);
        List<String> missing = new ArrayList<>();
        for (SettingMeta meta : allSettings()) {
            if (meta.titleKey().startsWith("vulkanmod.") && !declares(lang, meta.titleKey())) {
                missing.add(meta.titleKey());
            }
        }
        assertEquals(List.of(), missing);
    }

    @Test
    void aSettingWithNothingToExplainNamesNoReason() {
        assertEquals(Optional.empty(), SettingsDefinitions.disabledReasonKey(SettingsDefinitions.VSYNC));
        assertEquals(Optional.empty(), SettingsDefinitions.disabledReasonKey(SettingsDefinitions.GUI_SCALE));
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

    @Test
    void everyDeclaredReasonConstantResolvesInTheLanguageFile() throws IOException {
        List<String> lang = Files.readAllLines(LANG);
        List<String> reasons = new ArrayList<>();
        for (java.lang.reflect.Field field : SettingsDefinitions.class.getDeclaredFields()) {
            if (field.getName().startsWith("REASON_") && field.getType() == String.class) {
                field.setAccessible(true);
                try {
                    reasons.add((String) field.get(null));
                } catch (IllegalAccessException unreachable) {
                    throw new AssertionError(field.getName() + " is not readable", unreachable);
                }
            }
        }
        assertFalse(reasons.isEmpty(), "the reasons are declared as REASON_ constants; find them");
        for (String key : reasons) {
            assertTrue(declares(lang, key), "missing language key " + key);
        }
    }

    private static boolean declares(List<String> lang, String key) {
        return lang.stream().anyMatch(line -> line.trim().startsWith("\"" + key + "\":"));
    }

    private static List<SettingMeta> allSettings() {
        List<SettingMeta> all = new ArrayList<>(SettingsDefinitions.displayGeneral());
        all.addAll(SettingsDefinitions.displayInterface());
        all.addAll(SettingsDefinitions.displayVolcanic());
        all.addAll(renderingSettings());
        all.addAll(performanceSettings());
        all.addAll(qualitySettings());
        all.addAll(advancedSettings());
        all.addAll(SettingsDefinitions.shadersCurrent());
        all.addAll(SettingsDefinitions.developerTools());
        return all;
    }

    @Test
    void displayGeneralHasItsSixSettingsInSpecOrder() {
        assertEquals(List.of("vulkanmod:display.window_mode", "vulkanmod:display.resolution",
                        "vulkanmod:display.refresh_rate", "minecraft:display.vsync",
                        "minecraft:display.framerate_limit", "vulkanmod:display.disable_hidpi"),
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
                SettingType.ENUM, SettingType.INT, SettingType.BOOL),
                settings.stream().map(SettingMeta::type).toList());
        assertEquals(List.of(SettingSource.VOLCANIC, SettingSource.VOLCANIC, SettingSource.VOLCANIC,
                SettingSource.MINECRAFT, SettingSource.MINECRAFT, SettingSource.VOLCANIC),
                settings.stream().map(SettingMeta::source).toList());
        assertEquals(List.of(ApplyScope.WINDOW, ApplyScope.WINDOW, ApplyScope.WINDOW,
                ApplyScope.INSTANT, ApplyScope.INSTANT, ApplyScope.RESTART),
                settings.stream().map(SettingMeta::scope).toList());
    }

    @Test
    void everySettingCarriesATitleKey() {
        assertTrue(SettingsDefinitions.displayGeneral().stream()
                .allMatch(meta -> meta.titleKey() != null && !meta.titleKey().isBlank()));
    }

    @Test
    void displayInterfaceHasItsSettingsInSpecOrder() {
        assertEquals(List.of("minecraft:display.gui_scale", "minecraft:display.brightness",
                        "minecraft:display.distortion_effects", "minecraft:display.fov_effects",
                        "minecraft:display.view_bobbing", "minecraft:display.damage_tilt"),
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

        assertEquals(List.of(SettingType.INT, SettingType.INT, SettingType.INT, SettingType.INT,
                        SettingType.BOOL, SettingType.INT),
                settings.stream().map(SettingMeta::type).toList());
        assertTrue(settings.stream().allMatch(meta -> meta.source() == SettingSource.MINECRAFT));
        assertTrue(settings.stream().allMatch(meta -> meta.scope() == ApplyScope.INSTANT));
    }

    @Test
    void noDisplayInterfaceSettingIsFlaggedAdvanced() {
        assertTrue(SettingsDefinitions.displayInterface().stream().noneMatch(SettingMeta::advanced));
    }

    @Test
    void theHiDpiToggleSitsWithTheOtherPixelGridSettings() {
        assertEquals("vulkanmod:display.disable_hidpi",
                SettingsDefinitions.displayGeneral().get(SettingsDefinitions.displayGeneral().size() - 1)
                        .id().toString());
    }

    @Test
    void theHiDpiToggleIsAnAdvancedRestartSetting() {
        SettingMeta meta = SettingsDefinitions.displayGeneral()
                .get(SettingsDefinitions.displayGeneral().size() - 1);

        assertEquals(RouteId.parse("display.general"), meta.route());
        assertEquals(SettingType.BOOL, meta.type());
        assertEquals(SettingSource.VOLCANIC, meta.source());
        assertEquals(ApplyScope.RESTART, meta.scope());
        assertTrue(meta.advanced());
        assertEquals("vulkanmod.options.disableHiDPI", meta.titleKey());
    }

    @Test
    void renderingGeneralHasItsFiveSettingsInSpecOrder() {
        assertEquals(List.of("minecraft:rendering.render_distance", "minecraft:rendering.simulation_distance",
                        "vulkanmod:rendering.block_entity_distance",
                        "minecraft:rendering.chunk_update_priority", "vulkanmod:rendering.chunk_fade_in"),
                SettingsDefinitions.renderingGeneral().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void renderingGeneralTypesSourcesAndScopesMatchTheSettingsMap() {
        List<SettingMeta> settings = SettingsDefinitions.renderingGeneral();

        assertEquals(List.of(SettingType.INT, SettingType.INT, SettingType.INT,
                        SettingType.ENUM, SettingType.BOOL),
                settings.stream().map(SettingMeta::type).toList());
        assertEquals(List.of(SettingSource.MINECRAFT, SettingSource.MINECRAFT,
                        SettingSource.VOLCANIC, SettingSource.MINECRAFT, SettingSource.VOLCANIC),
                settings.stream().map(SettingMeta::source).toList());
        assertEquals(List.of(ApplyScope.INSTANT, ApplyScope.INSTANT, ApplyScope.INSTANT,
                        ApplyScope.INSTANT, ApplyScope.INSTANT),
                settings.stream().map(SettingMeta::scope).toList());
    }

    @Test
    void resolutionScalingHasItsFourSettingsInSpecOrder() {
        assertEquals(List.of("vulkanmod:vsr.preset", "vulkanmod:vsr.upscaler",
                        "vulkanmod:vsr.render_scale", "vulkanmod:vsr.sharpness"),
                SettingsDefinitions.performanceResolution().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void resolutionScalingTypesSourcesAndScopesMatchTheSettingsMap() {
        List<SettingMeta> settings = SettingsDefinitions.performanceResolution();

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
        assertTrue(SettingsDefinitions.renderingCulling().stream()
                .allMatch(meta -> RouteId.parse("rendering.culling").equals(meta.route())));
    }

    @Test
    void noRenderingSettingIsFlaggedExperimental() {
        assertTrue(renderingSettings().stream().noneMatch(SettingMeta::experimental));
    }

    private static List<SettingMeta> renderingSettings() {
        List<SettingMeta> all = new ArrayList<>(SettingsDefinitions.renderingGeneral());
        all.addAll(SettingsDefinitions.renderingCulling());
        return all;
    }

    @Test
    void theBuilderThreadsSitWithTheUploadBudgetTheyQueueBehind() {
        assertEquals(List.of("vulkanmod:performance.adaptive_chunk_uploads",
                        "vulkanmod:performance.chunk_uploads_per_frame",
                        "vulkanmod:performance.builder_threads"),
                SettingsDefinitions.performanceChunks().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void theRenderQueueSitsWithTheOtherGpuSideSettings() {
        assertEquals(List.of("vulkanmod:performance.indirect_draw",
                        "vulkanmod:performance.unique_opaque_layer",
                        "vulkanmod:performance.frame_queue"),
                SettingsDefinitions.performanceGpu().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void performanceTypesSourcesAndScopesMatchTheSettingsMap() {
        List<SettingMeta> settings = performanceSettings();

        assertEquals(List.of(SettingType.BOOL, SettingType.BOOL, SettingType.INT,
                        SettingType.BOOL, SettingType.INT, SettingType.INT,
                        SettingType.ENUM, SettingType.ENUM, SettingType.INT, SettingType.INT),
                settings.stream().map(SettingMeta::type).toList());
        assertTrue(settings.stream().allMatch(meta -> meta.source() == SettingSource.VOLCANIC));
        assertEquals(List.of(ApplyScope.CHUNK_REBUILD, ApplyScope.CHUNK_REBUILD, ApplyScope.SWAPCHAIN,
                        ApplyScope.INSTANT, ApplyScope.INSTANT, ApplyScope.RESTART,
                        ApplyScope.INSTANT, ApplyScope.INSTANT, ApplyScope.INSTANT, ApplyScope.INSTANT),
                settings.stream().map(SettingMeta::scope).toList());
    }

    @Test
    void onlyTheTwoTunablesAreFlaggedAdvancedOnThePerformancePages() {
        assertEquals(List.of(false, false, true, false, true, false, false, false, false, false),
                performanceSettings().stream().map(SettingMeta::advanced).toList());
    }

    @Test
    void noPerformanceSettingIsFlaggedExperimental() {
        assertTrue(performanceSettings().stream().noneMatch(SettingMeta::experimental));
    }

    @Test
    void everyPerformanceSettingSitsOnTheRouteItWasDeclaredFor() {
        assertTrue(SettingsDefinitions.performanceGpu().stream()
                .allMatch(meta -> RouteId.parse("performance.gpu").equals(meta.route())));
        assertTrue(SettingsDefinitions.performanceChunks().stream()
                .allMatch(meta -> RouteId.parse("performance.chunks").equals(meta.route())));
        assertTrue(SettingsDefinitions.performanceResolution().stream()
                .allMatch(meta -> RouteId.parse("performance.resolution").equals(meta.route())));
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
        List<SettingMeta> all = new ArrayList<>(SettingsDefinitions.performanceGpu());
        all.addAll(SettingsDefinitions.performanceChunks());
        all.addAll(SettingsDefinitions.performanceResolution());
        return all;
    }

    @Test
    void graphicsModeLeadsTheEnvironmentPageBecauseThatIsMostlyWhatItChanges() {
        assertEquals("minecraft:quality.graphics_mode",
                SettingsDefinitions.qualityEnvironment().get(0).id().toString());
        assertEquals(RouteId.parse("quality.environment"),
                SettingsDefinitions.qualityEnvironment().get(0).route());
    }

    @Test
    void qualityTexturesHasItsSettingsInSpecOrder() {
        assertEquals(List.of("minecraft:quality.mipmap_levels", "vulkanmod:quality.texture_animations",
                        "vulkanmod:quality.connected_textures", "vulkanmod:quality.custom_item_textures",
                        "vulkanmod:quality.core_shader_packs", "minecraft:quality.glint_strength",
                        "vulkanmod:experimental.anisotropic_filtering"),
                SettingsDefinitions.qualityTextures().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void qualityLightingHasItsSettingsInSpecOrder() {
        assertEquals(List.of("vulkanmod:quality.ambient_occlusion", "minecraft:quality.biome_blend",
                        "vulkanmod:quality.dynamic_light"),
                SettingsDefinitions.qualityLighting().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void qualityEnvironmentLeadsWithGraphicsModeThenCloudsWeatherWindAndHorizonFog() {
        assertEquals(List.of("minecraft:quality.graphics_mode",
                        "minecraft:quality.clouds", "vulkanmod:quality.weather_rendering",
                        "vulkanmod:environment.wind_strength", "vulkanmod:quality.horizon_fog",
                        "vulkanmod:quality.leaves"),
                SettingsDefinitions.qualityEnvironment().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void renderingAdvancedHoldsOnlyTheParticleLevel() {
        assertEquals(List.of("minecraft:quality.particles"),
                SettingsDefinitions.renderingAdvanced().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void renderingEntitiesHoldsOnlyTheVanillaEntityRows() {
        assertEquals(List.of("minecraft:quality.entity_shadows", "minecraft:quality.entity_distance"),
                SettingsDefinitions.renderingEntities().stream().map(meta -> meta.id().toString()).toList());
    }

    @Test
    void qualityTypesSourcesAndScopesMatchTheSettingsMap() {
        List<SettingMeta> settings = qualitySettings();

        assertEquals(SettingsDefinitions.qualityTextures().size()
                        + SettingsDefinitions.qualityLighting().size()
                        + SettingsDefinitions.qualityEnvironment().size()
                        + SettingsDefinitions.renderingAdvanced().size()
                        + SettingsDefinitions.renderingEntities().size(),
                settings.size());
        assertEquals(settings.size(), settings.stream().map(SettingMeta::type).filter(t -> t != null).count());
        assertEquals(settings.size(), settings.stream().map(SettingMeta::source).filter(t -> t != null).count());
        assertEquals(settings.size(), settings.stream().map(SettingMeta::scope).filter(t -> t != null).count());
        assertEquals(SettingType.ENUM, SettingsDefinitions.qualityTextures().get(0).type());
        assertEquals(ApplyScope.TEXTURE_RELOAD,
                SettingsDefinitions.qualityTextures().get(SettingsDefinitions.qualityTextures().size() - 1).scope());
        assertEquals(SettingType.ENUM, SettingsDefinitions.qualityEnvironment().get(0).type());
        assertEquals(ApplyScope.INSTANT, SettingsDefinitions.qualityEnvironment().get(0).scope());
    }

    @Test
    void connectedTexturesRebuildsChunksWhileCustomItemTexturesDoesNot() {
        SettingMeta ctm = SettingsDefinitions.qualityTextures().get(2);
        SettingMeta cit = SettingsDefinitions.qualityTextures().get(3);

        assertEquals(ApplyScope.CHUNK_REBUILD, ctm.scope());
        assertEquals(ApplyScope.INSTANT, cit.scope());
    }

    @Test
    void noQualitySettingIsFlaggedAdvancedAndOnlyTheSamplerIsExperimental() {
        assertTrue(qualitySettings().stream().noneMatch(SettingMeta::advanced));
        assertEquals(List.of("vulkanmod:experimental.anisotropic_filtering"),
                qualitySettings().stream().filter(SettingMeta::experimental)
                        .map(meta -> meta.id().toString()).toList());
    }

    @Test
    void everyQualitySettingSitsOnTheRouteItWasDeclaredFor() {
        assertTrue(SettingsDefinitions.qualityTextures().stream()
                .allMatch(meta -> RouteId.parse("quality.textures").equals(meta.route())));
        assertTrue(SettingsDefinitions.qualityLighting().stream()
                .allMatch(meta -> RouteId.parse("quality.lighting").equals(meta.route())));
        assertTrue(SettingsDefinitions.qualityEnvironment().stream()
                .allMatch(meta -> RouteId.parse("quality.environment").equals(meta.route())));
        assertTrue(SettingsDefinitions.renderingAdvanced().stream()
                .allMatch(meta -> RouteId.parse("rendering.advanced").equals(meta.route())));
        assertTrue(SettingsDefinitions.renderingEntities().stream()
                .allMatch(meta -> RouteId.parse("rendering.entities").equals(meta.route())));
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
        List<SettingMeta> all = new ArrayList<>(SettingsDefinitions.qualityTextures());
        all.addAll(SettingsDefinitions.qualityLighting());
        all.addAll(SettingsDefinitions.qualityEnvironment());
        all.addAll(SettingsDefinitions.renderingAdvanced());
        all.addAll(SettingsDefinitions.renderingEntities());
        return all;
    }

    @Test
    void theMoltenVkProfileJoinsTheOtherStartupOnlyVulkanSwitches() {
        assertEquals(List.of("vulkanmod:advanced.vulkan_validation", "vulkanmod:advanced.gpu_device",
                        "vulkanmod:advanced.moltenvk_aggressive"),
                SettingsDefinitions.advancedRenderer().stream().map(meta -> meta.id().toString()).toList());
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

        assertEquals(List.of(SettingType.BOOL, SettingType.ENUM, SettingType.BOOL,
                        SettingType.ENUM, SettingType.BOOL, SettingType.BOOL, SettingType.BOOL),
                settings.stream().map(SettingMeta::type).toList());
        assertTrue(settings.stream().allMatch(meta -> meta.source() == SettingSource.VOLCANIC));
    }

    @Test
    void everyAdvancedSettingIsReadOnceAtStartupSoItAppliesOnlyAfterRestart() {
        assertTrue(advancedSettings().stream().allMatch(meta -> meta.scope() == ApplyScope.RESTART),
                "everything on the advanced pages is read once at startup");
    }

    @Test
    void everyAdvancedSettingExceptTheDeviceSelectorCarriesTheAdvancedFlag() {
        assertEquals(List.of(false, false, true, true, true, true, true),
                advancedSettings().stream().map(SettingMeta::advanced).toList());
    }

    @Test
    void theExperimentalBadgeIsReservedForTheProfileAndTheSamplerSetting() {
        assertEquals(List.of("vulkanmod:experimental.anisotropic_filtering",
                        "vulkanmod:advanced.moltenvk_aggressive"),
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
        assertTrue(SettingsDefinitions.advancedCompatibility().stream()
                .allMatch(meta -> RouteId.parse("advanced.compatibility").equals(meta.route())));
    }

    @Test
    void noAdvancedTitleKeyIsSharedWithAnotherAdvancedSetting() {
        List<String> keys = advancedSettings().stream().map(SettingMeta::titleKey).toList();
        assertEquals(keys.size(), keys.stream().distinct().count());
    }

    private static List<SettingMeta> advancedSettings() {
        List<SettingMeta> all = new ArrayList<>(SettingsDefinitions.advancedRenderer());
        all.addAll(SettingsDefinitions.advancedCompatibility());
        return all;
    }
}
