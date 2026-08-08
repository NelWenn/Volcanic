package net.vulkanmod.config.ui.settings;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.resources.language.I18n;
import net.vulkanmod.Initializer;
import net.vulkanmod.api.LodCulling;
import net.vulkanmod.compat.capabilities.ExternalRenderPathOptions;
import net.vulkanmod.compat.external.ExternalRenderPathSupport;
import net.vulkanmod.config.GraphicsModeCompatibility;
import net.vulkanmod.config.PerformancePreset;
import net.vulkanmod.config.PerformancePresetApplier;
import net.vulkanmod.config.Platform;
import net.vulkanmod.config.RenderScale;
import net.vulkanmod.config.VsrPreset;
import net.vulkanmod.config.option.Options;
import net.vulkanmod.render.chunk.build.TaskDispatcher;
import net.vulkanmod.render.chunk.build.light.LightMode;
import net.vulkanmod.render.vsr.Vsr;
import net.vulkanmod.vulkan.FrameTimer;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.Device;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.pass.DefaultMainPass;
import net.vulkanmod.config.ui.core.OverviewModel;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingRegistry;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.config.video.VideoModeSet;
import net.vulkanmod.config.video.WindowMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// Choice labels are translation keys; Minecraft renders an unknown key as itself, so plain
// values such as "1920 x 1080" or "144" pass through untranslated.
public final class SettingsCatalog {
    private static final int CULLING_MODE_AGGRESSIVE = 1;
    private static final int CULLING_MODE_NORMAL = 2;
    private static final int CULLING_MODE_CONSERVATIVE = 3;
    private static final List<Integer> CULLING_MODES =
            List.of(CULLING_MODE_NORMAL, CULLING_MODE_CONSERVATIVE, CULLING_MODE_AGGRESSIVE);

    private static final int PARTICLE_CULLING_OFF = 0;
    private static final int PARTICLE_CULLING_QUALITY = 1;
    private static final int PARTICLE_CULLING_BALANCED = 2;
    private static final int PARTICLE_CULLING_PERFORMANCE = 3;
    private static final List<Integer> PARTICLE_CULLING_LEVELS = List.of(PARTICLE_CULLING_OFF,
            PARTICLE_CULLING_QUALITY, PARTICLE_CULLING_BALANCED, PARTICLE_CULLING_PERFORMANCE);

    private static final List<Integer> UPSCALERS =
            List.of(Vsr.BILINEAR, Vsr.FSR1, Vsr.VTU, Vsr.SHARPEN_ONLY);

    private static final List<Integer> MIPMAP_LEVELS = List.of(0, 1, 2, 3, 4);
    private static final int MIPMAP_LEVELS_DEFAULT = 4;

    private static final List<Integer> AMBIENT_OCCLUSION_MODES =
            List.of(LightMode.FLAT, LightMode.SMOOTH, LightMode.SUB_BLOCK);

    private static final int DEVICE_AUTO = -1;

    private static final List<ExternalRenderPathSupport.Mode> EXTERNAL_LOD_MODES =
            List.of(ExternalRenderPathSupport.Mode.OFF, ExternalRenderPathSupport.Mode.SAFE,
                    ExternalRenderPathSupport.Mode.EXPERIMENTAL);

    private final SettingRegistry registry = new SettingRegistry();
    private final Map<SettingId, SettingBinding> bindings = new LinkedHashMap<>();

    public SettingsCatalog() {
        bindDisplayGeneral();
        bindDisplayInterface();
        bindDisplayAdvanced();
        bindRenderingGeneral();
        bindRenderingResolution();
        bindRenderingCulling();
        bindPerformanceGeneral();
        bindPerformanceGpu();
        bindPerformanceChunks();
        bindPerformanceSynchronization();
        bindQualityGeneral();
        bindQualityTextures();
        bindQualityLighting();
        bindQualityEnvironment();
        bindQualityParticles();
        bindQualityEntities();
        bindAdvancedRenderer();
        bindAdvancedSynchronization();
        bindAdvancedCompatibility();

        registerAll(SettingsDefinitions.displayGeneral());
        registerAll(SettingsDefinitions.displayInterface());
        registerAll(SettingsDefinitions.displayAdvanced());
        registerAll(SettingsDefinitions.renderingGeneral());
        registerAll(SettingsDefinitions.renderingResolution());
        registerAll(SettingsDefinitions.renderingCulling());
        registerAll(SettingsDefinitions.performanceGeneral());
        registerAll(SettingsDefinitions.performanceGpu());
        registerAll(SettingsDefinitions.performanceChunks());
        registerAll(SettingsDefinitions.performanceSynchronization());
        registerAll(SettingsDefinitions.qualityGeneral());
        registerAll(SettingsDefinitions.qualityTextures());
        registerAll(SettingsDefinitions.qualityLighting());
        registerAll(SettingsDefinitions.qualityEnvironment());
        registerAll(SettingsDefinitions.qualityParticles());
        registerAll(SettingsDefinitions.qualityEntities());
        registerAll(SettingsDefinitions.advancedRenderer());
        registerAll(SettingsDefinitions.advancedSynchronization());
        registerAll(SettingsDefinitions.advancedCompatibility());
    }

    private void registerAll(List<SettingMeta> definitions) {
        for (SettingMeta meta : definitions) {
            registry.register(meta);
            if (!bindings.containsKey(meta.id())) {
                throw new IllegalStateException("no binding for setting id " + meta.id());
            }
        }
    }

    public SettingRegistry registry() {
        return registry;
    }

