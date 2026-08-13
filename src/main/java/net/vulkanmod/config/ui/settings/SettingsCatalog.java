package net.vulkanmod.config.ui.settings;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.vulkanmod.config.plugin.PluginTargetType;
import net.vulkanmod.gui.debug.DebugOverlay;
import net.vulkanmod.gui.HudHandler;
import net.vulkanmod.plugin.PluginEntry;
import net.vulkanmod.plugin.PluginRegistry;
import org.lwjgl.glfw.GLFW;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.particle.ParticleToggles;
import net.vulkanmod.vulkan.MoltenVKConfig;
import net.vulkanmod.compat.opengl.GlDrawOptions;
import net.vulkanmod.api.LodCulling;
import net.vulkanmod.compat.capabilities.ExternalRenderPathOptions;
import net.vulkanmod.compat.external.ExternalRenderPathSupport;
import net.vulkanmod.config.GraphicsModeCompatibility;
import net.vulkanmod.config.PerformancePreset;
import net.vulkanmod.config.Platform;
import net.vulkanmod.config.RenderScale;
import net.vulkanmod.config.VsrPreset;
import net.vulkanmod.render.chunk.build.TaskDispatcher;
import net.vulkanmod.render.chunk.build.light.LightMode;
import net.vulkanmod.render.vsr.Vsr;
import net.vulkanmod.config.ui.core.FrameSamples;
import net.vulkanmod.vulkan.SessionSamples;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.Device;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.pass.DefaultMainPass;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingRegistry;
import net.vulkanmod.config.ui.mods.ModConfigReader;
import net.vulkanmod.config.ui.mods.ModOptIn;
import net.vulkanmod.config.ui.mods.ModSettings;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.config.video.VideoModeSet;
import net.vulkanmod.config.video.WindowMode;
import net.vulkanmod.render.sodium.SodiumShaderBridge;

