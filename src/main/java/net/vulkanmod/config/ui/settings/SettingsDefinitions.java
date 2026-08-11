package net.vulkanmod.config.ui.settings;

import net.vulkanmod.config.ui.core.ApplyScope;
import net.vulkanmod.config.ui.core.ImpactLevel;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingSource;
import net.vulkanmod.config.ui.core.SettingType;

import java.util.List;
import java.util.Optional;

public final class SettingsDefinitions {
    public static final String REASON_EXCLUSIVE_FULLSCREEN = "vulkanmod.ui.disabled.exclusive_fullscreen";
    public static final String REASON_CORE_SHADER_PACK = "vulkanmod.ui.disabled.core_shader_pack";
    public static final String REASON_LAUNCH_FLAG = "vulkanmod.ui.disabled.launch_flag";
    public static final String REASON_MACOS_ONLY = "vulkanmod.ui.disabled.macos_only";
    public static final String REASON_VULKAN_SDK = "vulkanmod.ui.disabled.vulkan_sdk";
    public static final String REASON_ANISOTROPY = "vulkanmod.ui.disabled.anisotropy";

    public static final RouteId SHADERS_CURRENT = RouteId.parse("shaders.current");
    public static final SettingId SHADERS_ENABLED = SettingId.parse("vulkanmod:shaders.enabled");
    public static final SettingId SHADERS_SELECTED_PACK = SettingId.parse("vulkanmod:shaders.selected_pack");

    public static final RouteId DISPLAY_GENERAL = RouteId.parse("display.general");
    public static final RouteId DISPLAY_INTERFACE = RouteId.parse("display.interface");
    public static final RouteId DISPLAY_VOLCANIC = RouteId.parse("display.volcanic");
    public static final RouteId RENDERING_GENERAL = RouteId.parse("rendering.general");
    public static final RouteId PERFORMANCE_RESOLUTION = RouteId.parse("performance.resolution");
    public static final RouteId RENDERING_CULLING = RouteId.parse("rendering.culling");
    public static final RouteId RENDERING_ENTITIES = RouteId.parse("rendering.entities");
    public static final RouteId RENDERING_ADVANCED = RouteId.parse("rendering.advanced");
    public static final RouteId PERFORMANCE_GPU = RouteId.parse("performance.gpu");
    public static final RouteId PERFORMANCE_CHUNKS = RouteId.parse("performance.chunks");
    public static final RouteId QUALITY_TEXTURES = RouteId.parse("quality.textures");
    public static final RouteId QUALITY_LIGHTING = RouteId.parse("quality.lighting");
    public static final RouteId QUALITY_ENVIRONMENT = RouteId.parse("quality.environment");
    public static final RouteId DEVELOPER_TOOLS = RouteId.parse("developer.tools");
    public static final RouteId ADVANCED_RENDERER = RouteId.parse("advanced.renderer");
    public static final RouteId ADVANCED_COMPATIBILITY = RouteId.parse("advanced.compatibility");

    public static final SettingId BUILDER_THREADS = SettingId.parse("vulkanmod:performance.builder_threads");
    public static final SettingId VIEW_BOBBING = SettingId.parse("minecraft:display.view_bobbing");
    public static final SettingId DAMAGE_TILT = SettingId.parse("minecraft:display.damage_tilt");
    public static final SettingId GLINT_STRENGTH = SettingId.parse("minecraft:quality.glint_strength");
    public static final SettingId WIND_STRENGTH = SettingId.parse("vulkanmod:environment.wind_strength");
    public static final SettingId DEBUG_MENU = SettingId.parse("vulkanmod:developer.debug_menu");
    public static final SettingId DEBUG_MENU_KEY = SettingId.parse("vulkanmod:developer.debug_menu_key");
    public static final SettingId SHOW_FPS = SettingId.parse("vulkanmod:developer.show_fps");
    public static final SettingId DEBUG_OVERLAY = SettingId.parse("vulkanmod:developer.debug_overlay");
    public static final SettingId SHOW_COORDINATES = SettingId.parse("vulkanmod:developer.show_coordinates");
    public static final SettingId PERF_LOG = SettingId.parse("vulkanmod:developer.perf_log");
    public static final SettingId STATS_SAMPLE_IN_MENUS = SettingId.parse("vulkanmod:developer.stats_sample_in_menus");
    public static final SettingId VULKAN_VALIDATION = SettingId.parse("vulkanmod:advanced.vulkan_validation");
    public static final SettingId VSR_DEBUG = SettingId.parse("vulkanmod:developer.vsr_debug");