    public SettingBinding binding(SettingId id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        SettingBinding binding = bindings.get(id);
        if (binding == null) {
            throw new IllegalArgumentException("no binding for setting id " + id);
        }
        return binding;
    }

    public boolean enabled(SettingId id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (!bindings.containsKey(id)) {
            throw new IllegalArgumentException("no binding for setting id " + id);
        }
        if (SettingsDefinitions.RESOLUTION.equals(id) || SettingsDefinitions.REFRESH_RATE.equals(id)) {
            return windowMode() == WindowMode.EXCLUSIVE_FULLSCREEN;
        }
        if (SettingsDefinitions.DISABLE_HIDPI.equals(id)) {
            return Platform.isMacOS();
        }
        if (SettingsDefinitions.VSR_UPSCALER.equals(id)) {
            return VsrPreset.current(Initializer.CONFIG).isEnabled();
        }
        if (SettingsDefinitions.VSR_RENDER_SCALE.equals(id) || SettingsDefinitions.VSR_SHARPNESS.equals(id)) {
            return VsrPreset.current(Initializer.CONFIG).isCustom();
        }
        if (SettingsDefinitions.CULLING_LOD_GPU.equals(id)) {
            return Initializer.CONFIG.indirectDraw && DeviceManager.supportsFastIndirectDraw()
                    && lodCullingAvailable();
        }
        if (SettingsDefinitions.INDIRECT_DRAW.equals(id)) {
            return DeviceManager.supportsFastIndirectDraw();
        }
        if (SettingsDefinitions.CHUNK_UPLOADS_PER_FRAME.equals(id)) {
            return !Initializer.CONFIG.adaptiveChunkUploads;
        }
        if (SettingsDefinitions.MOLTENVK_AGGRESSIVE.equals(id)) {
            return Platform.isMacOS();
        }
        if (SettingsDefinitions.EXTERNAL_LOD_DRAW.equals(id)) {
            return configuredExternalLodMode() != ExternalRenderPathSupport.Mode.OFF;
        }
        return true;
    }

    public OverviewModel overview() {
        int scale = RenderScale.clamp(Initializer.CONFIG.renderScale);
        return new OverviewModel()
                .add("vulkanmod.overview.profile",
                        Optional.of(choiceText(SettingsDefinitions.PERFORMANCE_PROFILE)))
                .add("vulkanmod.overview.gpu", gpuName())
                .add("vulkanmod.overview.resolution", resolutionText(RenderScale.MAX))
                .add("vulkanmod.overview.refresh_rate", whenEnabled(SettingsDefinitions.REFRESH_RATE))
                .add("vulkanmod.overview.internal_resolution",
                        Optional.of(valueText(SettingsDefinitions.VSR_RENDER_SCALE)))
                .add("vulkanmod.overview.effective_resolution",
                        RenderScale.isScaled(scale) ? resolutionText(scale) : Optional.empty())
                .add("vulkanmod.overview.upscaler", whenEnabled(SettingsDefinitions.VSR_UPSCALER))
                .add("vulkanmod.overview.vsync", Optional.of(toggleText(SettingsDefinitions.VSYNC)))
                .add("vulkanmod.overview.render_distance",
                        Optional.of(valueText(SettingsDefinitions.RENDER_DISTANCE)))
                .add("vulkanmod.overview.occlusion_culling",
                        Optional.of(toggleText(SettingsDefinitions.CULLING_OCCLUSION)))
                .add("vulkanmod.overview.shader_pack", shaderPack())
                .add("vulkanmod.overview.gpu_frame_time", measuredGpuFrameTime());
    }

    private Optional<String> whenEnabled(SettingId id) {
        return enabled(id) ? Optional.of(choiceText(id)) : Optional.empty();
    }

    private String choiceText(SettingId id) {
        SettingBinding binding = binding(id);
        return I18n.get(binding.display(binding.get()));
    }

    private String valueText(SettingId id) {
        SettingBinding binding = binding(id);
        return binding.display(binding.get());
    }

    private String toggleText(SettingId id) {
        return I18n.get(Boolean.TRUE.equals(binding(id).get()) ? "options.on" : "options.off");
    }

