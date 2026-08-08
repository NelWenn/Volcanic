package net.vulkanmod.config.ui.settings;

import net.vulkanmod.config.ui.core.ApplyScope;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingSource;
import net.vulkanmod.config.ui.core.SettingType;

import java.util.List;

public final class SettingsDefinitions {
    public static final RouteId DISPLAY_GENERAL = RouteId.parse("display.general");

    public static final SettingId WINDOW_MODE = SettingId.parse("vulkanmod:display.window_mode");
    public static final SettingId RESOLUTION = SettingId.parse("vulkanmod:display.resolution");
    public static final SettingId REFRESH_RATE = SettingId.parse("vulkanmod:display.refresh_rate");
    public static final SettingId VSYNC = SettingId.parse("minecraft:display.vsync");
    public static final SettingId FRAMERATE_LIMIT = SettingId.parse("minecraft:display.framerate_limit");

    public static final int FRAMERATE_LIMIT_MIN = 10;
    public static final int FRAMERATE_LIMIT_MAX = 260;
    public static final int FRAMERATE_LIMIT_STEP = 10;

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
}