    public static final SettingId WINDOW_MODE = SettingId.parse("vulkanmod:display.window_mode");
    public static final SettingId RESOLUTION = SettingId.parse("vulkanmod:display.resolution");
    public static final SettingId REFRESH_RATE = SettingId.parse("vulkanmod:display.refresh_rate");
    public static final SettingId VSYNC = SettingId.parse("minecraft:display.vsync");
    public static final String VSYNC_OFF = "vulkanmod.options.vsync.off";
    public static final String VSYNC_ON = "vulkanmod.options.vsync.on";
    public static final String VSYNC_ADAPTIVE = "vulkanmod.options.vsync.adaptive";
    public static final SettingId FRAMERATE_LIMIT = SettingId.parse("minecraft:display.framerate_limit");

    public static final SettingId GUI_SCALE = SettingId.parse("minecraft:display.gui_scale");
    public static final SettingId BRIGHTNESS = SettingId.parse("minecraft:display.brightness");
    public static final SettingId DISTORTION_EFFECTS = SettingId.parse("minecraft:display.distortion_effects");
    public static final SettingId FOV_EFFECTS = SettingId.parse("minecraft:display.fov_effects");
    public static final SettingId DISABLE_HIDPI = SettingId.parse("vulkanmod:display.disable_hidpi");
    public static final SettingId UI_ANIMATIONS = SettingId.parse("vulkanmod:display.ui_animations");
    public static final SettingId BACKGROUND_ANIMATION = SettingId.parse("vulkanmod:display.background_animation");
    public static final SettingId UI_SOUNDS = SettingId.parse("vulkanmod:display.ui_sounds");
    public static final SettingId LOADING_SCREEN = SettingId.parse("vulkanmod:display.loading_screen");

    public static final SettingId RENDER_DISTANCE = SettingId.parse("minecraft:rendering.render_distance");
    public static final SettingId SIMULATION_DISTANCE = SettingId.parse("minecraft:rendering.simulation_distance");
    public static final SettingId CHUNK_UPDATE_PRIORITY = SettingId.parse("minecraft:rendering.chunk_update_priority");
    public static final SettingId CHUNK_FADE_IN = SettingId.parse("vulkanmod:rendering.chunk_fade_in");

    public static final SettingId VSR_PRESET = SettingId.parse("vulkanmod:vsr.preset");
    public static final SettingId VSR_UPSCALER = SettingId.parse("vulkanmod:vsr.upscaler");
    public static final SettingId VSR_RENDER_SCALE = SettingId.parse("vulkanmod:vsr.render_scale");
    public static final SettingId VSR_SHARPNESS = SettingId.parse("vulkanmod:vsr.sharpness");

    public static final SettingId CULLING_OCCLUSION = SettingId.parse("vulkanmod:culling.occlusion");
    public static final SettingId CULLING_MODE = SettingId.parse("vulkanmod:culling.mode");
    public static final SettingId CULLING_BLOCK_ENTITIES = SettingId.parse("vulkanmod:culling.block_entities");
    public static final SettingId CULLING_PARTICLES = SettingId.parse("vulkanmod:culling.particles");
    public static final SettingId CULLING_LOD_GPU = SettingId.parse("vulkanmod:culling.lod_gpu");

    public static final SettingId PERFORMANCE_PROFILE = SettingId.parse("vulkanmod:performance.profile");
    public static final SettingId INDIRECT_DRAW = SettingId.parse("vulkanmod:performance.indirect_draw");
    public static final SettingId UNIQUE_OPAQUE_LAYER = SettingId.parse("vulkanmod:performance.unique_opaque_layer");
    public static final SettingId ADAPTIVE_CHUNK_UPLOADS = SettingId.parse("vulkanmod:performance.adaptive_chunk_uploads");
    public static final SettingId CHUNK_UPLOADS_PER_FRAME = SettingId.parse("vulkanmod:performance.chunk_uploads_per_frame");
    public static final SettingId FRAME_QUEUE = SettingId.parse("vulkanmod:performance.frame_queue");