    private static Optional<String> gpuName() {
        Device device = DeviceManager.device;
        if (device == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(device.deviceName).filter(name -> !name.isBlank());
    }

    private static Optional<String> resolutionText(int scale) {
        SwapChain swapChain = Vulkan.getSwapChain();
        if (swapChain == null || swapChain.getWidth() <= 0 || swapChain.getHeight() <= 0) {
            return Optional.empty();
        }
        return Optional.of(RenderScale.scaleDimension(swapChain.getWidth(), scale) + " x "
                + RenderScale.scaleDimension(swapChain.getHeight(), scale));
    }

    private static Optional<String> shaderPack() {
        if (!DefaultMainPass.postShaderActive()) {
            return Optional.empty();
        }
        String selected = Initializer.CONFIG.selectedShader;
        String key = "vulkanmod.options.shader." + selected;
        return Optional.of(I18n.exists(key) ? I18n.get(key) : selected);
    }

    private static Optional<String> measuredGpuFrameTime() {
        double gpuMs = FrameTimer.gpuMs();
        return gpuMs > 0.0 ? Optional.of(String.format(Locale.ROOT, "%.2f ms", gpuMs)) : Optional.empty();
    }

    private void bindDisplayGeneral() {
        bindings.put(SettingsDefinitions.WINDOW_MODE, SettingBinding.choosing(
                () -> WindowMode.getComponentName(windowMode()),
                value -> {
                    WindowMode mode = windowModeFor(label(value));
                    Minecraft.getInstance().options.fullscreen().set(mode == WindowMode.EXCLUSIVE_FULLSCREEN);
                    Initializer.CONFIG.windowedFullscreen = mode == WindowMode.WINDOWED_FULLSCREEN;
                    Options.fullscreenDirty = true;
                },
                () -> Arrays.stream(WindowMode.values()).map(WindowMode::getComponentName).toList())
                .withDefault(() -> WindowMode.getComponentName(WindowMode.WINDOWED)));

        bindings.put(SettingsDefinitions.RESOLUTION, SettingBinding.choosing(
                () -> selectedResolution().toString(),
                value -> {
                    VideoModeSet resolution = resolutionFor(label(value));
                    int refreshRate = selectedVideoMode().refreshRate;
                    VideoModeManager.selectedVideoMode = resolution.hasRefreshRate(refreshRate)
                            ? resolution.getVideoMode(refreshRate)
                            : resolution.getVideoMode();
                    applyVideoMode();
                },
                () -> Arrays.stream(VideoModeManager.getVideoResolutions()).map(VideoModeSet::toString).toList())
                .withDefault(() -> VideoModeManager.getFirstAvailable().toString()));

        bindings.put(SettingsDefinitions.REFRESH_RATE, SettingBinding.choosing(
                () -> String.valueOf(selectedVideoMode().refreshRate),
                value -> {
                    int refreshRate = Integer.parseInt(label(value));
                    VideoModeSet resolution = selectedResolution();
                    if (!resolution.hasRefreshRate(refreshRate)) {
                        throw new IllegalArgumentException("refresh rate " + refreshRate
                                + " is not available at " + resolution);
                    }
                    selectedVideoMode().refreshRate = refreshRate;
                    applyVideoMode();
                },
                () -> selectedResolution().getRefreshRates().stream().map(String::valueOf).toList())
                .withDefault(() -> String.valueOf(highestRefreshRate())));

        bindings.put(SettingsDefinitions.VSYNC, SettingBinding.of(
                () -> Minecraft.getInstance().options.enableVsync().get(),
                value -> {
                    boolean enabled = boolValue(value);
                    Minecraft.getInstance().options.enableVsync().set(enabled);
                    Minecraft.getInstance().getWindow().updateVsync(enabled);
                }).withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.FRAMERATE_LIMIT, SettingBinding.ranged(
                () -> Minecraft.getInstance().options.framerateLimit().get(),
                value -> {
                    int limit = intValue(value);
                    Minecraft.getInstance().options.framerateLimit().set(limit);
                    Minecraft.getInstance().getWindow().setFramerateLimit(limit);
                },
                SettingsDefinitions.FRAMERATE_LIMIT_MIN, SettingsDefinitions.FRAMERATE_LIMIT_MAX,
                SettingsDefinitions.FRAMERATE_LIMIT_STEP)
                .withDefault(() -> SettingsDefinitions.FRAMERATE_LIMIT_DEFAULT)
                .withFormatter(SettingsCatalog::framerateLimitLabel));
    }

    private void bindDisplayInterface() {
        bindings.put(SettingsDefinitions.GUI_SCALE, SettingBinding.ranged(
                () -> Minecraft.getInstance().options.guiScale().get(),
                value -> Minecraft.getInstance().options.guiScale().set(intValue(value)),
                SettingsDefinitions.GUI_SCALE_MIN, SettingsCatalog::maxGuiScale,
                SettingsDefinitions.GUI_SCALE_STEP)
                .withDefault(() -> SettingsDefinitions.GUI_SCALE_AUTO)
                .withFormatter(SettingsCatalog::guiScaleLabel));

        bindings.put(SettingsDefinitions.BRIGHTNESS, SettingBinding.scaled(
                () -> Minecraft.getInstance().options.gamma().get(),
                value -> Minecraft.getInstance().options.gamma().set(doubleValue(value)),
                SettingsDefinitions.BRIGHTNESS_MIN, SettingsDefinitions.BRIGHTNESS_MAX,
                SettingsDefinitions.BRIGHTNESS_STEP, SettingsDefinitions.PERCENT_SCALE)
                .withDefault(() -> SettingsDefinitions.BRIGHTNESS_DEFAULT)
                .withFormatter(SettingsCatalog::brightnessLabel));

        bindings.put(SettingsDefinitions.DISTORTION_EFFECTS, SettingBinding.scaled(
                () -> Minecraft.getInstance().options.screenEffectScale().get(),
                value -> Minecraft.getInstance().options.screenEffectScale().set(doubleValue(value)),
                SettingsDefinitions.EFFECT_SCALE_MIN, SettingsDefinitions.EFFECT_SCALE_MAX,
                SettingsDefinitions.EFFECT_SCALE_STEP, SettingsDefinitions.PERCENT_SCALE)
                .withDefault(() -> SettingsDefinitions.EFFECT_SCALE_DEFAULT)
                .withFormatter(SettingsCatalog::percentLabel));

        bindings.put(SettingsDefinitions.SCREEN_EFFECTS_SCALE, SettingBinding.scaled(
                () -> Minecraft.getInstance().options.fovEffectScale().get(),
                value -> Minecraft.getInstance().options.fovEffectScale().set(doubleValue(value)),
                SettingsDefinitions.EFFECT_SCALE_MIN, SettingsDefinitions.EFFECT_SCALE_MAX,
                SettingsDefinitions.EFFECT_SCALE_STEP, SettingsDefinitions.PERCENT_SCALE)
                .withDefault(() -> SettingsDefinitions.EFFECT_SCALE_DEFAULT)
                .withFormatter(SettingsCatalog::percentLabel));
    }

    private void bindDisplayAdvanced() {
        bindings.put(SettingsDefinitions.DISABLE_HIDPI, SettingBinding.of(
                () -> Initializer.CONFIG.disableHiDPI,
                value -> Initializer.CONFIG.disableHiDPI = boolValue(value))
                .withDefault(() -> Boolean.FALSE));
    }

    private void bindRenderingGeneral() {
        bindings.put(SettingsDefinitions.RENDER_DISTANCE, SettingBinding.ranged(
                () -> Minecraft.getInstance().options.renderDistance().get(),
                value -> Minecraft.getInstance().options.renderDistance().set(intValue(value)),
                SettingsDefinitions.RENDER_DISTANCE_MIN, SettingsDefinitions.RENDER_DISTANCE_MAX,
                SettingsDefinitions.RENDER_DISTANCE_STEP)
                .withDefault(() -> SettingsDefinitions.RENDER_DISTANCE_DEFAULT));

        bindings.put(SettingsDefinitions.SIMULATION_DISTANCE, SettingBinding.ranged(
                () -> Minecraft.getInstance().options.simulationDistance().get(),
                value -> Minecraft.getInstance().options.simulationDistance().set(intValue(value)),
                SettingsDefinitions.SIMULATION_DISTANCE_MIN, SettingsDefinitions.SIMULATION_DISTANCE_MAX,
                SettingsDefinitions.SIMULATION_DISTANCE_STEP)
                .withDefault(() -> SettingsDefinitions.SIMULATION_DISTANCE_DEFAULT));

        bindings.put(SettingsDefinitions.CHUNK_UPDATE_PRIORITY, SettingBinding.choosing(
                () -> Minecraft.getInstance().options.prioritizeChunkUpdates().get().getKey(),
                value -> Minecraft.getInstance().options.prioritizeChunkUpdates()
                        .set(chunkUpdatePriorityFor(label(value))),
                () -> Arrays.stream(PrioritizeChunkUpdates.values())
                        .map(PrioritizeChunkUpdates::getKey).toList())
                .withDefault(() -> PrioritizeChunkUpdates.NONE.getKey()));

        bindings.put(SettingsDefinitions.CHUNK_FADE_IN, SettingBinding.of(
                () -> Initializer.CONFIG.chunkFadeIn,
                value -> Initializer.CONFIG.chunkFadeIn = boolValue(value))
                .withDefault(() -> Boolean.TRUE));
    }

    private void bindRenderingResolution() {
        bindings.put(SettingsDefinitions.VSR_PRESET, SettingBinding.choosing(
                () -> VsrPreset.current(Initializer.CONFIG).translationKey,
                value -> {
                    VsrPreset preset = vsrPresetFor(label(value));
                    Initializer.CONFIG.vsrPreset = preset.id;
                    preset.apply(Initializer.CONFIG);
                },
                () -> Arrays.stream(VsrPreset.values()).map(preset -> preset.translationKey).toList())
                .withDefault(() -> VsrPreset.OFF.translationKey));

        bindings.put(SettingsDefinitions.VSR_UPSCALER, SettingBinding.choosing(
                () -> upscalerKey(Vsr.clampBackend(Initializer.CONFIG.vsrBackend)),
                value -> Initializer.CONFIG.vsrBackend = upscalerFor(label(value)),
                () -> UPSCALERS.stream().map(SettingsCatalog::upscalerKey).toList())
                .withDefault(() -> upscalerKey(Vsr.BILINEAR)));

        bindings.put(SettingsDefinitions.VSR_RENDER_SCALE, SettingBinding.ranged(
                () -> RenderScale.clamp(Initializer.CONFIG.renderScale),
                value -> Initializer.CONFIG.renderScale = RenderScale.clamp(intValue(value)),
                RenderScale.MIN, RenderScale.MAX, RenderScale.STEP)
                .withDefault(() -> RenderScale.DEFAULT)
                .withFormatter(SettingsCatalog::percentLabel));

        bindings.put(SettingsDefinitions.VSR_SHARPNESS, SettingBinding.scaled(
                () -> Initializer.CONFIG.vsrSharpness,
                value -> Initializer.CONFIG.vsrSharpness = (float) doubleValue(value),
                SettingsDefinitions.SHARPNESS_MIN, SettingsDefinitions.SHARPNESS_MAX,
                SettingsDefinitions.SHARPNESS_STEP, SettingsDefinitions.PERCENT_SCALE)
                .withDefault(() -> SettingsDefinitions.SHARPNESS_DEFAULT)
                .withFormatter(SettingsCatalog::percentLabel));
    }

    private void bindRenderingCulling() {
        bindings.put(SettingsDefinitions.CULLING_OCCLUSION, SettingBinding.of(
                () -> Initializer.CONFIG.lodDepthSnapshot,
                value -> {
                    Initializer.CONFIG.lodDepthSnapshot = boolValue(value);
                    Minecraft.getInstance().levelRenderer.allChanged();
                })
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.CULLING_MODE, SettingBinding.choosing(
                () -> cullingModeKey(Initializer.CONFIG.advCulling),
                value -> {
                    Initializer.CONFIG.advCulling = cullingModeFor(label(value));
                    Minecraft.getInstance().levelRenderer.allChanged();
                },
                () -> CULLING_MODES.stream().map(SettingsCatalog::cullingModeKey).toList())
                .withDefault(() -> cullingModeKey(CULLING_MODE_NORMAL)));

        bindings.put(SettingsDefinitions.CULLING_BLOCK_ENTITIES, SettingBinding.of(
                () -> Initializer.CONFIG.blockEntityCulling,
                value -> {
                    Initializer.CONFIG.blockEntityCulling = boolValue(value);
                    Minecraft.getInstance().levelRenderer.allChanged();
                })
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.CULLING_PARTICLES, SettingBinding.choosing(
                () -> particleCullingKey(Initializer.CONFIG.particleCulling),
                value -> Initializer.CONFIG.particleCulling = particleCullingFor(label(value)),
                () -> PARTICLE_CULLING_LEVELS.stream().map(SettingsCatalog::particleCullingKey).toList())
                .withDefault(() -> particleCullingKey(PARTICLE_CULLING_BALANCED)));

        bindings.put(SettingsDefinitions.CULLING_LOD_GPU, SettingBinding.of(
                () -> Initializer.CONFIG.lodGpuCulling,
                value -> {
                    Initializer.CONFIG.lodGpuCulling = boolValue(value);
                    Minecraft.getInstance().levelRenderer.allChanged();
                })
                .withDefault(() -> Boolean.FALSE));
    }

    private void bindPerformanceGeneral() {
        bindings.put(SettingsDefinitions.PERFORMANCE_PROFILE, SettingBinding.choosing(
                () -> PerformancePreset.byId(Initializer.CONFIG.performancePreset).translationKey,
                value -> PerformancePresetApplier.apply(performanceProfileFor(label(value)),
                        Initializer.CONFIG, Minecraft.getInstance()),
                () -> performanceProfiles().stream().map(preset -> preset.translationKey).toList())
                .withDefault(() -> PerformancePreset.BALANCED.translationKey));
    }

    private void bindPerformanceGpu() {
        bindings.put(SettingsDefinitions.INDIRECT_DRAW, SettingBinding.of(
                () -> Initializer.CONFIG.indirectDraw && DeviceManager.supportsFastIndirectDraw(),
                value -> {
                    Initializer.CONFIG.indirectDraw = boolValue(value) && DeviceManager.supportsFastIndirectDraw();
                    Minecraft.getInstance().levelRenderer.allChanged();
                })
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.UNIQUE_OPAQUE_LAYER, SettingBinding.of(
                () -> Initializer.CONFIG.uniqueOpaqueLayer,
                value -> {
                    Initializer.CONFIG.uniqueOpaqueLayer = boolValue(value);
                    Minecraft.getInstance().levelRenderer.allChanged();
                })
                .withDefault(() -> Boolean.TRUE));
    }

    private void bindPerformanceChunks() {
        bindings.put(SettingsDefinitions.ADAPTIVE_CHUNK_UPLOADS, SettingBinding.of(
                () -> Initializer.CONFIG.adaptiveChunkUploads,
                value -> Initializer.CONFIG.adaptiveChunkUploads = boolValue(value))
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.CHUNK_UPLOADS_PER_FRAME, SettingBinding.ranged(
                () -> TaskDispatcher.clampMaxUploadsPerFrame(Initializer.CONFIG.chunkUploadsPerFrame),
                value -> Initializer.CONFIG.chunkUploadsPerFrame =
                        TaskDispatcher.clampMaxUploadsPerFrame(intValue(value)),
                SettingsDefinitions.CHUNK_UPLOADS_PER_FRAME_MIN, SettingsDefinitions.CHUNK_UPLOADS_PER_FRAME_MAX,
                SettingsDefinitions.CHUNK_UPLOADS_PER_FRAME_STEP)
                .withDefault(() -> PerformancePreset.BALANCED.chunkUploadsPerFrame));
    }

    private void bindPerformanceSynchronization() {
        bindings.put(SettingsDefinitions.FRAME_QUEUE, SettingBinding.ranged(
                () -> Initializer.CONFIG.frameQueueSize,
                value -> {
                    Initializer.CONFIG.frameQueueSize = intValue(value);
                    Renderer.scheduleSwapChainUpdate();
                },
                SettingsDefinitions.FRAME_QUEUE_MIN, SettingsDefinitions.FRAME_QUEUE_MAX,
                SettingsDefinitions.FRAME_QUEUE_STEP)
                .withDefault(() -> SettingsDefinitions.FRAME_QUEUE_DEFAULT));
    }

    private void bindQualityGeneral() {
        bindings.put(SettingsDefinitions.GRAPHICS_MODE, SettingBinding.choosing(
                () -> GraphicsModeCompatibility.coerce(
                        Minecraft.getInstance().options.graphicsMode().get()).getKey(),
                value -> Minecraft.getInstance().options.graphicsMode()
                        .set(GraphicsModeCompatibility.coerce(graphicsModeFor(label(value)))),
                () -> Arrays.stream(GraphicsModeCompatibility.supportedModes())
                        .map(GraphicsStatus::getKey).toList())
                .withDefault(() -> GraphicsStatus.FAST.getKey()));
    }

    private void bindQualityTextures() {
        bindings.put(SettingsDefinitions.MIPMAP_LEVELS, SettingBinding.choosing(
                () -> String.valueOf(Minecraft.getInstance().options.mipmapLevels().get()),
                value -> {
                    int level = mipmapLevelFor(label(value));
                    Minecraft minecraft = Minecraft.getInstance();
                    minecraft.options.mipmapLevels().set(level);
                    minecraft.updateMaxMipLevel(level);
                    minecraft.delayTextureReload();
                },
                () -> MIPMAP_LEVELS.stream().map(String::valueOf).toList())
                .withDefault(() -> String.valueOf(MIPMAP_LEVELS_DEFAULT)));

        bindings.put(SettingsDefinitions.TEXTURE_ANIMATIONS, SettingBinding.of(
                () -> Initializer.CONFIG.textureAnimations,
                value -> Initializer.CONFIG.textureAnimations = boolValue(value))
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.CONNECTED_TEXTURES, SettingBinding.of(
                () -> Initializer.CONFIG.ctmEnabled,
                value -> {
                    Initializer.CONFIG.ctmEnabled = boolValue(value);
                    Minecraft.getInstance().levelRenderer.allChanged();
                })
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.CUSTOM_ITEM_TEXTURES, SettingBinding.of(
                () -> Initializer.CONFIG.citEnabled,
                value -> Initializer.CONFIG.citEnabled = boolValue(value))
                .withDefault(() -> Boolean.TRUE));
    }

    private void bindQualityLighting() {
        bindings.put(SettingsDefinitions.AMBIENT_OCCLUSION, SettingBinding.choosing(
                () -> ambientOcclusionKey(Initializer.CONFIG.ambientOcclusion),
                value -> {
                    int mode = ambientOcclusionFor(label(value));
                    Minecraft.getInstance().options.ambientOcclusion().set(mode > LightMode.FLAT);
                    Initializer.CONFIG.ambientOcclusion = mode;
                    Minecraft.getInstance().levelRenderer.allChanged();
                },
                () -> AMBIENT_OCCLUSION_MODES.stream().map(SettingsCatalog::ambientOcclusionKey).toList())
                .withDefault(() -> ambientOcclusionKey(LightMode.SMOOTH)));

        bindings.put(SettingsDefinitions.BIOME_BLEND, SettingBinding.ranged(
                () -> Minecraft.getInstance().options.biomeBlendRadius().get(),
                value -> {
                    Minecraft.getInstance().options.biomeBlendRadius().set(intValue(value));
                    Minecraft.getInstance().levelRenderer.allChanged();
                },
                SettingsDefinitions.BIOME_BLEND_MIN, SettingsDefinitions.BIOME_BLEND_MAX,
                SettingsDefinitions.BIOME_BLEND_STEP)
                .withDefault(() -> SettingsDefinitions.BIOME_BLEND_DEFAULT)
                .withFormatter(SettingsCatalog::biomeBlendLabel));
    }

    private void bindQualityEnvironment() {
        bindings.put(SettingsDefinitions.CLOUDS, SettingBinding.choosing(
                () -> Minecraft.getInstance().options.cloudStatus().get().getKey(),
                value -> Minecraft.getInstance().options.cloudStatus().set(cloudStatusFor(label(value))),
                () -> Arrays.stream(CloudStatus.values()).map(CloudStatus::getKey).toList())
                .withDefault(() -> CloudStatus.FANCY.getKey()));
    }

    private void bindQualityParticles() {
        bindings.put(SettingsDefinitions.PARTICLES, SettingBinding.choosing(
                () -> Minecraft.getInstance().options.particles().get().getKey(),
                value -> Minecraft.getInstance().options.particles().set(particleStatusFor(label(value))),
                () -> particleLevels().stream().map(ParticleStatus::getKey).toList())
                .withDefault(() -> ParticleStatus.ALL.getKey()));
    }

    private void bindQualityEntities() {
        bindings.put(SettingsDefinitions.ENTITY_SHADOWS, SettingBinding.of(
                () -> Minecraft.getInstance().options.entityShadows().get(),
                value -> Minecraft.getInstance().options.entityShadows().set(boolValue(value)))
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.ENTITY_DISTANCE, SettingBinding.scaled(
                () -> Minecraft.getInstance().options.entityDistanceScaling().get(),
                value -> Minecraft.getInstance().options.entityDistanceScaling().set(doubleValue(value)),
                SettingsDefinitions.ENTITY_DISTANCE_MIN, SettingsDefinitions.ENTITY_DISTANCE_MAX,
                SettingsDefinitions.ENTITY_DISTANCE_STEP, SettingsDefinitions.PERCENT_SCALE)
                .withDefault(() -> SettingsDefinitions.ENTITY_DISTANCE_DEFAULT)
                .withFormatter(SettingsCatalog::percentLabel));
    }

    private void bindAdvancedRenderer() {
        bindings.put(SettingsDefinitions.GPU_DEVICE, SettingBinding.choosing(
                () -> deviceLabel(Initializer.CONFIG.device),
                value -> Initializer.CONFIG.device = deviceFor(label(value)),
                SettingsCatalog::deviceChoices)
                .withDefault(() -> deviceLabel(DEVICE_AUTO)));
    }

    private void bindAdvancedSynchronization() {
        bindings.put(SettingsDefinitions.MOLTENVK_AGGRESSIVE, SettingBinding.of(
                () -> Initializer.CONFIG.moltenvkAggressive,
                value -> Initializer.CONFIG.moltenvkAggressive = boolValue(value))
                .withDefault(() -> Boolean.FALSE));
    }

    private void bindAdvancedCompatibility() {
        bindings.put(SettingsDefinitions.EXTERNAL_LOD, SettingBinding.choosing(
                () -> externalLodKey(configuredExternalLodMode()),
                value -> Initializer.CONFIG.externalLod = externalLodFor(label(value)),
                () -> EXTERNAL_LOD_MODES.stream().map(SettingsCatalog::externalLodKey).toList())
                .withDefault(() -> externalLodKey(ExternalRenderPathSupport.Mode.OFF)));

        bindings.put(SettingsDefinitions.EXTERNAL_LOD_DRAW, SettingBinding.of(
                () -> Initializer.CONFIG.externalLodDraw,
                value -> Initializer.CONFIG.externalLodDraw = boolValue(value))
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.GL_LEGACY_BRIDGE, SettingBinding.of(
                () -> Initializer.CONFIG.glLegacyBridge,
                value -> Initializer.CONFIG.glLegacyBridge = boolValue(value))
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.GL_FBO_VIEWPORT, SettingBinding.of(
                () -> Initializer.CONFIG.glFboViewport,
                value -> Initializer.CONFIG.glFboViewport = boolValue(value))
                .withDefault(() -> Boolean.TRUE));
    }

    private static List<Device> suitableDevices() {
        List<Device> devices = DeviceManager.suitableDevices;
        return devices == null ? List.of() : devices;
    }

    private static List<String> deviceChoices() {
        List<String> choices = new ArrayList<>();
        choices.add(deviceLabel(DEVICE_AUTO));
        for (int index = 0; index < suitableDevices().size(); index++) {
            choices.add(deviceLabel(index));
        }
        return choices;
    }

    private static String deviceLabel(int index) {
        List<Device> devices = suitableDevices();
        if (index < 0 || index >= devices.size()) {
            return "vulkanmod.options.deviceSelector.auto";
        }
        return devices.get(index).deviceName;
    }

    private static int deviceFor(String label) {
        if (deviceLabel(DEVICE_AUTO).equals(label)) {
            return DEVICE_AUTO;
        }
        for (int index = 0; index < suitableDevices().size(); index++) {
            if (deviceLabel(index).equals(label)) {
                return index;
            }
        }
        throw new IllegalArgumentException("unknown graphics device " + label);
    }

    private static ExternalRenderPathSupport.Mode configuredExternalLodMode() {
        return ExternalRenderPathSupport.Mode.fromProperty(ExternalRenderPathOptions.externalLodMode());
    }

    private static String externalLodKey(ExternalRenderPathSupport.Mode mode) {
        return switch (mode) {
            case SAFE -> "vulkanmod.options.externalLod.safe";
            case EXPERIMENTAL -> "vulkanmod.options.externalLod.experimental";
            default -> "vulkanmod.options.externalLod.off";
        };
    }

    private static String externalLodFor(String key) {
        for (ExternalRenderPathSupport.Mode mode : EXTERNAL_LOD_MODES) {
            if (externalLodKey(mode).equals(key)) {
                return mode.name().toLowerCase(Locale.ROOT);
            }
        }
        throw new IllegalArgumentException("unknown external LOD bridge mode " + key);
    }

    private static GraphicsStatus graphicsModeFor(String key) {
        for (GraphicsStatus mode : GraphicsModeCompatibility.supportedModes()) {
            if (mode.getKey().equals(key)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("unknown graphics mode " + key);
    }

    private static int mipmapLevelFor(String label) {
        for (int level : MIPMAP_LEVELS) {
            if (String.valueOf(level).equals(label)) {
                return level;
            }
        }
        throw new IllegalArgumentException("unknown mipmap level " + label);
    }

    private static String ambientOcclusionKey(int mode) {
        return switch (mode) {
            case LightMode.FLAT -> "options.off";
            case LightMode.SUB_BLOCK -> "vulkanmod.options.ao.subBlock";
            default -> "options.on";
        };
    }

    private static int ambientOcclusionFor(String key) {
        for (int mode : AMBIENT_OCCLUSION_MODES) {
            if (ambientOcclusionKey(mode).equals(key)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("unknown ambient occlusion mode " + key);
    }

    private static CloudStatus cloudStatusFor(String key) {
        for (CloudStatus status : CloudStatus.values()) {
            if (status.getKey().equals(key)) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown cloud mode " + key);
    }

    private static List<ParticleStatus> particleLevels() {
        return List.of(ParticleStatus.MINIMAL, ParticleStatus.DECREASED, ParticleStatus.ALL);
    }

    private static ParticleStatus particleStatusFor(String key) {
        for (ParticleStatus status : particleLevels()) {
            if (status.getKey().equals(key)) {
                return status;
            }
        }
        throw new IllegalArgumentException("unknown particle level " + key);
    }

    private static String biomeBlendLabel(Object value) {
        int size = intValue(value) * 2 + 1;
        return size + " x " + size;
    }

    private static List<PerformancePreset> performanceProfiles() {
        return List.of(PerformancePreset.POTATO, PerformancePreset.BALANCED,
                PerformancePreset.QUALITY, PerformancePreset.CUSTOM);
    }

    private static PerformancePreset performanceProfileFor(String key) {
        for (PerformancePreset preset : performanceProfiles()) {
            if (preset.translationKey.equals(key)) {
                return preset;
            }
        }
        throw new IllegalArgumentException("unknown performance profile " + key);
    }

    private static PrioritizeChunkUpdates chunkUpdatePriorityFor(String key) {
        for (PrioritizeChunkUpdates priority : PrioritizeChunkUpdates.values()) {
            if (priority.getKey().equals(key)) {
                return priority;
            }
        }
        throw new IllegalArgumentException("unknown chunk update priority " + key);
    }

    private static VsrPreset vsrPresetFor(String key) {
        for (VsrPreset preset : VsrPreset.values()) {
            if (preset.translationKey.equals(key)) {
                return preset;
            }
        }
        throw new IllegalArgumentException("unknown resolution scaling preset " + key);
    }

    private static String upscalerKey(int backend) {
        return switch (backend) {
            case Vsr.FSR1 -> "vulkanmod.options.vsrBackend.fsr1";
            case Vsr.SHARPEN_ONLY -> "vulkanmod.options.vsrBackend.sharpen";
            case Vsr.VTU -> "vulkanmod.options.vsrBackend.vtu";
            default -> "vulkanmod.options.vsrBackend.bilinear";
        };
    }

    private static int upscalerFor(String key) {
        for (int backend : UPSCALERS) {
            if (upscalerKey(backend).equals(key)) {
                return backend;
            }
        }
        throw new IllegalArgumentException("unknown upscaler " + key);
    }

    private static String cullingModeKey(int mode) {
        return switch (mode) {
            case CULLING_MODE_AGGRESSIVE -> "vulkanmod.options.advCulling.aggressive";
            case CULLING_MODE_CONSERVATIVE -> "vulkanmod.options.advCulling.conservative";
            default -> "vulkanmod.options.advCulling.normal";
        };
    }

    private static int cullingModeFor(String key) {
        for (int mode : CULLING_MODES) {
            if (cullingModeKey(mode).equals(key)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("unknown culling strategy " + key);
    }

    private static String particleCullingKey(int level) {
        return switch (level) {
            case PARTICLE_CULLING_OFF -> "options.off";
            case PARTICLE_CULLING_QUALITY -> "vulkanmod.options.particleCulling.quality";
            case PARTICLE_CULLING_PERFORMANCE -> "vulkanmod.options.particleCulling.performance";
            default -> "vulkanmod.options.particleCulling.balanced";
        };
    }

    private static int particleCullingFor(String key) {
        for (int level : PARTICLE_CULLING_LEVELS) {
            if (particleCullingKey(level).equals(key)) {
                return level;
            }
        }
        throw new IllegalArgumentException("unknown particle culling level " + key);
    }

    private static int maxGuiScale() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getWindow().calculateScale(0, minecraft.isEnforceUnicode());
    }

    private static String guiScaleLabel(Object value) {
        int scale = intValue(value);
        return scale == SettingsDefinitions.GUI_SCALE_AUTO
                ? I18n.get("options.guiScale.auto")
                : String.valueOf(scale);
    }

    private static String brightnessLabel(Object value) {
        return switch (intValue(value)) {
            case SettingsDefinitions.BRIGHTNESS_MIN -> I18n.get("options.gamma.min");
            case SettingsDefinitions.BRIGHTNESS_DEFAULT -> I18n.get("options.gamma.default");
            case SettingsDefinitions.BRIGHTNESS_MAX -> I18n.get("options.gamma.max");
            default -> String.valueOf(intValue(value));
        };
    }

    private static String percentLabel(Object value) {
        int percent = intValue(value);
        return percent == SettingsDefinitions.EFFECT_SCALE_MIN
                ? I18n.get("options.off")
                : I18n.get("options.percent_value", percent);
    }

    private static String framerateLimitLabel(Object value) {
        int limit = intValue(value);
        return limit >= SettingsDefinitions.FRAMERATE_LIMIT_MAX
                ? I18n.get("options.framerateLimit.max")
                : String.valueOf(limit);
    }

    private static int highestRefreshRate() {
        List<Integer> rates = selectedResolution().getRefreshRates();
        return rates.get(rates.size() - 1);
    }

    private Boolean lodCullingAvailable;

    private boolean lodCullingAvailable() {
        if (lodCullingAvailable == null) {
            lodCullingAvailable = LodCulling.isAvailable();
        }
        return lodCullingAvailable;
    }

    private static WindowMode windowMode() {
        if (Minecraft.getInstance().options.fullscreen().get()) {
            return WindowMode.EXCLUSIVE_FULLSCREEN;
        }
        return Initializer.CONFIG.windowedFullscreen ? WindowMode.WINDOWED_FULLSCREEN : WindowMode.WINDOWED;
    }

    private static WindowMode windowModeFor(String label) {
        for (WindowMode mode : WindowMode.values()) {
            if (WindowMode.getComponentName(mode).equals(label)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("unknown window mode " + label);
    }

    private static VideoModeSet resolutionFor(String label) {
        for (VideoModeSet resolution : VideoModeManager.getVideoResolutions()) {
            if (resolution.toString().equals(label)) {
                return resolution;
            }
        }
        throw new IllegalArgumentException("unknown resolution " + label);
    }

    private static VideoModeSet.VideoMode selectedVideoMode() {
        VideoModeSet.VideoMode selected = VideoModeManager.selectedVideoMode;
        if (selected != null) {
            return selected;
        }
        VideoModeSet.VideoMode configured = Initializer.CONFIG.videoMode;
        return configured != null ? configured : VideoModeManager.getFirstAvailable().getVideoMode();
    }

    private static VideoModeSet selectedResolution() {
        VideoModeSet resolution = VideoModeManager.getFromVideoMode(selectedVideoMode());
        return resolution != null ? resolution : VideoModeSet.getDummy();
    }

    private static void applyVideoMode() {
        VideoModeManager.applySelectedVideoMode();
        if (Minecraft.getInstance().options.fullscreen().get()) {
            Options.fullscreenDirty = true;
        }
    }

    private static String label(Object value) {
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException("expected a choice label, got " + value);
        }
        return text;
    }

    private static boolean boolValue(Object value) {
        if (!(value instanceof Boolean flag)) {
            throw new IllegalArgumentException("expected a boolean, got " + value);
        }
        return flag;
    }

    private static int intValue(Object value) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("expected a number, got " + value);
        }
        return number.intValue();
    }

    private static double doubleValue(Object value) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("expected a number, got " + value);
        }
        return number.doubleValue();
    }
}
