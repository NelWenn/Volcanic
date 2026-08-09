package net.vulkanmod.config.ui.settings;

import net.vulkanmod.config.ui.core.ApplyScope;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingSource;
import net.vulkanmod.config.ui.core.SettingType;

import java.util.List;

public final class SettingsDefinitions {
    public static final RouteId SHADERS_CURRENT = RouteId.parse("shaders.current");
    public static final SettingId SHADERS_ENABLED = SettingId.parse("vulkanmod:shaders.enabled");
    public static final SettingId SHADERS_SELECTED_PACK = SettingId.parse("vulkanmod:shaders.selected_pack");

    public static final RouteId DISPLAY_GENERAL = RouteId.parse("display.general");
    public static final RouteId DISPLAY_INTERFACE = RouteId.parse("display.interface");
    public static final RouteId DISPLAY_ADVANCED = RouteId.parse("display.advanced");
    public static final RouteId RENDERING_GENERAL = RouteId.parse("rendering.general");
    public static final RouteId RENDERING_RESOLUTION = RouteId.parse("rendering.resolution");
    public static final RouteId RENDERING_CULLING = RouteId.parse("rendering.culling");
    public static final RouteId PERFORMANCE_GENERAL = RouteId.parse("performance.general");
    public static final RouteId PERFORMANCE_GPU = RouteId.parse("performance.gpu");
    public static final RouteId PERFORMANCE_CHUNKS = RouteId.parse("performance.chunks");
    public static final RouteId PERFORMANCE_SYNCHRONIZATION = RouteId.parse("performance.synchronization");
    public static final RouteId QUALITY_GENERAL = RouteId.parse("quality.general");
    public static final RouteId QUALITY_TEXTURES = RouteId.parse("quality.textures");
    public static final RouteId QUALITY_LIGHTING = RouteId.parse("quality.lighting");
    public static final RouteId QUALITY_ENVIRONMENT = RouteId.parse("quality.environment");
    public static final RouteId QUALITY_PARTICLES = RouteId.parse("quality.particles");
    public static final RouteId QUALITY_ENTITIES = RouteId.parse("quality.entities");
    public static final RouteId ADVANCED_RENDERER = RouteId.parse("advanced.renderer");
    public static final RouteId ADVANCED_SYNCHRONIZATION = RouteId.parse("advanced.synchronization");
    public static final RouteId ADVANCED_COMPATIBILITY = RouteId.parse("advanced.compatibility");

    public static final SettingId WINDOW_MODE = SettingId.parse("vulkanmod:display.window_mode");
    public static final SettingId RESOLUTION = SettingId.parse("vulkanmod:display.resolution");
    public static final SettingId REFRESH_RATE = SettingId.parse("vulkanmod:display.refresh_rate");
    public static final SettingId VSYNC = SettingId.parse("minecraft:display.vsync");
    public static final SettingId FRAMERATE_LIMIT = SettingId.parse("minecraft:display.framerate_limit");

    public static final SettingId GUI_SCALE = SettingId.parse("minecraft:display.gui_scale");
    public static final SettingId BRIGHTNESS = SettingId.parse("minecraft:display.brightness");
    public static final SettingId DISTORTION_EFFECTS = SettingId.parse("minecraft:display.distortion_effects");
    public static final SettingId FOV_EFFECTS = SettingId.parse("minecraft:display.fov_effects");
    public static final SettingId DISABLE_HIDPI = SettingId.parse("vulkanmod:display.disable_hidpi");

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
    public static final SettingId PARTICLES = SettingId.parse("minecraft:quality.particles");
    public static final SettingId ENTITY_SHADOWS = SettingId.parse("minecraft:quality.entity_shadows");
    public static final SettingId ENTITY_DISTANCE = SettingId.parse("minecraft:quality.entity_distance");