    public static final SettingId GRAPHICS_MODE = SettingId.parse("minecraft:quality.graphics_mode");
    public static final SettingId MIPMAP_LEVELS = SettingId.parse("minecraft:quality.mipmap_levels");
    public static final SettingId TEXTURE_ANIMATIONS = SettingId.parse("vulkanmod:quality.texture_animations");
    public static final SettingId CONNECTED_TEXTURES = SettingId.parse("vulkanmod:quality.connected_textures");
    public static final SettingId CUSTOM_ITEM_TEXTURES = SettingId.parse("vulkanmod:quality.custom_item_textures");
    public static final SettingId AMBIENT_OCCLUSION = SettingId.parse("vulkanmod:quality.ambient_occlusion");
    public static final SettingId BIOME_BLEND = SettingId.parse("minecraft:quality.biome_blend");
    public static final SettingId CLOUDS = SettingId.parse("minecraft:quality.clouds");
    public static final SettingId WEATHER_RENDERING = SettingId.parse("vulkanmod:quality.weather_rendering");
    public static final SettingId LEAVES_QUALITY = SettingId.parse("vulkanmod:quality.leaves");
    public static final String LEAVES_FANCY = "vulkanmod.options.leavesQuality.fancy";
    public static final String LEAVES_FAST = "vulkanmod.options.leavesQuality.fast";
    public static final SettingId DYNAMIC_LIGHT = SettingId.parse("vulkanmod:quality.dynamic_light");
    public static final String REASON_NOT_YET = "vulkanmod.ui.disabled.not_yet";
    public static final SettingId HORIZON_FOG = SettingId.parse("vulkanmod:quality.horizon_fog");
    public static final SettingId PARTICLES = SettingId.parse("minecraft:quality.particles");
    public static final SettingId ENTITY_SHADOWS = SettingId.parse("minecraft:quality.entity_shadows");
    public static final SettingId ENTITY_DISTANCE = SettingId.parse("minecraft:quality.entity_distance");

    public static final SettingId GPU_DEVICE = SettingId.parse("vulkanmod:advanced.gpu_device");
    public static final SettingId MOLTENVK_AGGRESSIVE = SettingId.parse("vulkanmod:advanced.moltenvk_aggressive");
    public static final SettingId EXTERNAL_LOD = SettingId.parse("vulkanmod:advanced.external_lod");
    public static final SettingId EXTERNAL_LOD_DRAW = SettingId.parse("vulkanmod:advanced.external_lod_draw");
    public static final SettingId GL_LEGACY_BRIDGE = SettingId.parse("vulkanmod:advanced.gl_legacy_bridge");
    public static final SettingId GL_FBO_VIEWPORT = SettingId.parse("vulkanmod:advanced.gl_fbo_viewport");
    public static final SettingId CORE_SHADER_PACKS = SettingId.parse("vulkanmod:quality.core_shader_packs");
    public static final SettingId BLOCK_ENTITY_DISTANCE = SettingId.parse("vulkanmod:rendering.block_entity_distance");
    public static final SettingId ANISOTROPIC_FILTERING = SettingId.parse("vulkanmod:experimental.anisotropic_filtering");

    public static final int FRAMERATE_LIMIT_MIN = 10;
    public static final int FRAMERATE_LIMIT_MAX = 260;
    public static final int FRAMERATE_LIMIT_STEP = 10;
    public static final int FRAMERATE_LIMIT_DEFAULT = 120;

    public static final int GUI_SCALE_MIN = 0;
    public static final int GUI_SCALE_STEP = 1;
    public static final int GUI_SCALE_AUTO = 0;

    public static final int BRIGHTNESS_MIN = 0;
    public static final int BRIGHTNESS_MAX = 100;
    public static final int BRIGHTNESS_STEP = 1;
    public static final int BRIGHTNESS_DEFAULT = 50;

    public static final int EFFECT_SCALE_MIN = 0;
    public static final int EFFECT_SCALE_MAX = 100;
    public static final int EFFECT_SCALE_STEP = 1;
    public static final int EFFECT_SCALE_DEFAULT = 100;

    public static final int RENDER_DISTANCE_MIN = 2;
    public static final int RENDER_DISTANCE_MAX = 32;
    public static final int RENDER_DISTANCE_STEP = 1;
    public static final int RENDER_DISTANCE_DEFAULT = 8;

    public static final int SIMULATION_DISTANCE_MIN = 5;
    public static final int SIMULATION_DISTANCE_MAX = 32;
    public static final int SIMULATION_DISTANCE_STEP = 1;
    public static final int SIMULATION_DISTANCE_DEFAULT = 6;

    public static final int BLOCK_ENTITY_DISTANCE_MIN = 16;
    public static final int BLOCK_ENTITY_DISTANCE_MAX = 128;
    public static final int BLOCK_ENTITY_DISTANCE_STEP = 8;
    public static final int BLOCK_ENTITY_DISTANCE_DEFAULT = BLOCK_ENTITY_DISTANCE_MAX;

    public static final int HORIZON_FOG_STEP = 5;
    public static final int HORIZON_FOG_DEFAULT = 100;

