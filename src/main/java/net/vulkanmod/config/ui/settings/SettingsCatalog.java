package net.vulkanmod.config.ui.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.option.Options;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingRegistry;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.config.video.VideoModeSet;
import net.vulkanmod.config.video.WindowMode;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Choice labels are translation keys; Minecraft renders an unknown key as itself, so plain
// values such as "1920 x 1080" or "144" pass through untranslated.
public final class SettingsCatalog {
    private final SettingRegistry registry = new SettingRegistry();
    private final Map<SettingId, SettingBinding> bindings = new LinkedHashMap<>();

    public SettingsCatalog() {
        bindDisplayGeneral();

        for (SettingMeta meta : SettingsDefinitions.displayGeneral()) {
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
        return true;
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
}