    public static final SettingId GPU_DEVICE = SettingId.parse("vulkanmod:advanced.gpu_device");
    public static final SettingId MOLTENVK_AGGRESSIVE = SettingId.parse("vulkanmod:advanced.moltenvk_aggressive");
    public static final SettingId EXTERNAL_LOD = SettingId.parse("vulkanmod:advanced.external_lod");
    public static final SettingId EXTERNAL_LOD_DRAW = SettingId.parse("vulkanmod:advanced.external_lod_draw");
    public static final SettingId GL_LEGACY_BRIDGE = SettingId.parse("vulkanmod:advanced.gl_legacy_bridge");
    public static final SettingId GL_FBO_VIEWPORT = SettingId.parse("vulkanmod:advanced.gl_fbo_viewport");

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

    private SettingsDefinitions() {
    }

    public static List<SettingMeta> displayGeneral() {
        return List.of(
                new SettingMeta.Builder(WINDOW_MODE, DISPLAY_GENERAL, "vulkanmod.options.windowMode",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .scope(ApplyScope.WINDOW).build(),
                new SettingMeta.Builder(RESOLUTION, DISPLAY_GENERAL, "options.fullscreen.resolution",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .scope(ApplyScope.WINDOW).build(),
                new SettingMeta.Builder(REFRESH_RATE, DISPLAY_GENERAL, "vulkanmod.options.refreshRate",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .scope(ApplyScope.WINDOW).build(),
                new SettingMeta.Builder(VSYNC, DISPLAY_GENERAL, "options.vsync",
                        SettingType.BOOL, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT).build(),
                new SettingMeta.Builder(FRAMERATE_LIMIT, DISPLAY_GENERAL, "options.framerateLimit",
                        SettingType.INT, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT).build());
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
                        .scope(ApplyScope.INSTANT).build());
    }

    public static List<SettingMeta> shadersCurrent() {
        return List.of(
                new SettingMeta.Builder(SHADERS_ENABLED, SHADERS_CURRENT, "vulkanmod.options.shadersEnabled",
                        SettingType.BOOL, SettingSource.SHADERS)
                        .scope(ApplyScope.INSTANT).build(),
                new SettingMeta.Builder(SHADERS_SELECTED_PACK, SHADERS_CURRENT, "vulkanmod.options.selectedShader",
                        SettingType.ENUM, SettingSource.SHADERS)
                        .scope(ApplyScope.INSTANT).build());
    }

    public static List<SettingMeta> displayAdvanced() {
        return List.of(
                new SettingMeta.Builder(DISABLE_HIDPI, DISPLAY_ADVANCED, "vulkanmod.options.disableHiDPI",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .scope(ApplyScope.RESTART).advanced(true).build());
    }

    public static List<SettingMeta> renderingGeneral() {
        return List.of(
                new SettingMeta.Builder(RENDER_DISTANCE, RENDERING_GENERAL, "options.renderDistance",
                        SettingType.INT, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT).build(),
                new SettingMeta.Builder(SIMULATION_DISTANCE, RENDERING_GENERAL, "options.simulationDistance",
                        SettingType.INT, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT).build(),
                new SettingMeta.Builder(CHUNK_UPDATE_PRIORITY, RENDERING_GENERAL, "options.prioritizeChunkUpdates",
                        SettingType.ENUM, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT).build(),
                new SettingMeta.Builder(CHUNK_FADE_IN, RENDERING_GENERAL, "vulkanmod.options.chunkFadeIn",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .scope(ApplyScope.INSTANT).build());
    }

    public static List<SettingMeta> renderingResolution() {
        return List.of(
                new SettingMeta.Builder(VSR_PRESET, RENDERING_RESOLUTION, "vulkanmod.options.vsrPreset",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .scope(ApplyScope.INSTANT).build(),
                new SettingMeta.Builder(VSR_UPSCALER, RENDERING_RESOLUTION, "vulkanmod.options.vsrBackend",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .scope(ApplyScope.INSTANT).build(),
                new SettingMeta.Builder(VSR_RENDER_SCALE, RENDERING_RESOLUTION, "vulkanmod.options.renderScale",
                        SettingType.INT, SettingSource.VOLCANIC)
                        .scope(ApplyScope.INSTANT).build(),
                new SettingMeta.Builder(VSR_SHARPNESS, RENDERING_RESOLUTION, "vulkanmod.options.vsrSharpness",
                        SettingType.INT, SettingSource.VOLCANIC)
                        .scope(ApplyScope.INSTANT).build());
    }

    public static List<SettingMeta> renderingCulling() {
        return List.of(
                new SettingMeta.Builder(CULLING_OCCLUSION, RENDERING_CULLING, "vulkanmod.options.occlusionCulling",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .scope(ApplyScope.CHUNK_REBUILD).build(),
                new SettingMeta.Builder(CULLING_MODE, RENDERING_CULLING, "vulkanmod.options.advCulling",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .scope(ApplyScope.CHUNK_REBUILD).advanced(true).build(),
                new SettingMeta.Builder(CULLING_BLOCK_ENTITIES, RENDERING_CULLING, "vulkanmod.options.blockEntityCulling",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .scope(ApplyScope.CHUNK_REBUILD).build(),
                new SettingMeta.Builder(CULLING_PARTICLES, RENDERING_CULLING, "vulkanmod.options.particleCulling",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .scope(ApplyScope.INSTANT).build(),
                new SettingMeta.Builder(CULLING_LOD_GPU, RENDERING_CULLING, "vulkanmod.options.lodGpuCulling",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .scope(ApplyScope.CHUNK_REBUILD).advanced(true).build());
    }

    public static List<SettingMeta> performanceGeneral() {
        return List.of(
                new SettingMeta.Builder(PERFORMANCE_PROFILE, PERFORMANCE_GENERAL,
                        "vulkanmod.options.performancePreset", SettingType.ENUM, SettingSource.VOLCANIC)
                        .scope(ApplyScope.CHUNK_REBUILD).build());
    }

    public static List<SettingMeta> performanceGpu() {
        return List.of(
                new SettingMeta.Builder(INDIRECT_DRAW, PERFORMANCE_GPU, "vulkanmod.options.indirectDraw",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .scope(ApplyScope.CHUNK_REBUILD).build(),
                new SettingMeta.Builder(UNIQUE_OPAQUE_LAYER, PERFORMANCE_GPU, "vulkanmod.options.uniqueOpaqueLayer",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .scope(ApplyScope.CHUNK_REBUILD).build());
    }

    public static List<SettingMeta> performanceChunks() {
        return List.of(
                new SettingMeta.Builder(ADAPTIVE_CHUNK_UPLOADS, PERFORMANCE_CHUNKS,
                        "vulkanmod.options.adaptiveChunkUploads", SettingType.BOOL, SettingSource.VOLCANIC)
                        .scope(ApplyScope.INSTANT).build(),
                new SettingMeta.Builder(CHUNK_UPLOADS_PER_FRAME, PERFORMANCE_CHUNKS,
                        "vulkanmod.options.chunkUploadsPerFrame", SettingType.INT, SettingSource.VOLCANIC)
                        .scope(ApplyScope.INSTANT).advanced(true).build());
    }

    public static List<SettingMeta> performanceSynchronization() {
        return List.of(
                new SettingMeta.Builder(FRAME_QUEUE, PERFORMANCE_SYNCHRONIZATION, "vulkanmod.options.frameQueue",
                        SettingType.INT, SettingSource.VOLCANIC)
                        .scope(ApplyScope.SWAPCHAIN).advanced(true).build());
    }

    public static List<SettingMeta> qualityGeneral() {
        return List.of(
                new SettingMeta.Builder(GRAPHICS_MODE, QUALITY_GENERAL, "options.graphics",
                        SettingType.ENUM, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT).build());
    }

    public static List<SettingMeta> qualityTextures() {
        return List.of(
                new SettingMeta.Builder(MIPMAP_LEVELS, QUALITY_TEXTURES, "options.mipmapLevels",
                        SettingType.ENUM, SettingSource.MINECRAFT)
                        .scope(ApplyScope.TEXTURE_RELOAD).build(),
                new SettingMeta.Builder(TEXTURE_ANIMATIONS, QUALITY_TEXTURES, "vulkanmod.options.textureAnimations",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .scope(ApplyScope.INSTANT).build(),
                new SettingMeta.Builder(CONNECTED_TEXTURES, QUALITY_TEXTURES, "vulkanmod.options.connectedTextures",
                        SettingType.BOOL, SettingSource.VOLCANIC)
                        .scope(ApplyScope.CHUNK_REBUILD).build(),
                new SettingMeta.Builder(CUSTOM_ITEM_TEXTURES, QUALITY_TEXTURES,
                        "vulkanmod.options.customItemTextures", SettingType.BOOL, SettingSource.VOLCANIC)
                        .scope(ApplyScope.INSTANT).build());
    }

    public static List<SettingMeta> qualityLighting() {
        return List.of(
                new SettingMeta.Builder(AMBIENT_OCCLUSION, QUALITY_LIGHTING, "options.ao",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .scope(ApplyScope.CHUNK_REBUILD).build(),
                new SettingMeta.Builder(BIOME_BLEND, QUALITY_LIGHTING, "options.biomeBlendRadius",
                        SettingType.INT, SettingSource.MINECRAFT)
                        .scope(ApplyScope.CHUNK_REBUILD).build());
    }

    public static List<SettingMeta> qualityEnvironment() {
        return List.of(
                new SettingMeta.Builder(CLOUDS, QUALITY_ENVIRONMENT, "options.renderClouds",
                        SettingType.ENUM, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT).build());
    }

    public static List<SettingMeta> qualityParticles() {
        return List.of(
                new SettingMeta.Builder(PARTICLES, QUALITY_PARTICLES, "options.particles",
                        SettingType.ENUM, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT).build());
    }

    public static List<SettingMeta> qualityEntities() {
        return List.of(
                new SettingMeta.Builder(ENTITY_SHADOWS, QUALITY_ENTITIES, "options.entityShadows",
                        SettingType.BOOL, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT).build(),
                new SettingMeta.Builder(ENTITY_DISTANCE, QUALITY_ENTITIES, "options.entityDistanceScaling",
                        SettingType.INT, SettingSource.MINECRAFT)
                        .scope(ApplyScope.INSTANT).build());
    }

    public static List<SettingMeta> advancedRenderer() {
        return List.of(
                new SettingMeta.Builder(GPU_DEVICE, ADVANCED_RENDERER, "vulkanmod.options.deviceSelector",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .scope(ApplyScope.RESTART).build());
    }

    public static List<SettingMeta> advancedSynchronization() {
        return List.of(
                new SettingMeta.Builder(MOLTENVK_AGGRESSIVE, ADVANCED_SYNCHRONIZATION,
                        "vulkanmod.options.moltenVkAggressive", SettingType.BOOL, SettingSource.VOLCANIC)
                        .scope(ApplyScope.RESTART).advanced(true).build());
    }

    public static List<SettingMeta> advancedCompatibility() {
        return List.of(
                new SettingMeta.Builder(EXTERNAL_LOD, ADVANCED_COMPATIBILITY, "vulkanmod.options.externalLod",
                        SettingType.ENUM, SettingSource.VOLCANIC)
                        .scope(ApplyScope.RESTART).advanced(true).build(),
                new SettingMeta.Builder(EXTERNAL_LOD_DRAW, ADVANCED_COMPATIBILITY,
                        "vulkanmod.options.externalLodDraw", SettingType.BOOL, SettingSource.VOLCANIC)
                        .scope(ApplyScope.RESTART).advanced(true).build(),
                new SettingMeta.Builder(GL_LEGACY_BRIDGE, ADVANCED_COMPATIBILITY,
                        "vulkanmod.options.glLegacyBridge", SettingType.BOOL, SettingSource.VOLCANIC)
                        .scope(ApplyScope.RESTART).advanced(true).build(),
                new SettingMeta.Builder(GL_FBO_VIEWPORT, ADVANCED_COMPATIBILITY,
                        "vulkanmod.options.glFboViewport", SettingType.BOOL, SettingSource.VOLCANIC)
                        .scope(ApplyScope.RESTART).advanced(true).build());
    }
}