    public static final int SHARPNESS_MIN = 0;
    public static final int SHARPNESS_MAX = 100;
    public static final int SHARPNESS_STEP = 5;
    public static final int SHARPNESS_DEFAULT = 0;

    public static final int CHUNK_UPLOADS_PER_FRAME_MIN = 1;
    public static final int CHUNK_UPLOADS_PER_FRAME_MAX = 16;
    public static final int CHUNK_UPLOADS_PER_FRAME_STEP = 1;

    public static final int FRAME_QUEUE_MIN = 2;
    public static final int FRAME_QUEUE_MAX = 5;
    public static final int FRAME_QUEUE_STEP = 1;
    public static final int FRAME_QUEUE_DEFAULT = 2;

    public static final int BIOME_BLEND_MIN = 0;
    public static final int BIOME_BLEND_MAX = 7;
    public static final int BIOME_BLEND_STEP = 1;
    public static final int BIOME_BLEND_DEFAULT = 2;

    public static final int ENTITY_DISTANCE_MIN = 50;
    public static final int ENTITY_DISTANCE_MAX = 500;
    public static final int ENTITY_DISTANCE_STEP = 25;
    public static final int ENTITY_DISTANCE_DEFAULT = 100;

    public static final int PERCENT_SCALE = 100;
    public static final int BUILDER_THREADS_AUTO = 0;
    public static final int GLINT_DEFAULT = 75;
    public static final int WIND_MAX = 200;
    public static final int WIND_STEP = 10;
    public static final int WIND_DEFAULT = 100;

    private SettingsDefinitions() {
    }

    public static Optional<String> disabledReasonKey(SettingId id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (RESOLUTION.equals(id) || REFRESH_RATE.equals(id)) {
            return Optional.of(REASON_EXCLUSIVE_FULLSCREEN);
        }
        if (SHADERS_SELECTED_PACK.equals(id)) {
            return Optional.of(REASON_CORE_SHADER_PACK);
        }
        if (VULKAN_VALIDATION.equals(id)) {
            return Optional.of(REASON_VULKAN_SDK);
        }
        if (ANISOTROPIC_FILTERING.equals(id)) {
            return Optional.of(REASON_ANISOTROPY);
        }
        return Optional.empty();
    }