import java.util.*;
import java.util.function.Supplier;

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
    private static final List<Integer> ANISOTROPY_LEVELS = List.of(1, 2, 4, 8, 16);
    private static final int MIPMAP_LEVELS_DEFAULT = 4;

    private static final List<Integer> AMBIENT_OCCLUSION_MODES =
            List.of(LightMode.FLAT, LightMode.SMOOTH, LightMode.SUB_BLOCK);

    private static final int DEVICE_AUTO = -1;

    private static final List<ExternalRenderPathSupport.Mode> EXTERNAL_LOD_MODES =
            List.of(ExternalRenderPathSupport.Mode.OFF, ExternalRenderPathSupport.Mode.SAFE,
                    ExternalRenderPathSupport.Mode.EXPERIMENTAL);

    private final SettingRegistry registry = new SettingRegistry();
    private final Map<SettingId, SettingBinding> bindings = new LinkedHashMap<>();
    private final List<String> modIds = new ArrayList<>();
    private final List<String> pluginIds = new ArrayList<>();

    public SettingsCatalog() {
        bindDisplayGeneral();
        bindDisplayInterface();
        bindDisplayAdvanced();
        bindDisplayVolcanic();
        bindRenderingGeneral();
        bindRenderingResolution();
        bindRenderingCulling();
        bindPerformanceGeneral();
        bindPerformanceGpu();
        bindPerformanceChunks();
        bindPerformanceSynchronization();
        bindQualityExtras();
        bindQualityGeneral();
        bindQualityTextures();
        bindGlint();
        bindQualityLighting();
        bindQualityEnvironment();
        bindEnvironmentWind();
        bindRenderingAdvanced();
        bindRenderingEntities();
        bindDeveloperTools();
        bindAdvancedRenderer();
        bindAdvancedSynchronization();
        bindAdvancedCompatibility();
        bindExperimental();
        bindShadersCurrent();

        registerAll(SettingsDefinitions.displayGeneral());
        registerAll(SettingsDefinitions.displayInterface());
        registerAll(SettingsDefinitions.displayVolcanic());
        registerAll(SettingsDefinitions.renderingGeneral());
        registerAll(SettingsDefinitions.performanceResolution());
        registerAll(SettingsDefinitions.renderingCulling());
        registerAll(SettingsDefinitions.performanceGpu());
        registerAll(SettingsDefinitions.performanceChunks());
        registerAll(SettingsDefinitions.qualityTextures());
        registerAll(SettingsDefinitions.qualityLighting());
        registerAll(SettingsDefinitions.qualityEnvironment());
        registerAll(SettingsDefinitions.renderingAdvanced());
        registerAll(SettingsDefinitions.renderingEntities());
        registerAll(SettingsDefinitions.advancedRenderer());
        registerAll(SettingsDefinitions.advancedCompatibility());
        registerAll(SettingsDefinitions.shadersCurrent());
        registerAll(SettingsDefinitions.developerTools());
        registerParticles();

        registerPlugins();
        registerMods();
    }

    private boolean batching;
    private boolean rebuildWanted;

    private void requestChunkRebuild() {
        if (batching) {
            this.rebuildWanted = true;
            return;
        }
        Minecraft.getInstance().levelRenderer.allChanged();
    }

    public void beginBatch() {
        this.batching = true;
        this.rebuildWanted = false;
        net.vulkanmod.config.ui.mods.ModConfigReader.beginStaging();
    }

    public void endBatch() {
        this.batching = false;
        net.vulkanmod.config.ui.mods.ModConfigReader.flush();
        if (rebuildWanted) {
            this.rebuildWanted = false;
            Minecraft.getInstance().levelRenderer.allChanged();
        }
    }

    private void registerParticles() {
        List<ResourceLocation> ids;
        try {
            ids = ParticleToggles.ids();
        } catch (Throwable unavailable) {
            return;
        }
        for (ResourceLocation id : ids) {
            SettingMeta meta = SettingsDefinitions.particleToggle(id.toString(), particleLabel(id));
            registry.register(meta);
            bindings.put(meta.id(), SettingBinding.of(
                    () -> ParticleToggles.enabled(id),
                    value -> ParticleToggles.setEnabled(id, boolValue(value)))
                    .withDefault(() -> Boolean.TRUE));
        }
    }

    private static String particleLabel(ResourceLocation id) {
        StringBuilder out = new StringBuilder();
        for (String word : id.getPath().split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        String name = out.isEmpty() ? id.getPath() : out.toString();
        return "minecraft".equals(id.getNamespace()) ? name : name + " (" + id.getNamespace() + ")";
    }

    private void registerAll(List<SettingMeta> definitions) {
        for (SettingMeta meta : definitions) {
            registry.register(meta);
            if (!bindings.containsKey(meta.id())) {
                throw new IllegalStateException("no binding for setting id " + meta.id());
            }
        }
    }

    private void registerPlugins() {
        for (PluginEntry plugin : MenuPlugins.discover()) {
            try {
                PluginSettings.Converted converted =
                        PluginSettings.convert(plugin.getPlugin().id(), MenuPlugins.settingsOf(plugin));
                for (SettingMeta meta : converted.metas()) {
                    if (registry.contains(meta.id())) {
                        throw new IllegalArgumentException("duplicate setting id " + meta.id());
                    }
                }
                converted.metas().forEach(registry::register);

                bindings.putAll(converted.bindings());
                pluginIds.add(plugin.getPlugin().id());
            } catch (RuntimeException failure) {
                Initializer.LOGGER.warn("Left plugin {} out of the menu: {}",
                        MenuPlugins.displayNameOf(plugin), failure.toString());
            }
        }
    }

    public List<String> pluginIds() {
        return List.copyOf(pluginIds);
    }

    private void registerMods() {
        Set<String> optedIn = new LinkedHashSet<>(pluginIds);
        for (ModSettings mod : read(ModOptIn::readAll)) {
            if (optedIn.add(mod.modId())) {
                registerGuarded(mod);
            }
        }

        for (ModSettings mod : read(ModConfigReader::readAll)) {
            if (!optedIn.contains(mod.modId())) {
                registerGuarded(mod);
            }
        }
    }

    private static List<ModSettings> read(Supplier<List<ModSettings>> source) {
        try {
            return source.get();
        } catch (Throwable t) {
            return List.of();
        }
    }

    private void registerGuarded(ModSettings mod) {
        try {
            registerMod(mod);
        } catch (RuntimeException e) {
            Initializer.LOGGER.warn("Left {} out of the mods list: its settings could not be registered",
                    mod.modId(), e);
        }
    }

    private void registerMod(ModSettings mod) {
        Set<SettingId> ids = new LinkedHashSet<>();
        for (SettingMeta meta : mod.metas()) {
            if (registry.contains(meta.id()) || !ids.add(meta.id())) {
                throw new IllegalArgumentException("duplicate setting id " + meta.id());
            }
            if (!mod.bindings().containsKey(meta.id())) {
                throw new IllegalArgumentException("no binding for setting id " + meta.id());
            }
        }
        for (SettingMeta meta : mod.metas()) {
            registry.register(meta);
        }
        bindings.putAll(mod.bindings());
        modIds.add(mod.modId());
    }

    public SettingRegistry registry() {
        return registry;
    }

    public List<String> modIds() {
        return List.copyOf(modIds);
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

    private static boolean anisotropySupported() {
        try {
            return DeviceManager.device != null
                    && DeviceManager.device.availableFeatures.features().samplerAnisotropy()
                    && DeviceManager.deviceProperties.limits().maxSamplerAnisotropy() >= 2.0f;
        } catch (Throwable failure) {
            return false;
        }
    }

    public boolean enabled(SettingId id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (!bindings.containsKey(id)) {
            throw new IllegalArgumentException("no binding for setting id " + id);
        }
        if (SettingsDefinitions.SHADERS_SELECTED_PACK.equals(id)) {
            return !SodiumShaderBridge.isActive();
        }
        if (SettingsDefinitions.RESOLUTION.equals(id) || SettingsDefinitions.REFRESH_RATE.equals(id)) {
            return windowMode() == WindowMode.EXCLUSIVE_FULLSCREEN;
        }
        if (SettingsDefinitions.DISABLE_HIDPI.equals(id)) {
            return Platform.isMacOS();
        }
        if (SettingsDefinitions.VULKAN_VALIDATION.equals(id)) {
            return net.vulkanmod.vulkan.MoltenVKConfig.vulkanSdkPresent();
        }
        if (SettingsDefinitions.ANISOTROPIC_FILTERING.equals(id)) {
            return anisotropySupported();
        }
        if (SettingsDefinitions.DYNAMIC_LIGHT.equals(id)) {
            return false;
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
            return Platform.isMacOS() && !overriddenByLaunchFlag(id);
        }
        if (SettingsDefinitions.EXTERNAL_LOD_DRAW.equals(id)) {
            return configuredExternalLodMode() != ExternalRenderPathSupport.Mode.OFF
                    && !overriddenByLaunchFlag(id);
        }
        return !overriddenByLaunchFlag(id);
    }

    static boolean overriddenByLaunchFlag(SettingId id) {
        if (SettingsDefinitions.MOLTENVK_AGGRESSIVE.equals(id)) {
            return MoltenVKConfig.aggressiveOverridden();
        }
        if (SettingsDefinitions.EXTERNAL_LOD.equals(id)) {
            return ExternalRenderPathOptions.externalLodOverridden();
        }
        if (SettingsDefinitions.EXTERNAL_LOD_DRAW.equals(id)) {
            return ExternalRenderPathOptions.externalLodDrawOverridden();
        }
        if (SettingsDefinitions.GL_LEGACY_BRIDGE.equals(id)) {
            return GlDrawOptions.legacyBridgeOverridden();
        }
        if (SettingsDefinitions.GL_FBO_VIEWPORT.equals(id)) {
            return GlDrawOptions.fboViewportOverridden();
        }
        return false;
    }

    public Optional<String> disabledReason(SettingId id) {
        if (enabled(id)) {
            return Optional.empty();
        }
        Optional<String> declared = SettingsDefinitions.disabledReasonKey(id);
        if (declared.isPresent()) {
            return declared;
        }
        if (SettingsDefinitions.DYNAMIC_LIGHT.equals(id)) {
            return Optional.of(SettingsDefinitions.REASON_NOT_YET);
        }
        if ((SettingsDefinitions.MOLTENVK_AGGRESSIVE.equals(id)
                || SettingsDefinitions.DISABLE_HIDPI.equals(id)) && !Platform.isMacOS()) {
            return Optional.of(SettingsDefinitions.REASON_MACOS_ONLY);
        }
        return overriddenByLaunchFlag(id)
                ? Optional.of(SettingsDefinitions.REASON_LAUNCH_FLAG)
                : Optional.empty();
    }

    public Map<String, Map<SettingId, Object>> profileValues() {
        Map<String, Map<SettingId, Object>> profiles = new LinkedHashMap<>();
        for (PerformancePreset preset : derivableProfiles()) {
            profiles.put(preset.translationKey, presetValues(preset));
        }
        return profiles;
    }

    public Map<SettingId, Object> currentProfileValues() {
        Map<SettingId, Object> values = new LinkedHashMap<>();
        for (Map<SettingId, Object> governed : profileValues().values()) {
            for (SettingId id : governed.keySet()) {
                if (!values.containsKey(id)) {
                    values.put(id, binding(id).get());
                }
            }
        }
        return values;
    }

    private static Map<SettingId, Object> presetValues(PerformancePreset preset) {
        Map<SettingId, Object> values = new LinkedHashMap<>();
        values.put(SettingsDefinitions.INDIRECT_DRAW,
                preset.indirectDraw && DeviceManager.supportsFastIndirectDraw());
        values.put(SettingsDefinitions.UNIQUE_OPAQUE_LAYER, preset.uniqueOpaqueLayer);
        values.put(SettingsDefinitions.ADAPTIVE_CHUNK_UPLOADS, preset.adaptiveChunkUploads);
        values.put(SettingsDefinitions.CHUNK_UPLOADS_PER_FRAME,
                TaskDispatcher.clampMaxUploadsPerFrame(preset.chunkUploadsPerFrame));
        values.put(SettingsDefinitions.CULLING_MODE, cullingModeKey(preset.advCulling));
        values.put(SettingsDefinitions.CULLING_BLOCK_ENTITIES, preset.blockEntityCulling);
        values.put(SettingsDefinitions.CULLING_PARTICLES, particleCullingKey(preset.particleCulling));
        values.put(SettingsDefinitions.RENDER_DISTANCE, preset.renderDistance);
        values.put(SettingsDefinitions.SIMULATION_DISTANCE, preset.simulationDistance);
        values.put(SettingsDefinitions.GRAPHICS_MODE,
                GraphicsModeCompatibility.coerce(preset.graphicsStatus).getKey());
        values.put(SettingsDefinitions.MIPMAP_LEVELS, String.valueOf(preset.mipmapLevels));
        values.put(SettingsDefinitions.AMBIENT_OCCLUSION, ambientOcclusionKey(preset.ambientOcclusion));
        values.put(SettingsDefinitions.BIOME_BLEND, preset.biomeBlendRadius);
        values.put(SettingsDefinitions.CLOUDS, preset.cloudStatus.getKey());
        values.put(SettingsDefinitions.PARTICLES, preset.particleStatus.getKey());
        values.put(SettingsDefinitions.ENTITY_SHADOWS, preset.entityShadows);
        values.put(SettingsDefinitions.ENTITY_DISTANCE, preset.entityDistancePercent);
        return values;
    }

    private static List<PerformancePreset> derivableProfiles() {
        return performanceProfiles().stream()
                .filter(preset -> preset != PerformancePreset.CUSTOM)
                .toList();
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
        if (!DefaultMainPass.postShaderActiveStatic()) {
            return Optional.empty();
        }
        String selected = Initializer.CONFIG.selectedShader;
        String key = "vulkanmod.options.shader." + selected;
        return Optional.of(I18n.exists(key) ? I18n.get(key) : selected);
    }

    private static Optional<String> measuredFrameTime() {
        FrameSamples samples = SessionSamples.samples();
        if (!samples.ready()) {
            return Optional.of(I18n.get("vulkanmod.ui.overview.sampling", samples.count(), FrameSamples.READY_AT));
        }
        return Optional.of(String.format(Locale.ROOT, "%.1f ms  ·  %.0f fps", samples.median(), samples.fps()));
    }

    private static Optional<String> measuredStability() {
        FrameSamples samples = SessionSamples.samples();
        if (!samples.ready()) {
            return Optional.empty();
        }
        if (!samples.hasLowPercentile()) {
            return Optional.of(String.format(Locale.ROOT, "P95 %.1f ms", samples.p95()));
        }
        return Optional.of(String.format(Locale.ROOT, "P95 %.1f ms  ·  P1 %.1f ms", samples.p95(), samples.p1()));
    }

    private static Optional<String> measuredFrom() {
        FrameSamples samples = SessionSamples.samples();
        return samples.ready()
                ? Optional.of(I18n.get("vulkanmod.ui.overview.from_frames", samples.count()))
                : Optional.empty();
    }

    private void bindDisplayGeneral() {
        bindings.put(SettingsDefinitions.WINDOW_MODE, SettingBinding.choosing(
                () -> WindowMode.getComponentName(windowMode()),
                value -> {
                    WindowMode mode = windowModeFor(label(value));
                    Minecraft.getInstance().options.fullscreen().set(mode == WindowMode.EXCLUSIVE_FULLSCREEN);
                    Initializer.CONFIG.windowedFullscreen = mode == WindowMode.WINDOWED_FULLSCREEN;
                    VideoModeManager.fullscreenDirty = true;
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
                    selectedVideoMode().refreshRate = resolution.hasRefreshRate(refreshRate)
                            ? refreshRate
                            : resolution.getVideoMode().refreshRate;
                    applyVideoMode();
                },
                () -> selectedResolution().getRefreshRates().stream().map(String::valueOf).toList())
                .withDefault(() -> String.valueOf(highestRefreshRate())));

        bindings.put(SettingsDefinitions.VSYNC, SettingBinding.choosing(
                SettingsCatalog::vsyncChoice,
                value -> {
                    String choice = label(value);
                    boolean adaptive = SettingsDefinitions.VSYNC_ADAPTIVE.equals(choice);
                    boolean enabled = adaptive || SettingsDefinitions.VSYNC_ON.equals(choice);
                    Minecraft.getInstance().options.enableVsync().set(enabled);
                    Minecraft.getInstance().getWindow().updateVsync(enabled);
                    Initializer.CONFIG.adaptiveVsync = adaptive;
                    Renderer.scheduleSwapChainUpdate();
                },
                SettingsCatalog::vsyncChoices)
                .withDefault(() -> SettingsDefinitions.VSYNC_ON));

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

        bindings.put(SettingsDefinitions.FOV_EFFECTS, SettingBinding.scaled(
                () -> Minecraft.getInstance().options.fovEffectScale().get(),
                value -> Minecraft.getInstance().options.fovEffectScale().set(doubleValue(value)),
                SettingsDefinitions.EFFECT_SCALE_MIN, SettingsDefinitions.EFFECT_SCALE_MAX,
                SettingsDefinitions.EFFECT_SCALE_STEP, SettingsDefinitions.PERCENT_SCALE)
                .withDefault(() -> SettingsDefinitions.EFFECT_SCALE_DEFAULT)
                .withFormatter(SettingsCatalog::percentLabel));

        bindings.put(SettingsDefinitions.VIEW_BOBBING, SettingBinding.of(
                () -> Minecraft.getInstance().options.bobView().get(),
                value -> Minecraft.getInstance().options.bobView().set(boolValue(value)))
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.DAMAGE_TILT, SettingBinding.scaled(
                () -> Minecraft.getInstance().options.damageTiltStrength().get(),
                value -> Minecraft.getInstance().options.damageTiltStrength().set(doubleValue(value)),
                SettingsDefinitions.EFFECT_SCALE_MIN, SettingsDefinitions.EFFECT_SCALE_MAX,
                SettingsDefinitions.EFFECT_SCALE_STEP, SettingsDefinitions.PERCENT_SCALE)
                .withDefault(() -> SettingsDefinitions.EFFECT_SCALE_MAX)
                .withFormatter(SettingsCatalog::percentLabel));
    }

    private void bindDisplayAdvanced() {
        bindings.put(SettingsDefinitions.DISABLE_HIDPI, SettingBinding.of(
                () -> Initializer.CONFIG.disableHiDPI,
                value -> Initializer.CONFIG.disableHiDPI = boolValue(value))
                .withDefault(() -> Boolean.FALSE));
    }

    private void bindDisplayVolcanic() {
        bindings.put(SettingsDefinitions.UI_ANIMATIONS, SettingBinding.of(
                () -> Initializer.CONFIG.uiAnimations,
                value -> Initializer.CONFIG.uiAnimations = boolValue(value))
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.BACKGROUND_ANIMATION, SettingBinding.of(
                () -> Initializer.CONFIG.backgroundAnimation,
                value -> Initializer.CONFIG.backgroundAnimation = boolValue(value))
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.UI_SOUNDS, SettingBinding.of(
                () -> Initializer.CONFIG.uiSounds,
                value -> Initializer.CONFIG.uiSounds = boolValue(value))
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.LOADING_SCREEN, SettingBinding.of(
                () -> Initializer.CONFIG.loadingScreen,
                value -> Initializer.CONFIG.loadingScreen = boolValue(value))
                .withDefault(() -> Boolean.TRUE));
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

        bindings.put(SettingsDefinitions.BLOCK_ENTITY_DISTANCE, SettingBinding.ranged(
                () -> Initializer.CONFIG.blockEntityDistance,
                value -> Initializer.CONFIG.blockEntityDistance = intValue(value),
                SettingsDefinitions.BLOCK_ENTITY_DISTANCE_MIN,
                SettingsDefinitions.BLOCK_ENTITY_DISTANCE_MAX,
                SettingsDefinitions.BLOCK_ENTITY_DISTANCE_STEP)
                .withDefault(() -> SettingsDefinitions.BLOCK_ENTITY_DISTANCE_DEFAULT)
                .withFormatter(SettingsCatalog::blockEntityDistanceLabel));

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
                    requestChunkRebuild();
                })
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.CULLING_MODE, SettingBinding.choosing(
                () -> cullingModeKey(Initializer.CONFIG.advCulling),
                value -> {
                    Initializer.CONFIG.advCulling = cullingModeFor(label(value));
                    requestChunkRebuild();
                },
                () -> CULLING_MODES.stream().map(SettingsCatalog::cullingModeKey).toList())
                .withDefault(() -> cullingModeKey(CULLING_MODE_NORMAL)));

        bindings.put(SettingsDefinitions.CULLING_BLOCK_ENTITIES, SettingBinding.of(
                () -> Initializer.CONFIG.blockEntityCulling,
                value -> {
                    Initializer.CONFIG.blockEntityCulling = boolValue(value);
                    requestChunkRebuild();
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
                    requestChunkRebuild();
                })
                .withDefault(() -> Boolean.FALSE));
    }

    private void bindPerformanceGeneral() {
        bindings.put(SettingsDefinitions.BUILDER_THREADS, SettingBinding.ranged(
                () -> Initializer.CONFIG.chunkBuilderThreads,
                value -> Initializer.CONFIG.chunkBuilderThreads = intValue(value),
                SettingsDefinitions.BUILDER_THREADS_AUTO, TaskDispatcher::maxThreadCount, 1)
                .withDefault(() -> SettingsDefinitions.BUILDER_THREADS_AUTO)
                .withFormatter(SettingsCatalog::builderThreadsLabel));

    }

    private void bindPerformanceGpu() {
        bindings.put(SettingsDefinitions.INDIRECT_DRAW, SettingBinding.of(
                () -> Initializer.CONFIG.indirectDraw && DeviceManager.supportsFastIndirectDraw(),
                value -> {
                    Initializer.CONFIG.indirectDraw = boolValue(value) && DeviceManager.supportsFastIndirectDraw();
                    requestChunkRebuild();
                })
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.UNIQUE_OPAQUE_LAYER, SettingBinding.of(
                () -> Initializer.CONFIG.uniqueOpaqueLayer,
                value -> {
                    Initializer.CONFIG.uniqueOpaqueLayer = boolValue(value);
                    requestChunkRebuild();
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

    private static String vsyncChoice() {
        if (!Minecraft.getInstance().options.enableVsync().get()) {
            return SettingsDefinitions.VSYNC_OFF;
        }
        return Initializer.CONFIG.adaptiveVsync && SwapChain.supportsFifoRelaxed()
                ? SettingsDefinitions.VSYNC_ADAPTIVE : SettingsDefinitions.VSYNC_ON;
    }

    private static List<String> vsyncChoices() {
        return SwapChain.supportsFifoRelaxed()
                ? List.of(SettingsDefinitions.VSYNC_OFF, SettingsDefinitions.VSYNC_ON,
                        SettingsDefinitions.VSYNC_ADAPTIVE)
                : List.of(SettingsDefinitions.VSYNC_OFF, SettingsDefinitions.VSYNC_ON);
    }

    private void bindQualityExtras() {
        bindings.put(SettingsDefinitions.LEAVES_QUALITY, SettingBinding.choosing(
                () -> Initializer.CONFIG.leavesQuality > 0
                        ? SettingsDefinitions.LEAVES_FAST : SettingsDefinitions.LEAVES_FANCY,
                value -> Initializer.CONFIG.leavesQuality =
                        SettingsDefinitions.LEAVES_FAST.equals(label(value)) ? 1 : 0,
                () -> List.of(SettingsDefinitions.LEAVES_FANCY, SettingsDefinitions.LEAVES_FAST))
                .withDefault(() -> SettingsDefinitions.LEAVES_FAST));

        bindings.put(SettingsDefinitions.DYNAMIC_LIGHT, SettingBinding.of(
                () -> Initializer.CONFIG.dynamicLight,
                value -> Initializer.CONFIG.dynamicLight = boolValue(value))
                .withDefault(() -> Boolean.FALSE));
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

    private void bindDeveloperTools() {
        bindings.put(SettingsDefinitions.SHOW_FPS, SettingBinding.of(
                () -> Initializer.CONFIG.showFpsCounter,
                value -> {
                    Initializer.CONFIG.showFpsCounter = boolValue(value);
                    unhideOverlay(Initializer.CONFIG.showFpsCounter);
                })
                .withDefault(() -> Boolean.FALSE));

        bindings.put(SettingsDefinitions.SHOW_COORDINATES, SettingBinding.of(
                () -> Initializer.CONFIG.showCoordinates,
                value -> {
                    Initializer.CONFIG.showCoordinates = boolValue(value);
                    unhideOverlay(Initializer.CONFIG.showCoordinates);
                })
                .withDefault(() -> Boolean.FALSE));

        bindings.put(SettingsDefinitions.PERF_LOG, SettingBinding.of(
                () -> Initializer.CONFIG.perfLog,
                value -> {
                    Initializer.CONFIG.perfLog = boolValue(value);
                    net.vulkanmod.vulkan.FrameTimer.setLogging(Initializer.CONFIG.perfLog);
                })
                .withDefault(() -> Boolean.FALSE));

        bindings.put(SettingsDefinitions.STATS_SAMPLE_IN_MENUS, SettingBinding.of(
                () -> Initializer.CONFIG.statsSampleInMenus,
                value -> Initializer.CONFIG.statsSampleInMenus = boolValue(value))
                .withDefault(() -> Boolean.FALSE));

        bindings.put(SettingsDefinitions.DEBUG_OVERLAY, SettingBinding.of(
                () -> Minecraft.getInstance().getDebugOverlay().showDebugScreen(),
                value -> {
                    if (boolValue(value) != Minecraft.getInstance().getDebugOverlay().showDebugScreen()) {
                        Minecraft.getInstance().getDebugOverlay().toggleOverlay();
                    }
                })
                .withDefault(() -> Boolean.FALSE));

        bindings.put(SettingsDefinitions.DEBUG_MENU, SettingBinding.of(
                () -> debugMenu() != null && debugMenu().isEnabled(),
                value -> {
                    DebugOverlay overlay = debugMenu();
                    if (overlay != null) {
                        overlay.setEnabled(boolValue(value));
                    }
                })
                .withDefault(() -> Boolean.FALSE));

        bindings.put(SettingsDefinitions.DEBUG_MENU_KEY, SettingBinding.of(
                SettingsCatalog::debugMenuKeyCode,
                value -> bindDebugMenuKey(intValue(value)))
                .withFormatter(value -> keyLabel(intValue(value)))
                .withDefault(() -> GLFW.GLFW_KEY_END));

        bindings.put(SettingsDefinitions.VSR_DEBUG, SettingBinding.of(
                () -> Initializer.CONFIG.vsrDebug,
                value -> Initializer.CONFIG.vsrDebug = boolValue(value))
                .withDefault(() -> Boolean.FALSE));

    }

    private void bindEnvironmentWind() {
        bindings.put(SettingsDefinitions.WIND_STRENGTH, SettingBinding.ranged(
                () -> Math.round(Initializer.CONFIG.windStrength * SettingsDefinitions.PERCENT_SCALE),
                value -> {
                    int percent = intValue(value);
                    Initializer.CONFIG.windEnabled = percent > 0;
                    Initializer.CONFIG.windStrength = percent / (float) SettingsDefinitions.PERCENT_SCALE;
                },
                0, SettingsDefinitions.WIND_MAX, SettingsDefinitions.WIND_STEP)
                .withDefault(() -> SettingsDefinitions.WIND_DEFAULT)
                .withFormatter(SettingsCatalog::windLabel));
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
                    requestChunkRebuild();
                })
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.CUSTOM_ITEM_TEXTURES, SettingBinding.of(
                () -> Initializer.CONFIG.citEnabled,
                value -> Initializer.CONFIG.citEnabled = boolValue(value))
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.CORE_SHADER_PACKS, SettingBinding.of(
                () -> Initializer.CONFIG.sodiumCoreShaders,
                value -> {
                    Initializer.CONFIG.sodiumCoreShaders = boolValue(value);
                    Minecraft.getInstance().delayTextureReload();
                })
                .withDefault(() -> Boolean.TRUE));
    }

    private void bindGlint() {
        bindings.put(SettingsDefinitions.GLINT_STRENGTH, SettingBinding.scaled(
                () -> Minecraft.getInstance().options.glintStrength().get(),
                value -> Minecraft.getInstance().options.glintStrength().set(doubleValue(value)),
                SettingsDefinitions.EFFECT_SCALE_MIN, SettingsDefinitions.EFFECT_SCALE_MAX,
                SettingsDefinitions.EFFECT_SCALE_STEP, SettingsDefinitions.PERCENT_SCALE)
                .withDefault(() -> SettingsDefinitions.GLINT_DEFAULT)
                .withFormatter(SettingsCatalog::percentLabel));
    }

    private void bindQualityLighting() {
        bindings.put(SettingsDefinitions.AMBIENT_OCCLUSION, SettingBinding.choosing(
                () -> ambientOcclusionKey(Initializer.CONFIG.ambientOcclusion),
                value -> {
                    int mode = ambientOcclusionFor(label(value));
                    Minecraft.getInstance().options.ambientOcclusion().set(mode > LightMode.FLAT);
                    Initializer.CONFIG.ambientOcclusion = mode;
                    requestChunkRebuild();
                },
                () -> AMBIENT_OCCLUSION_MODES.stream().map(SettingsCatalog::ambientOcclusionKey).toList())
                .withDefault(() -> ambientOcclusionKey(LightMode.SMOOTH)));

        bindings.put(SettingsDefinitions.BIOME_BLEND, SettingBinding.ranged(
                () -> Minecraft.getInstance().options.biomeBlendRadius().get(),
                value -> {
                    Minecraft.getInstance().options.biomeBlendRadius().set(intValue(value));
                    requestChunkRebuild();
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
                .withDefault(CloudStatus.FANCY::getKey));

        bindings.put(SettingsDefinitions.WEATHER_RENDERING, SettingBinding.of(
                () -> Initializer.CONFIG.weatherRendering,
                value -> Initializer.CONFIG.weatherRendering = boolValue(value))
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.HORIZON_FOG, SettingBinding.scaled(
                () -> Initializer.CONFIG.horizonFog,
                value -> Initializer.CONFIG.horizonFog = (float) doubleValue(value),
                SettingsDefinitions.EFFECT_SCALE_MIN, SettingsDefinitions.EFFECT_SCALE_MAX,
                SettingsDefinitions.HORIZON_FOG_STEP, SettingsDefinitions.PERCENT_SCALE)
                .withDefault(() -> SettingsDefinitions.HORIZON_FOG_DEFAULT)
                .withFormatter(SettingsCatalog::percentLabel));
    }

    private void bindRenderingAdvanced() {

        bindings.put(SettingsDefinitions.PARTICLES, SettingBinding.choosing(
                () -> Minecraft.getInstance().options.particles().get().getKey(),
                value -> Minecraft.getInstance().options.particles().set(particleStatusFor(label(value))),
                () -> particleLevels().stream().map(ParticleStatus::getKey).toList())
                .withDefault(() -> ParticleStatus.ALL.getKey()));
    }

    private void bindRenderingEntities() {

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
        bindings.put(SettingsDefinitions.VULKAN_VALIDATION, SettingBinding.of(
                () -> Initializer.CONFIG.vulkanValidation,
                value -> Initializer.CONFIG.vulkanValidation = boolValue(value))
                .withDefault(() -> Boolean.FALSE));

        bindings.put(SettingsDefinitions.GPU_DEVICE, SettingBinding.choosing(
                () -> deviceLabel(Initializer.CONFIG.device),
                value -> Initializer.CONFIG.device = deviceFor(label(value)),
                SettingsCatalog::deviceChoices)
                .withDefault(() -> deviceLabel(DEVICE_AUTO)));
    }

    private void bindAdvancedSynchronization() {
        bindings.put(SettingsDefinitions.MOLTENVK_AGGRESSIVE, SettingBinding.of(
                MoltenVKConfig::aggressiveEnabled,
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
                ExternalRenderPathOptions::externalLodDrawEnabled,
                value -> Initializer.CONFIG.externalLodDraw = boolValue(value))
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.GL_LEGACY_BRIDGE, SettingBinding.of(
                GlDrawOptions::shouldPreserveLegacyBridge,
                value -> Initializer.CONFIG.glLegacyBridge = boolValue(value))
                .withDefault(() -> Boolean.TRUE));

        bindings.put(SettingsDefinitions.GL_FBO_VIEWPORT, SettingBinding.of(
                GlDrawOptions::fboViewportUsesFboConvention,
                value -> Initializer.CONFIG.glFboViewport = boolValue(value))
                .withDefault(() -> Boolean.TRUE));
    }

    private void bindExperimental() {
        bindings.put(SettingsDefinitions.ANISOTROPIC_FILTERING, SettingBinding.choosing(
                () -> anisotropyKey(Initializer.CONFIG.anisotropicFiltering),
                value -> {
                    Initializer.CONFIG.anisotropicFiltering = anisotropyFor(label(value));
                    Minecraft.getInstance().delayTextureReload();
                },
                () -> ANISOTROPY_LEVELS.stream().map(SettingsCatalog::anisotropyKey).toList())
                .withDefault(() -> anisotropyKey(1)));
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

    private static void unhideOverlay(boolean wanted) {
        net.vulkanmod.render.profiling.FpsOverlay overlay =
                HudHandler.getInstance().get(net.vulkanmod.render.profiling.FpsOverlay.class);
        if (wanted && overlay != null) {
            overlay.setEnabled(true);
        }
    }

    private static DebugOverlay debugMenu() {
        return HudHandler.getInstance().get(DebugOverlay.class);
    }

    private static String keyLabel(int code) {
        return InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
    }

    private static int debugMenuKeyCode() {
        DebugOverlay overlay = debugMenu();
        return overlay == null ? GLFW.GLFW_KEY_END : overlay.getToggleKeyMapping().getKey().getValue();
    }

    private static void bindDebugMenuKey(int code) {
        DebugOverlay overlay = debugMenu();
        if (overlay == null) {
            return;
        }
        Minecraft.getInstance().options.setKey(overlay.getToggleKeyMapping(),
                code == GLFW.GLFW_KEY_UNKNOWN
                        ? InputConstants.UNKNOWN
                        : InputConstants.Type.KEYSYM.getOrCreate(code));
        KeyMapping.resetMapping();
    }

    private static String blockEntityDistanceLabel(Object value) {
        int distance = intValue(value);
        return distance >= SettingsDefinitions.BLOCK_ENTITY_DISTANCE_MAX
                ? I18n.get("vulkanmod.options.blockEntityDistance.unlimited")
                : I18n.get("vulkanmod.options.blockEntityDistance.blocks", distance);
    }

    private static String anisotropyKey(int level) {
        return level <= 1 ? I18n.get("options.off") : level + "x";
    }

    private static int anisotropyFor(String label) {
        for (int level : ANISOTROPY_LEVELS) {
            if (anisotropyKey(level).equals(label)) {
                return level;
            }
        }
        throw new IllegalArgumentException("unknown anisotropy level " + label);
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
        return List.of(PerformancePreset.PERFORMANCE, PerformancePreset.BALANCED,
                PerformancePreset.QUALITY, PerformancePreset.ULTRA, PerformancePreset.CUSTOM);
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


    private static String windLabel(Object value) {
        int percent = intValue(value);
        return percent == 0 ? I18n.get("options.off") : percent + "%";
    }

    private static String builderThreadsLabel(Object value) {
        int threads = intValue(value);
        return threads <= SettingsDefinitions.BUILDER_THREADS_AUTO
                ? I18n.get("options.guiScale.auto") + " (" + TaskDispatcher.autoThreadCount() + ")"
                : Integer.toString(threads);
    }

    private static int clampIndex(int value, int size) {
        return Math.max(0, Math.min(size - 1, value));
    }

    private static int indexOfLabel(List<String> keys, Object value) {
        String label = String.valueOf(value);
        for (int index = 0; index < keys.size(); index++) {
            if (I18n.get(keys.get(index)).equals(label)) {
                return index;
            }
        }
        return 0;
    }

    private static String percentLabel(Object value) {
        int percent = intValue(value);
        return percent == SettingsDefinitions.EFFECT_SCALE_MIN
                ? I18n.get("options.off")
                : percent + "%";
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

    private static final String PLUGIN_SPI = "net.vulkanmod.plugin.RenderPipelinePlugin";

    private void bindShadersCurrent() {
        bindings.put(SettingsDefinitions.SHADERS_ENABLED, SettingBinding.of(
                        () -> Initializer.CONFIG.shadersEnabled,
                        value -> Initializer.CONFIG.shadersEnabled = boolValue(value))
                .withDefault(() -> Boolean.FALSE));

        bindings.put(SettingsDefinitions.SHADERS_SELECTED_PACK, SettingBinding.choosing(
                        () -> Initializer.CONFIG.selectedShader,
                        value -> Initializer.CONFIG.selectedShader = label(value),
                        SettingsCatalog::availableShaders)
                .withDefault(() -> "off"));
    }

    private static List<String> availableShaders() {
        List<String> ids = new ArrayList<>();
        ids.add("off");

        ids.addAll(PluginRegistry.getPlugins()
                .values()
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(pl -> pl.getPlugin()
                            .type() == PluginTargetType.SHADERS)
                    .map(pl -> pl.getPlugin().id())
                    .toList());

        String selected = Initializer.CONFIG.selectedShader;

        if (selected != null && !ids.contains(selected))
            ids.add(selected);

        return List.copyOf(ids);
    }

    private static List<String> discoveredPluginIds() {
        try {
            Class<?> spi = Class.forName(PLUGIN_SPI);
            List<String> ids = new ArrayList<>();
            for (Object plugin : ServiceLoader.load(spi)) {
                Object id = spi.getMethod("id").invoke(plugin);
                if (id instanceof String text && !text.isBlank()) {
                    ids.add(text);
                }
            }
            return ids;
        } catch (ClassNotFoundException absent) {
            return List.of();
        } catch (Throwable failure) {
            Initializer.LOGGER.warn("Shader plugin discovery unavailable: {}", failure.toString());
            return List.of();
        }
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
            VideoModeManager.fullscreenDirty = true;
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