    public static List<SettingMeta> displayGeneral() {
        return List.of(
                new SettingMeta.Builder(WINDOW_MODE, DISPLAY_GENERAL, "vulkanmod.options.windowMode",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .scope(ApplyScope.WINDOW).build(),
                new SettingMeta.Builder(RESOLUTION, DISPLAY_GENERAL, "options.fullscreen.resolution",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .scope(ApplyScope.WINDOW)
                        .performance(ImpactLevel.HIGH).visual(ImpactLevel.MEDIUM).build(),
                new SettingMeta.Builder(REFRESH_RATE, DISPLAY_GENERAL, "vulkanmod.options.refreshRate",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .scope(ApplyScope.WINDOW).build(),
                new SettingMeta.Builder(VSYNC, DISPLAY_GENERAL, "options.vsync",
                        SettingType.ENUM, SettingSource.MINECRAFT)
                        .descriptionKey("vulkanmod.options.vsync.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.NONE).build(),
                new SettingMeta.Builder(FRAMERATE_LIMIT, DISPLAY_GENERAL, "options.framerateLimit",
                        SettingType.INT, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.NONE).build(),
                new SettingMeta.Builder(DISABLE_HIDPI, DISPLAY_GENERAL, "vulkanmod.options.disableHiDPI",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.disableHiDPI.tooltip")
                        .scope(ApplyScope.RESTART).advanced(true)
                        .performance(ImpactLevel.HIGH).visual(ImpactLevel.MEDIUM).build()
);
    }

    public static List<SettingMeta> displayInterface() {
        return List.of(
                new SettingMeta.Builder(GUI_SCALE, DISPLAY_INTERFACE, "options.guiScale",
                        SettingType.INT, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT).build(),
                new SettingMeta.Builder(BRIGHTNESS, DISPLAY_INTERFACE, "options.gamma",
                        SettingType.INT, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT).build(),
                new SettingMeta.Builder(DISTORTION_EFFECTS, DISPLAY_INTERFACE, "options.screenEffectScale",
                        SettingType.INT, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT).build(),
                new SettingMeta.Builder(FOV_EFFECTS, DISPLAY_INTERFACE, "options.fovEffectScale",
                        SettingType.INT, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT).build(),
                new SettingMeta.Builder(VIEW_BOBBING, DISPLAY_INTERFACE, "options.viewBobbing",
                        SettingType.BOOL, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.NONE).visual(ImpactLevel.MEDIUM).build(),
                new SettingMeta.Builder(DAMAGE_TILT, DISPLAY_INTERFACE, "options.damageTiltStrength",
                        SettingType.INT, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.NONE).visual(ImpactLevel.MEDIUM).build());
    }

    public static List<SettingMeta> shadersCurrent() {
        return List.of(
                new SettingMeta.Builder(SHADERS_ENABLED, SHADERS_CURRENT, "vulkanmod.options.shadersEnabled",
                        SettingType.BOOL, SettingSource.SHADERS)
                        .descriptionKey("vulkanmod.options.shadersEnabled.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.HIGH).visual(ImpactLevel.HIGH).build(),
                new SettingMeta.Builder(SHADERS_SELECTED_PACK, SHADERS_CURRENT, "vulkanmod.options.selectedShader",
                        SettingType.ENUM, SettingSource.SHADERS)
                        .scope(ApplyScope.INSTANT).build());
    }

    public static List<SettingMeta> displayVolcanic() {
        return List.of(
                new SettingMeta.Builder(UI_ANIMATIONS, DISPLAY_VOLCANIC, "vulkanmod.options.uiAnimations",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.uiAnimations.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.LOW).visual(ImpactLevel.MEDIUM).build(),
                new SettingMeta.Builder(BACKGROUND_ANIMATION, DISPLAY_VOLCANIC,
                        "vulkanmod.options.backgroundAnimation", SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.backgroundAnimation.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.LOW).visual(ImpactLevel.MEDIUM).build(),
                new SettingMeta.Builder(UI_SOUNDS, DISPLAY_VOLCANIC, "vulkanmod.options.uiSounds",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.uiSounds.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.NONE).visual(ImpactLevel.NONE).build(),
                new SettingMeta.Builder(LOADING_SCREEN, DISPLAY_VOLCANIC,
                        "vulkanmod.options.loadingScreen", SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.loadingScreen.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.NONE).visual(ImpactLevel.MEDIUM).build());
    }

    public static List<SettingMeta> renderingGeneral() {
        return List.of(
                new SettingMeta.Builder(RENDER_DISTANCE, RENDERING_GENERAL, "options.renderDistance",
                        SettingType.INT, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.HIGH).visual(ImpactLevel.HIGH).build(),
                new SettingMeta.Builder(SIMULATION_DISTANCE, RENDERING_GENERAL, "options.simulationDistance",
                        SettingType.INT, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.LOW).build(),
                new SettingMeta.Builder(BLOCK_ENTITY_DISTANCE, RENDERING_GENERAL,
                        "vulkanmod.options.blockEntityDistance", SettingType.INT, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.blockEntityDistance.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.MEDIUM).build(),
                new SettingMeta.Builder(CHUNK_UPDATE_PRIORITY, RENDERING_GENERAL, "options.prioritizeChunkUpdates",
                        SettingType.ENUM, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.LOW).visual(ImpactLevel.LOW).build(),
                new SettingMeta.Builder(CHUNK_FADE_IN, RENDERING_GENERAL, "vulkanmod.options.chunkFadeIn",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.chunkFadeIn.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.NONE).visual(ImpactLevel.LOW).build());
    }

    public static List<SettingMeta> performanceResolution() {
        return List.of(
                new SettingMeta.Builder(VSR_PRESET, PERFORMANCE_RESOLUTION, "vulkanmod.options.vsrPreset",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.vsrPreset.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.HIGH).visual(ImpactLevel.HIGH).build(),
                new SettingMeta.Builder(VSR_UPSCALER, PERFORMANCE_RESOLUTION, "vulkanmod.options.vsrBackend",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.vsrBackend.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.MEDIUM).build(),
                new SettingMeta.Builder(VSR_RENDER_SCALE, PERFORMANCE_RESOLUTION, "vulkanmod.options.renderScale",
                        SettingType.INT, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.renderScale.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.HIGH).visual(ImpactLevel.HIGH).build(),
                new SettingMeta.Builder(VSR_SHARPNESS, PERFORMANCE_RESOLUTION, "vulkanmod.options.vsrSharpness",
                        SettingType.INT, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.vsrSharpness.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.NONE).visual(ImpactLevel.LOW).build());
    }

    public static List<SettingMeta> renderingCulling() {
        return List.of(
                new SettingMeta.Builder(CULLING_OCCLUSION, RENDERING_CULLING, "vulkanmod.options.occlusionCulling",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.occlusionCulling.tooltip")
                        .scope(ApplyScope.CHUNK_REBUILD).recommended(true)
                        .performance(ImpactLevel.HIGH).visual(ImpactLevel.NONE).build(),
                new SettingMeta.Builder(CULLING_MODE, RENDERING_CULLING, "vulkanmod.options.advCulling",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.advCulling.tooltip")
                        .scope(ApplyScope.CHUNK_REBUILD).advanced(true)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.MEDIUM).build(),
                new SettingMeta.Builder(CULLING_BLOCK_ENTITIES, RENDERING_CULLING, "vulkanmod.options.blockEntityCulling",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.blockEntityCulling.tooltip")
                        .scope(ApplyScope.CHUNK_REBUILD)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.LOW).build(),
                new SettingMeta.Builder(CULLING_PARTICLES, RENDERING_CULLING, "vulkanmod.options.particleCulling",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.particleCulling.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.LOW).visual(ImpactLevel.LOW).build(),
                new SettingMeta.Builder(CULLING_LOD_GPU, RENDERING_CULLING, "vulkanmod.options.lodGpuCulling",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.lodGpuCulling.tooltip")
                        .scope(ApplyScope.CHUNK_REBUILD).advanced(true)
                        .performance(ImpactLevel.HIGH).visual(ImpactLevel.NONE).build());
    }

    public static List<SettingMeta> performanceGpu() {
        return List.of(
                new SettingMeta.Builder(INDIRECT_DRAW, PERFORMANCE_GPU, "vulkanmod.options.indirectDraw",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.indirectDraw.tooltip")
                        .scope(ApplyScope.CHUNK_REBUILD)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.NONE).build(),
                new SettingMeta.Builder(UNIQUE_OPAQUE_LAYER, PERFORMANCE_GPU, "vulkanmod.options.uniqueOpaqueLayer",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.uniqueOpaqueLayer.tooltip")
                        .scope(ApplyScope.CHUNK_REBUILD)
                        .performance(ImpactLevel.LOW).visual(ImpactLevel.NONE).build(),
                new SettingMeta.Builder(FRAME_QUEUE, PERFORMANCE_GPU, "vulkanmod.options.frameQueue",
                        SettingType.INT, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.frameQueue.tooltip")
                        .scope(ApplyScope.SWAPCHAIN).advanced(true)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.NONE).build());
    }

    public static List<SettingMeta> performanceChunks() {
        return List.of(
                new SettingMeta.Builder(ADAPTIVE_CHUNK_UPLOADS, PERFORMANCE_CHUNKS,
                        "vulkanmod.options.adaptiveChunkUploads", SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.adaptiveChunkUploads.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.NONE).build(),
                new SettingMeta.Builder(CHUNK_UPLOADS_PER_FRAME, PERFORMANCE_CHUNKS,
                        "vulkanmod.options.chunkUploadsPerFrame", SettingType.INT, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.chunkUploadsPerFrame.tooltip")
                        .scope(ApplyScope.INSTANT).advanced(true)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.NONE).build(),
                new SettingMeta.Builder(BUILDER_THREADS, PERFORMANCE_CHUNKS,
                        "vulkanmod.options.builderThreads", SettingType.INT, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.builderThreads.tooltip")
                        .scope(ApplyScope.RESTART)
                        .performance(ImpactLevel.HIGH).visual(ImpactLevel.NONE).build()
);
    }

    public static List<SettingMeta> qualityTextures() {
        return List.of(
                new SettingMeta.Builder(MIPMAP_LEVELS, QUALITY_TEXTURES, "options.mipmapLevels",
                        SettingType.ENUM, SettingSource.MINECRAFT)
                        .scope(ApplyScope.TEXTURE_RELOAD)
                        .performance(ImpactLevel.LOW).visual(ImpactLevel.MEDIUM).build(),
                new SettingMeta.Builder(TEXTURE_ANIMATIONS, QUALITY_TEXTURES, "vulkanmod.options.textureAnimations",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.LOW).visual(ImpactLevel.LOW).build(),
                new SettingMeta.Builder(CONNECTED_TEXTURES, QUALITY_TEXTURES, "vulkanmod.options.connectedTextures",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.connectedTextures.tooltip")
                        .scope(ApplyScope.CHUNK_REBUILD)
                        .performance(ImpactLevel.LOW).visual(ImpactLevel.MEDIUM).build(),
                new SettingMeta.Builder(CUSTOM_ITEM_TEXTURES, QUALITY_TEXTURES,
                        "vulkanmod.options.customItemTextures", SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.customItemTextures.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.NONE).visual(ImpactLevel.LOW).build(),
                new SettingMeta.Builder(CORE_SHADER_PACKS, QUALITY_TEXTURES,
                        "vulkanmod.options.coreShaderPacks", SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.coreShaderPacks.tooltip")
                        .scope(ApplyScope.TEXTURE_RELOAD)
                        .performance(ImpactLevel.NONE).visual(ImpactLevel.MEDIUM).build(),
                new SettingMeta.Builder(GLINT_STRENGTH, QUALITY_TEXTURES, "options.glintStrength",
                        SettingType.INT, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.NONE).visual(ImpactLevel.MEDIUM).build(),
                new SettingMeta.Builder(ANISOTROPIC_FILTERING, QUALITY_TEXTURES,
                        "vulkanmod.options.anisotropicFiltering", SettingType.ENUM, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.anisotropicFiltering.tooltip")
                        .scope(ApplyScope.TEXTURE_RELOAD).experimental(true)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.MEDIUM).build()
);
    }

    public static List<SettingMeta> qualityLighting() {
        return List.of(
                new SettingMeta.Builder(AMBIENT_OCCLUSION, QUALITY_LIGHTING, "options.ao",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .scope(ApplyScope.CHUNK_REBUILD)
                        .performance(ImpactLevel.LOW).visual(ImpactLevel.MEDIUM).build(),
                new SettingMeta.Builder(BIOME_BLEND, QUALITY_LIGHTING, "options.biomeBlendRadius",
                        SettingType.INT, SettingSource.MINECRAFT)
                        .scope(ApplyScope.CHUNK_REBUILD)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.LOW).build(),
                new SettingMeta.Builder(DYNAMIC_LIGHT, QUALITY_LIGHTING,
                        "vulkanmod.options.dynamicLight", SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.dynamicLight.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.HIGH).visual(ImpactLevel.HIGH).build());
    }

    public static List<SettingMeta> qualityEnvironment() {
        return List.of(
                new SettingMeta.Builder(GRAPHICS_MODE, QUALITY_ENVIRONMENT, "options.graphics",
                        SettingType.ENUM, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.MEDIUM).build(),
                new SettingMeta.Builder(CLOUDS, QUALITY_ENVIRONMENT, "options.renderClouds",
                        SettingType.ENUM, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.LOW).visual(ImpactLevel.LOW).build(),
                new SettingMeta.Builder(WEATHER_RENDERING, QUALITY_ENVIRONMENT,
                        "vulkanmod.options.weatherRendering", SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.weatherRendering.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.HIGH).build(),
                new SettingMeta.Builder(WIND_STRENGTH, QUALITY_ENVIRONMENT, "vulkanmod.options.windStrength",
                        SettingType.INT, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.windStrength.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.HIGH).visual(ImpactLevel.MEDIUM).build(),
                new SettingMeta.Builder(HORIZON_FOG, QUALITY_ENVIRONMENT, "vulkanmod.options.horizonFog",
                        SettingType.INT, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.horizonFog.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.LOW).visual(ImpactLevel.MEDIUM).build(),
                new SettingMeta.Builder(LEAVES_QUALITY, QUALITY_ENVIRONMENT,
                        "vulkanmod.options.leavesQuality", SettingType.ENUM, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.leavesQuality.tooltip")
                        .scope(ApplyScope.CHUNK_REBUILD)
                        .performance(ImpactLevel.HIGH).visual(ImpactLevel.LOW).build());
    }

    public static List<SettingMeta> renderingAdvanced() {
        return List.of(
                new SettingMeta.Builder(PARTICLES, RENDERING_ADVANCED, "options.particles",
                        SettingType.ENUM, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.MEDIUM).build());
    }

    public static List<SettingMeta> renderingEntities() {
        return List.of(
                new SettingMeta.Builder(ENTITY_SHADOWS, RENDERING_ENTITIES, "options.entityShadows",
                        SettingType.BOOL, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.LOW).visual(ImpactLevel.LOW).build(),
                new SettingMeta.Builder(ENTITY_DISTANCE, RENDERING_ENTITIES, "options.entityDistanceScaling",
                        SettingType.INT, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.LOW).build());
    }

    public static final String GROUP_PARTICLES = "vulkanmod.ui.group.particles";

    public static SettingMeta particleToggle(String particleId, String label) {
        if (particleId == null || particleId.isBlank()) {
            throw new IllegalArgumentException("particleId must not be blank");
        }
        return new SettingMeta.Builder(SettingId.of("particle", particleId), RENDERING_ADVANCED,
                label, SettingType.BOOL, SettingSource.VOLCANIC)
                .group(GROUP_PARTICLES)
                .scope(ApplyScope.INSTANT)
                .performance(ImpactLevel.LOW).visual(ImpactLevel.LOW)
                .build();
    }

    public static List<SettingMeta> developerTools() {
        return List.of(
                new SettingMeta.Builder(SHOW_FPS, DEVELOPER_TOOLS, "vulkanmod.options.showFps",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.showFps.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.NONE).visual(ImpactLevel.LOW).build(),
                new SettingMeta.Builder(SHOW_COORDINATES, DEVELOPER_TOOLS,
                        "vulkanmod.options.showCoordinates", SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.showCoordinates.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.NONE).visual(ImpactLevel.LOW).build(),
                new SettingMeta.Builder(PERF_LOG, DEVELOPER_TOOLS, "vulkanmod.options.perfLog",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.perfLog.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.LOW).visual(ImpactLevel.NONE).build(),
                new SettingMeta.Builder(STATS_SAMPLE_IN_MENUS, DEVELOPER_TOOLS,
                        "vulkanmod.options.statsSampleInMenus", SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.statsSampleInMenus.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.NONE).visual(ImpactLevel.NONE).build(),
                new SettingMeta.Builder(DEBUG_OVERLAY, DEVELOPER_TOOLS, "vulkanmod.options.debugOverlay",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.debugOverlay.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.LOW).visual(ImpactLevel.NONE).build(),
                new SettingMeta.Builder(DEBUG_MENU, DEVELOPER_TOOLS, "vulkanmod.options.debugMenu",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.debugMenu.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.LOW).visual(ImpactLevel.NONE).build(),
                new SettingMeta.Builder(DEBUG_MENU_KEY, DEVELOPER_TOOLS, "vulkanmod.options.debugMenuKey",
                        SettingType.KEY, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.debugMenuKey.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.NONE).visual(ImpactLevel.NONE).build(),
                new SettingMeta.Builder(VSR_DEBUG, DEVELOPER_TOOLS, "vulkanmod.options.vsrDebug",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.vsrDebug.tooltip")
                        .scope(ApplyScope.INSTANT)
                        .performance(ImpactLevel.NONE).visual(ImpactLevel.HIGH).build());
    }

    public static List<SettingMeta> advancedRenderer() {
        return List.of(
                new SettingMeta.Builder(VULKAN_VALIDATION, ADVANCED_RENDERER,
                        "vulkanmod.options.vulkanValidation", SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.vulkanValidation.tooltip")
                        .scope(ApplyScope.RESTART)
                        .performance(ImpactLevel.HIGH).visual(ImpactLevel.NONE).build(),
                new SettingMeta.Builder(GPU_DEVICE, ADVANCED_RENDERER, "vulkanmod.options.deviceSelector",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.deviceSelector.tooltip")
                        .scope(ApplyScope.RESTART)
                        .performance(ImpactLevel.HIGH).visual(ImpactLevel.NONE).build(),
                new SettingMeta.Builder(MOLTENVK_AGGRESSIVE, ADVANCED_RENDERER,
                        "vulkanmod.options.moltenVkAggressive", SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.moltenVkAggressive.tooltip")
                        .scope(ApplyScope.RESTART).advanced(true).experimental(true)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.NONE).build()
);
    }

    public static List<SettingMeta> advancedCompatibility() {
        return List.of(
                new SettingMeta.Builder(EXTERNAL_LOD, ADVANCED_COMPATIBILITY, "vulkanmod.options.externalLod",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.externalLod.tooltip")
                        .scope(ApplyScope.RESTART).advanced(true)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.HIGH).build(),
                new SettingMeta.Builder(EXTERNAL_LOD_DRAW, ADVANCED_COMPATIBILITY,
                        "vulkanmod.options.externalLodDraw", SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.externalLodDraw.tooltip")
                        .scope(ApplyScope.RESTART).advanced(true)
                        .performance(ImpactLevel.MEDIUM).visual(ImpactLevel.HIGH).build(),
                new SettingMeta.Builder(GL_LEGACY_BRIDGE, ADVANCED_COMPATIBILITY,
                        "vulkanmod.options.glLegacyBridge", SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.glLegacyBridge.tooltip")
                        .scope(ApplyScope.RESTART).advanced(true).build(),
                new SettingMeta.Builder(GL_FBO_VIEWPORT, ADVANCED_COMPATIBILITY,
                        "vulkanmod.options.glFboViewport", SettingType.BOOL, SettingSource.VOLCANIC)
                        .descriptionKey("vulkanmod.options.glFboViewport.tooltip")
                        .scope(ApplyScope.RESTART).advanced(true).build());
    }

}
