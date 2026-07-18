package net.vulkanmod.config.option;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.*;
import net.minecraft.network.chat.Component;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.GraphicsModeCompatibility;
import net.vulkanmod.config.PerformancePreset;
import net.vulkanmod.config.PerformancePresetApplier;
import net.vulkanmod.config.RenderScale;
import net.vulkanmod.config.gui.OptionBlock;

import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.config.video.VideoModeSet;
import net.vulkanmod.config.video.WindowMode;
import net.vulkanmod.render.chunk.build.light.LightMode;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.device.DeviceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public abstract class Options {
    public static boolean fullscreenDirty = false;

    public static String shaderNav = null;
    public static String shaderCat = null;
    public static Runnable shaderNavRebuild = null;

    private static boolean shaderOptsBuilt = false;
    private static Option<Boolean> enabledOpt;
    private static ShaderEntryOption fogEntryOpt;
    private static ShaderEntryOption colorGradeEntryOpt;
    private static RangeOption fogDensityOpt;
    private static RangeOption fogHeightOpt;
    private static Option<Boolean> shadowsOpt;
    private static Option<Boolean> taaOpt;
    private static RangeOption shadowQualityOpt;
    private static RangeOption shadowDistanceOpt;
    private static RangeOption exposureOpt;
    private static RangeOption contrastOpt;
    private static RangeOption saturationOpt;
    private static RangeOption temperatureOpt;
    private static Option<?>[] lightingOptsCache;
    private static Option<?>[] cameraOptsCache;

    static Config config = Initializer.CONFIG;
    static Minecraft minecraft = Minecraft.getInstance();
    static Window window = minecraft.getWindow();
    static net.minecraft.client.Options minecraftOptions = minecraft.options;

    private static void markPerformancePresetCustom() {
        config.performancePreset = PerformancePreset.CUSTOM.id;
    }

    public static List<OptionPage> getOptionPages() {
        List<OptionPage> optionPages = new ArrayList<>();

        optionPages.add(new OptionPage(
                Component.translatable("vulkanmod.options.pages.video").getString(),
                Options.getVideoOpts()));

        optionPages.add(new OptionPage(
                Component.translatable("vulkanmod.options.pages.shaders").getString(),
                Options.getShaderOpts()));

        optionPages.add(new OptionPage(
                Component.translatable("vulkanmod.options.pages.graphics").getString(),
                Options.getGraphicsOpts()));

        optionPages.add(new OptionPage(
                Component.translatable("vulkanmod.options.pages.optimizations").getString(),
                Options.getOptimizationOpts()));

        optionPages.add(new OptionPage(
                Component.translatable("vulkanmod.options.pages.other").getString(),
                Options.getOtherOpts()));

        return optionPages;
    }

    public static void resetShaderNav() {
        shaderNav = null;
        shaderCat = null;
        shaderOptsBuilt = false;
    }

    public static void invalidateShaderOptsCache() {
        shaderOptsBuilt = false;
    }

    private static void buildShaderOptionsIfNeeded() {
        if (shaderOptsBuilt) {
            return;
        }

        enabledOpt = new SwitchOption(
                Component.translatable("vulkanmod.options.shadersEnabled"),
                value -> config.shadersEnabled = value,
                () -> config.shadersEnabled)
                .setTooltip(Component.translatable("vulkanmod.options.shadersEnabled.tooltip"));

        fogEntryOpt = shaderListEntry("fog");
        colorGradeEntryOpt = shaderListEntry("color_grade");

        shadowsOpt = new SwitchOption(
                Component.translatable("vulkanmod.options.shadowsEnabled"),
                value -> config.shadowsEnabled = value,
                () -> config.shadowsEnabled);
        shadowsOpt.setActivationFn(() -> config.shadersEnabled && "fog".equals(config.selectedShader));

        taaOpt = new SwitchOption(
                Component.translatable("vulkanmod.options.taaEnabled"),
                value -> config.taaEnabled = value,
                () -> config.taaEnabled);
        taaOpt.setActivationFn(() -> config.shadersEnabled && "fog".equals(config.selectedShader));

        final int[] shadowRes = { 1024, 2048, 3072, 4096, 6144 };
        shadowQualityOpt = new RangeOption(
                Component.translatable("vulkanmod.options.shadowQuality"),
                0, 4, 1,
                value -> Component.nullToEmpty(shadowRes[Math.max(0, Math.min(4, value))] + " px"),
                value -> config.shadowQuality = value,
                () -> config.shadowQuality);
        shadowQualityOpt.setActivationFn(() -> config.shadersEnabled && "fog".equals(config.selectedShader) && config.shadowsEnabled);

        shadowDistanceOpt = new RangeOption(
                Component.translatable("vulkanmod.options.shadowDistance"),
                24, 160, 8,
                value -> Component.nullToEmpty(value + " blocks"),
                value -> config.shadowDistance = value,
                () -> config.shadowDistance);
        shadowDistanceOpt.setActivationFn(() -> config.shadersEnabled && "fog".equals(config.selectedShader) && config.shadowsEnabled);

        exposureOpt = new RangeOption(
                Component.translatable("vulkanmod.options.cgExposure"),
                50, 200, 5,
                value -> Component.nullToEmpty(String.format("%.2f", value / 100.0f)),
                value -> config.cgExposure = value / 100.0f,
                () -> Math.round(config.cgExposure * 100.0f));
        exposureOpt.setActivationFn(() -> config.shadersEnabled && "color_grade".equals(config.selectedShader));

        contrastOpt = new RangeOption(
                Component.translatable("vulkanmod.options.cgContrast"),
                50, 200, 5,
                value -> Component.nullToEmpty(String.format("%.2f", value / 100.0f)),
                value -> config.cgContrast = value / 100.0f,
                () -> Math.round(config.cgContrast * 100.0f));
        contrastOpt.setActivationFn(() -> config.shadersEnabled && "color_grade".equals(config.selectedShader));

        saturationOpt = new RangeOption(
                Component.translatable("vulkanmod.options.cgSaturation"),
                0, 200, 5,
                value -> Component.nullToEmpty(String.format("%.2f", value / 100.0f)),
                value -> config.cgSaturation = value / 100.0f,
                () -> Math.round(config.cgSaturation * 100.0f));
        saturationOpt.setActivationFn(() -> config.shadersEnabled && "color_grade".equals(config.selectedShader));

        temperatureOpt = new RangeOption(
                Component.translatable("vulkanmod.options.cgTemperature"),
                -100, 100, 5,
                value -> Component.nullToEmpty(String.format("%.2f", value / 100.0f)),
                value -> config.cgTemperature = value / 100.0f,
                () -> Math.round(config.cgTemperature * 100.0f));
        temperatureOpt.setActivationFn(() -> config.shadersEnabled && "color_grade".equals(config.selectedShader));

        fogDensityOpt = new RangeOption(
                Component.translatable("vulkanmod.options.fogDensity"),
                0, 30, 1,
                value -> Component.nullToEmpty(String.format("%.2f", value / 100.0f)),
                value -> config.fogDensity = value / 100.0f,
                () -> Math.round(config.fogDensity * 100.0f));
        fogDensityOpt.setActivationFn(() -> config.shadersEnabled && "fog".equals(config.selectedShader));

        fogHeightOpt = new RangeOption(
                Component.translatable("vulkanmod.options.fogHeight"),
                0, 256, 4,
                value -> Component.nullToEmpty(String.valueOf(value)),
                value -> config.fogHeight = value,
                () -> Math.round(config.fogHeight));
        fogHeightOpt.setActivationFn(() -> config.shadersEnabled && "fog".equals(config.selectedShader));

        lightingOptsCache = buildLightingOptions();
        cameraOptsCache = buildCameraOptions();

        shaderOptsBuilt = true;
    }

    public static OptionBlock[] getShaderOpts() {
        buildShaderOptionsIfNeeded();

        if ("fog".equals(shaderNav)) {
            if ("fog".equals(shaderCat)) {
                return new OptionBlock[]{
                        new OptionBlock("", new Option<?>[]{ backTo(() -> shaderCat = null) }),
                        new OptionBlock(Component.translatable("vulkanmod.options.category.fog").getString(),
                                new Option<?>[]{ fogDensityOpt, fogHeightOpt })
                };
            }

            if ("shadows".equals(shaderCat)) {
                return new OptionBlock[]{
                        new OptionBlock("", new Option<?>[]{ backTo(() -> shaderCat = null) }),
                        new OptionBlock(Component.translatable("vulkanmod.options.category.shadows").getString(),
                                new Option<?>[]{ shadowsOpt, shadowQualityOpt, shadowDistanceOpt, taaOpt })
                };
            }

            if ("lighting".equals(shaderCat)) {
                return new OptionBlock[]{
                        new OptionBlock("", new Option<?>[]{ backTo(() -> shaderCat = null) }),
                        new OptionBlock(Component.translatable("vulkanmod.options.pages.lighting").getString(),
                                lightingOptsCache)
                };
            }

            if ("camera".equals(shaderCat)) {
                return new OptionBlock[]{
                        new OptionBlock("", new Option<?>[]{ backTo(() -> shaderCat = null) }),
                        new OptionBlock(Component.translatable("vulkanmod.options.category.camera").getString(),
                                cameraOptsCache)
                };
            }

            ActionOption fogCat = new ActionOption(
                    Component.translatable("vulkanmod.options.category.fog"),
                    Component.translatable("vulkanmod.options.category.fog"),
                    () -> {
                        shaderCat = "fog";
                        if (shaderNavRebuild != null) shaderNavRebuild.run();
                    }).setFullButton(true);

            ActionOption shadowsCat = new ActionOption(
                    Component.translatable("vulkanmod.options.category.shadows"),
                    Component.translatable("vulkanmod.options.category.shadows"),
                    () -> {
                        shaderCat = "shadows";
                        if (shaderNavRebuild != null) shaderNavRebuild.run();
                    }).setFullButton(true);

            ActionOption lightingCat = new ActionOption(
                    Component.translatable("vulkanmod.options.pages.lighting"),
                    Component.translatable("vulkanmod.options.pages.lighting"),
                    () -> {
                        shaderCat = "lighting";
                        if (shaderNavRebuild != null) shaderNavRebuild.run();
                    }).setFullButton(true);

            ActionOption cameraCat = new ActionOption(
                    Component.translatable("vulkanmod.options.category.camera"),
                    Component.translatable("vulkanmod.options.category.camera"),
                    () -> {
                        shaderCat = "camera";
                        if (shaderNavRebuild != null) shaderNavRebuild.run();
                    }).setFullButton(true);

            return new OptionBlock[]{
                    new OptionBlock("", new Option<?>[]{ backTo(() -> { shaderNav = null; shaderCat = null; }) }),
                    new OptionBlock(Component.translatable("vulkanmod.options.category.general").getString(),
                            new Option<?>[]{ fogCat, shadowsCat, lightingCat, cameraCat })
            };
        }

        if ("color_grade".equals(shaderNav)) {
            return new OptionBlock[]{
                    new OptionBlock("", new Option<?>[]{ backTo(() -> { shaderNav = null; shaderCat = null; }) }),
                    new OptionBlock(Component.translatable("vulkanmod.options.category.colorGrade").getString(),
                            new Option<?>[]{ exposureOpt, contrastOpt, saturationOpt, temperatureOpt })
            };
        }

        return new OptionBlock[]{
                new OptionBlock("", new Option<?>[]{ enabledOpt }),
                new OptionBlock(Component.translatable("vulkanmod.options.category.shaderPack").getString(),
                        new Option<?>[]{ fogEntryOpt, colorGradeEntryOpt })
        };
    }

    private static ActionOption backTo(Runnable navChange) {
        return new ActionOption(
                Component.empty(),
                Component.literal("← Back"),
                () -> {
                    navChange.run();
                    if (shaderNavRebuild != null) shaderNavRebuild.run();
                }).setBackStyle(true);
    }

    private static ShaderEntryOption shaderListEntry(String id) {
        return new ShaderEntryOption(
                Component.translatable("vulkanmod.options.shader." + id),
                () -> id.equals(config.selectedShader),
                () -> config.selectedShader = id,
                () -> {
                    shaderNav = id;
                    shaderCat = null;
                    if (shaderNavRebuild != null) shaderNavRebuild.run();
                });
    }

    public static OptionBlock[] getVideoOpts() {
        var videoMode = config.videoMode;
        var videoModeSet = VideoModeManager.getFromVideoMode(videoMode);

        if (videoModeSet == null) {
            videoModeSet = VideoModeSet.getDummy();
            videoMode = videoModeSet.getVideoMode(-1);
        }

        VideoModeManager.selectedVideoMode = videoMode;
        var refreshRates = videoModeSet.getRefreshRates();

        CyclingOption<Integer> RefreshRate = (CyclingOption<Integer>) new CyclingOption<>(
                Component.translatable("vulkanmod.options.refreshRate"),
                refreshRates.toArray(new Integer[0]),
                (value) -> {
                    VideoModeManager.selectedVideoMode.refreshRate = value;
                    VideoModeManager.applySelectedVideoMode();

                    if (minecraftOptions.fullscreen().get())
                        fullscreenDirty = true;
                },
                () -> VideoModeManager.selectedVideoMode.refreshRate)
                .setTranslator(refreshRate -> Component.nullToEmpty(refreshRate.toString()));

        Option<VideoModeSet> resolutionOption = new CyclingOption<>(
                Component.translatable("options.fullscreen.resolution"),
                VideoModeManager.getVideoResolutions(),
                (value) -> {
                    VideoModeManager.selectedVideoMode = value.getVideoMode(RefreshRate.getNewValue());
                    VideoModeManager.applySelectedVideoMode();

                    if (minecraftOptions.fullscreen().get())
                        fullscreenDirty = true;
                },
                () -> {
                    var selectedVideoMode = VideoModeManager.selectedVideoMode;
                    var selectedVideoModeSet = VideoModeManager.getFromVideoMode(selectedVideoMode);

                    return selectedVideoModeSet != null ? selectedVideoModeSet : VideoModeSet.getDummy();
                })
                .setTranslator(resolution -> Component.nullToEmpty(resolution.toString()));

        resolutionOption.setOnChange(() -> {
            var newVideoMode = resolutionOption.getNewValue();
            var newRefreshRates = newVideoMode.getRefreshRates().toArray(new Integer[0]);

            RefreshRate.setValues(newRefreshRates);
            RefreshRate.setNewValue(newRefreshRates[newRefreshRates.length - 1]);
        });

        var windowModeOption = new CyclingOption<>(Component.translatable("vulkanmod.options.windowMode"),
                WindowMode.values(),
                value -> {
                    minecraftOptions.fullscreen().set(value == WindowMode.EXCLUSIVE_FULLSCREEN);
                    config.windowedFullscreen = (value == WindowMode.WINDOWED_FULLSCREEN);
                    fullscreenDirty = true;
                },
                () -> minecraftOptions.fullscreen().get() ? WindowMode.EXCLUSIVE_FULLSCREEN
                        : config.windowedFullscreen ? WindowMode.WINDOWED_FULLSCREEN
                        : WindowMode.WINDOWED)
                .setTranslator(value -> Component.translatable(WindowMode.getComponentName(value)));

        resolutionOption.setActivationFn(() -> windowModeOption.getNewValue() == WindowMode.EXCLUSIVE_FULLSCREEN);
        RefreshRate.setActivationFn(() -> windowModeOption.getNewValue() == WindowMode.EXCLUSIVE_FULLSCREEN);

        windowModeOption.setOnChange(() -> {
            resolutionOption.updateActiveState();
            RefreshRate.updateActiveState();
        });

        return new OptionBlock[]{
                new OptionBlock("", new Option<?>[]{
                        windowModeOption,
                        resolutionOption,
                        RefreshRate,
                        new RangeOption(Component.translatable("options.framerateLimit"),
                                10, 260, 10,
                                value -> Component.nullToEmpty(value == 260 ?
                                        Component.translatable("options.framerateLimit.max").getString() :
                                        String.valueOf(value)),
                                value -> {
                                    minecraftOptions.framerateLimit().set(value);
                                    window.setFramerateLimit(value);
                                },
                                () -> minecraftOptions.framerateLimit().get()),
                        new SwitchOption(Component.translatable("options.vsync"),
                                value -> {
                                    minecraftOptions.enableVsync().set(value);
                                    window.updateVsync(value);
                                },
                                () -> minecraftOptions.enableVsync().get()),
                        new SwitchOption(Component.translatable("vulkanmod.options.disableHiDPI"),
                                value -> config.disableHiDPI = value,
                                () -> config.disableHiDPI)
                                .setTooltip(Component.translatable("vulkanmod.options.disableHiDPI.tooltip")),
                }),
                new OptionBlock("", new Option<?>[]{
                        new RangeOption(Component.translatable("options.guiScale"),
                                0, window.calculateScale(0, minecraft.isEnforceUnicode()), 1,
                                value -> Component.translatable((value == 0)
                                        ? "options.guiScale.auto"
                                        : String.valueOf(value)),
                                value -> {
                                    minecraftOptions.guiScale().set(value);
                                    minecraft.resizeDisplay();
                                },
                                () -> (minecraftOptions.guiScale().get())),
                        new RangeOption(Component.translatable("options.gamma"),
                                0, 100, 1,
                                value -> Component.translatable(switch (value) {
                                    case 0 -> "options.gamma.min";
                                    case 50 -> "options.gamma.default";
                                    case 100 -> "options.gamma.max";
                                    default -> String.valueOf(value);
                                }),
                                value -> minecraftOptions.gamma().set(value * 0.01),
                                () -> (int) (minecraftOptions.gamma().get() * 100.0)),
                }),
                new OptionBlock("", new Option<?>[]{
                        new SwitchOption(Component.translatable("options.viewBobbing"),
                                (value) -> minecraftOptions.bobView().set(value),
                                () -> minecraftOptions.bobView().get()),
                        new CyclingOption<>(Component.translatable("options.attackIndicator"),
                                AttackIndicatorStatus.values(),
                                value -> minecraftOptions.attackIndicator().set(value),
                                () -> minecraftOptions.attackIndicator().get())
                                .setTranslator(value -> Component.translatable(value.getKey())),
                        new SwitchOption(Component.translatable("options.autosaveIndicator"),
                                value -> minecraftOptions.showAutosaveIndicator().set(value),
                                () -> minecraftOptions.showAutosaveIndicator().get()),
                })
        };
    }

    private static Option<?>[] buildLightingOptions() {
        SwitchOption customLightmap = new SwitchOption(
                Component.translatable("vulkanmod.options.customLightmap"),
                value -> config.customLightmap = value,
                () -> config.customLightmap);

        RangeOption brightness = new RangeOption(
                Component.translatable("vulkanmod.options.lightBrightness"),
                60, 150, 5,
                value -> Component.nullToEmpty(String.format("%.2f", value / 100.0f)),
                value -> config.lightBrightness = value / 100.0f,
                () -> Math.round(config.lightBrightness * 100.0f));
        brightness.setActivationFn(() -> config.customLightmap);

        RangeOption nightDarkness = new RangeOption(
                Component.translatable("vulkanmod.options.nightDarkness"),
                0, 100, 5,
                value -> Component.nullToEmpty(String.format("%.2f", value / 100.0f)),
                value -> config.nightDarkness = value / 100.0f,
                () -> Math.round(config.nightDarkness * 100.0f));
        nightDarkness.setActivationFn(() -> config.customLightmap);

        RangeOption torchIntensity = new RangeOption(
                Component.translatable("vulkanmod.options.torchIntensity"),
                50, 160, 5,
                value -> Component.nullToEmpty(String.format("%.2f", value / 100.0f)),
                value -> config.torchIntensity = value / 100.0f,
                () -> Math.round(config.torchIntensity * 100.0f));
        torchIntensity.setActivationFn(() -> config.customLightmap);

        RangeOption caveAmbient = new RangeOption(
                Component.translatable("vulkanmod.options.caveAmbient"),
                0, 15, 1,
                value -> Component.nullToEmpty(String.format("%.2f", value / 100.0f)),
                value -> config.caveAmbient = value / 100.0f,
                () -> Math.round(config.caveAmbient * 100.0f));
        caveAmbient.setActivationFn(() -> config.customLightmap);

        RangeOption glowStrength = new RangeOption(
                Component.translatable("vulkanmod.options.glowStrength"),
                0, 200, 10,
                value -> Component.nullToEmpty(String.format("%.2f", value / 100.0f)),
                value -> config.glowStrength = value / 100.0f,
                () -> Math.round(config.glowStrength * 100.0f));
        glowStrength.setActivationFn(() -> config.shadersEnabled && "fog".equals(config.selectedShader));

        SwitchOption pointLights = new SwitchOption(
                Component.translatable("vulkanmod.options.pointLights"),
                value -> config.pointLightsEnabled = value,
                () -> config.pointLightsEnabled);
        pointLights.setActivationFn(() -> config.shadersEnabled && "fog".equals(config.selectedShader));

        RangeOption pointLightStrength = new RangeOption(
                Component.translatable("vulkanmod.options.pointLightStrength"),
                0, 200, 10,
                value -> Component.nullToEmpty(String.format("%.2f", value / 100.0f)),
                value -> config.pointLightStrength = value / 100.0f,
                () -> Math.round(config.pointLightStrength * 100.0f));
        pointLightStrength.setActivationFn(() -> config.shadersEnabled && "fog".equals(config.selectedShader)
                && config.pointLightsEnabled);

        SwitchOption wind = new SwitchOption(
                Component.translatable("vulkanmod.options.wind"),
                value -> config.windEnabled = value,
                () -> config.windEnabled);

        RangeOption windStrength = new RangeOption(
                Component.translatable("vulkanmod.options.windStrength"),
                0, 200, 10,
                value -> Component.nullToEmpty(String.format("%.2f", value / 100.0f)),
                value -> config.windStrength = value / 100.0f,
                () -> Math.round(config.windStrength * 100.0f));
        windStrength.setActivationFn(() -> config.windEnabled);

        return new Option<?>[]{
                customLightmap,
                brightness,
                nightDarkness,
                torchIntensity,
                caveAmbient,
                glowStrength,
                pointLights,
                pointLightStrength,
                wind,
                windStrength
        };
    }

    private static Option<?>[] buildCameraOptions() {
        SwitchOption autoExposure = new SwitchOption(
                Component.translatable("vulkanmod.options.autoExposure"),
                value -> config.autoExposure = value,
                () -> config.autoExposure);
        autoExposure.setActivationFn(() -> config.shadersEnabled && "fog".equals(config.selectedShader));

        RangeOption exposureStrength = new RangeOption(
                Component.translatable("vulkanmod.options.exposureStrength"),
                0, 150, 10,
                value -> Component.nullToEmpty(String.format("%.2f", value / 100.0f)),
                value -> config.exposureStrength = value / 100.0f,
                () -> Math.round(config.exposureStrength * 100.0f));
        exposureStrength.setActivationFn(() -> config.shadersEnabled && "fog".equals(config.selectedShader)
                && config.autoExposure);

        return new Option<?>[]{
                autoExposure,
                exposureStrength
        };
    }

    public static OptionBlock[] getGraphicsOpts() {
        return new OptionBlock[]{
                new OptionBlock("", new Option<?>[]{
                        new RangeOption(Component.translatable("options.renderDistance"),
                                2, 32, 1,
                                (value) -> {
                                    markPerformancePresetCustom();
                                    minecraftOptions.renderDistance().set(value);
                                },
                                () -> minecraftOptions.renderDistance().get())
                                .setImpact(PerformanceImpact.HIGH),
                        new RangeOption(Component.translatable("options.simulationDistance"),
                                5, 32, 1,
                                (value) -> {
                                    markPerformancePresetCustom();
                                    minecraftOptions.simulationDistance().set(value);
                                },
                                () -> minecraftOptions.simulationDistance().get()),
                        new CyclingOption<>(Component.translatable("options.prioritizeChunkUpdates"),
                                PrioritizeChunkUpdates.values(),
                                value -> minecraftOptions.prioritizeChunkUpdates().set(value),
                                () -> minecraftOptions.prioritizeChunkUpdates().get())
                                .setTranslator(value -> Component.translatable(value.getKey())),
                }),
                new OptionBlock("", new Option<?>[]{
                        new CyclingOption<>(Component.translatable("options.graphics"),
                                GraphicsModeCompatibility.supportedModes(),
                                value -> {
                                    markPerformancePresetCustom();
                                    minecraftOptions.graphicsMode().set(GraphicsModeCompatibility.coerce(value));
                                },
                                () -> GraphicsModeCompatibility.coerce(minecraftOptions.graphicsMode().get()))
                                .setTranslator(graphicsMode -> Component.translatable(graphicsMode.getKey())),
                        new CyclingOption<>(Component.translatable("options.particles"),
                                new ParticleStatus[]{ParticleStatus.MINIMAL, ParticleStatus.DECREASED, ParticleStatus.ALL},
                                value -> {
                                    markPerformancePresetCustom();
                                    minecraftOptions.particles().set(value);
                                },
                                () -> minecraftOptions.particles().get())
                                .setTranslator(particlesMode -> Component.translatable(particlesMode.getKey()))
                                .setImpact(PerformanceImpact.MEDIUM),
                        new CyclingOption<>(Component.translatable("options.renderClouds"),
                                CloudStatus.values(),
                                value -> {
                                    markPerformancePresetCustom();
                                    minecraftOptions.cloudStatus().set(value);
                                },
                                () -> minecraftOptions.cloudStatus().get())
                                .setTranslator(value -> Component.translatable(value.getKey())),
                        new CyclingOption<>(Component.translatable("options.ao"),
                                new Integer[]{LightMode.FLAT, LightMode.SMOOTH, LightMode.SUB_BLOCK},
                                (value) -> {
                                    markPerformancePresetCustom();

                                    if (value > LightMode.FLAT)
                                        minecraftOptions.ambientOcclusion().set(true);
                                    else
                                        minecraftOptions.ambientOcclusion().set(false);

                                    config.ambientOcclusion = value;

                                    minecraft.levelRenderer.allChanged();
                                },
                                () -> config.ambientOcclusion)
                                .setTranslator(value -> Component.translatable(switch (value) {
                                    case LightMode.FLAT -> "options.off";
                                    case LightMode.SMOOTH -> "options.on";
                                    case LightMode.SUB_BLOCK -> "vulkanmod.options.ao.subBlock";
                                    default -> "vulkanmod.options.unknown";
                                }))
                                .setTooltip(Component.translatable("vulkanmod.options.ao.subBlock.tooltip"))
                                .setImpact(PerformanceImpact.LOW),
                        new RangeOption(Component.translatable("options.biomeBlendRadius"),
                                0, 7, 1,
                                value -> {
                                    int v = value * 2 + 1;
                                    return Component.nullToEmpty("%d x %d".formatted(v, v));
                                },
                                (value) -> {
                                    markPerformancePresetCustom();
                                    minecraftOptions.biomeBlendRadius().set(value);
                                    minecraft.levelRenderer.allChanged();
                                },
                                () -> minecraftOptions.biomeBlendRadius().get()),
                }),
                new OptionBlock("", new Option<?>[]{
                        new SwitchOption(Component.translatable("options.entityShadows"),
                                value -> {
                                    markPerformancePresetCustom();
                                    minecraftOptions.entityShadows().set(value);
                                },
                                () -> minecraftOptions.entityShadows().get())
                                .setImpact(PerformanceImpact.LOW),
                        new RangeOption(Component.translatable("options.entityDistanceScaling"),
                                50, 500, 25,
                                value -> {
                                    markPerformancePresetCustom();
                                    minecraftOptions.entityDistanceScaling().set(value * 0.01);
                                },
                                () -> (int) Math.round(minecraftOptions.entityDistanceScaling().get() * 100.0))
                                .setImpact(PerformanceImpact.HIGH),
                        new CyclingOption<>(Component.translatable("options.mipmapLevels"),
                                new Integer[]{0, 1, 2, 3, 4},
                                value -> {
                                    markPerformancePresetCustom();
                                    minecraftOptions.mipmapLevels().set(value);
                                    minecraft.updateMaxMipLevel(value);
                                    minecraft.delayTextureReload();
                                },
                                () -> minecraftOptions.mipmapLevels().get())
                                .setTranslator(value -> Component.nullToEmpty(value.toString()))
                                .setImpact(PerformanceImpact.LOW)
                })
        };
    }

    public static OptionBlock[] getOptimizationOpts() {
        return new OptionBlock[]{
                new OptionBlock("", new Option<?>[]{
                        new CyclingOption<>(Component.translatable("vulkanmod.options.performancePreset"),
                                PerformancePreset.values(),
                                value -> PerformancePresetApplier.apply(value, config, minecraft),
                                () -> PerformancePreset.byId(config.performancePreset))
                                .setTranslator(value -> Component.translatable(value.translationKey))
                                .setTooltip(Component.translatable("vulkanmod.options.performancePreset.tooltip"))
                }),
                new OptionBlock("", new Option[]{
                        new CyclingOption<>(Component.translatable("vulkanmod.options.advCulling"),
                                new Integer[]{1, 2, 3, 10},
                                value -> {
                                    markPerformancePresetCustom();
                                    config.advCulling = value;
                                },
                                () -> config.advCulling)
                                .setTranslator(value -> Component.translatable(switch (value) {
                                    case 1 -> "vulkanmod.options.advCulling.aggressive";
                                    case 2 -> "vulkanmod.options.advCulling.normal";
                                    case 3 -> "vulkanmod.options.advCulling.conservative";
                                    case 10 -> "options.off";
                                    default -> "vulkanmod.options.unknown";
                                }))
                                .setTooltip(Component.translatable("vulkanmod.options.advCulling.tooltip"))
                                .setImpact(PerformanceImpact.HIGH),
                        new SwitchOption(Component.translatable("vulkanmod.options.entityCulling"),
                                value -> {
                                    markPerformancePresetCustom();
                                    config.entityCulling = value;
                                },
                                () -> config.entityCulling)
                                .setTooltip(Component.translatable("vulkanmod.options.entityCulling.tooltip"))
                                .setImpact(PerformanceImpact.HIGH),
                        new SwitchOption(Component.translatable("vulkanmod.options.blockEntityCulling"),
                                value -> {
                                    markPerformancePresetCustom();
                                    config.blockEntityCulling = value;
                                },
                                () -> config.blockEntityCulling)
                                .setTooltip(Component.translatable("vulkanmod.options.blockEntityCulling.tooltip"))
                                .setImpact(PerformanceImpact.HIGH),
                        new SwitchOption(Component.translatable("vulkanmod.options.leavesCulling"),
                                value -> {
                                    markPerformancePresetCustom();
                                    config.leavesCulling = value;
                                    minecraft.levelRenderer.allChanged();
                                },
                                () -> config.leavesCulling)
                                .setTooltip(Component.translatable("vulkanmod.options.leavesCulling.tooltip"))
                                .setImpact(PerformanceImpact.HIGH),
                        new SwitchOption(Component.translatable("vulkanmod.options.uniqueOpaqueLayer"),
                                value -> {
                                    markPerformancePresetCustom();
                                    config.uniqueOpaqueLayer = value;
                                    minecraft.levelRenderer.allChanged();
                                },
                                () -> config.uniqueOpaqueLayer)
                                .setTooltip(Component.translatable("vulkanmod.options.uniqueOpaqueLayer.tooltip"))
                                .setImpact(PerformanceImpact.HIGH),
                        new SwitchOption(Component.translatable("vulkanmod.options.indirectDraw"),
                                value -> {
                                    markPerformancePresetCustom();
                                    config.indirectDraw = value && DeviceManager.supportsFastIndirectDraw();
                                },
                                () -> config.indirectDraw && DeviceManager.supportsFastIndirectDraw())
                                .setTooltip(Component.translatable("vulkanmod.options.indirectDraw.tooltip"))
                                .setImpact(PerformanceImpact.HIGH),
                        new SwitchOption(Component.translatable("vulkanmod.options.adaptiveChunkUploads"),
                                value -> {
                                    markPerformancePresetCustom();
                                    config.adaptiveChunkUploads = value;
                                },
                                () -> config.adaptiveChunkUploads)
                                .setTooltip(Component.translatable("vulkanmod.options.adaptiveChunkUploads.tooltip")),
                        new CyclingOption<>(Component.translatable("vulkanmod.options.particleCulling"),
                                new Integer[]{0, 1, 2, 3},
                                value -> {
                                    markPerformancePresetCustom();
                                    config.particleCulling = value;
                                },
                                () -> config.particleCulling)
                                .setTranslator(value -> Component.translatable(switch (value) {
                                    case 0 -> "options.off";
                                    case 1 -> "vulkanmod.options.particleCulling.quality";
                                    case 2 -> "vulkanmod.options.particleCulling.balanced";
                                    case 3 -> "vulkanmod.options.particleCulling.performance";
                                    default -> "vulkanmod.options.unknown";
                                }))
                                .setTooltip(Component.translatable("vulkanmod.options.particleCulling.tooltip")),
                })
        };

    }

    public static OptionBlock[] getOtherOpts() {
        return new OptionBlock[]{
                new OptionBlock("", new Option[]{
                        new RangeOption(Component.translatable("vulkanmod.options.renderScale"),
                                RenderScale.MIN, RenderScale.MAX, RenderScale.STEP,
                                value -> Component.nullToEmpty(value + "%"),
                                value -> {
                                    config.renderScale = RenderScale.clamp(value);
                                    minecraft.resizeDisplay();
                                },
                                () -> RenderScale.clamp(config.renderScale))
                                .setTooltip(Component.translatable("vulkanmod.options.renderScale.tooltip")),
                        new RangeOption(Component.translatable("vulkanmod.options.frameQueue"),
                                2, 5, 1,
                                value -> {
                                    markPerformancePresetCustom();
                                    config.frameQueueSize = value;
                                    Renderer.scheduleSwapChainUpdate();
                                }, () -> config.frameQueueSize)
                                .setTooltip(Component.translatable("vulkanmod.options.frameQueue.tooltip")),
                        new SwitchOption(Component.translatable("vulkanmod.options.textureAnimations"),
                                value -> {
                                    config.textureAnimations = value;
                                },
                                () -> config.textureAnimations),
                        new CyclingOption<>(Component.translatable("vulkanmod.options.deviceSelector"),
                                IntStream.range(-1, DeviceManager.suitableDevices.size()).boxed().toArray(Integer[]::new),
                                value -> config.device = value,
                                () -> config.device)
                                .setTranslator(value -> Component.translatable((value == -1)
                                        ? "vulkanmod.options.deviceSelector.auto"
                                        : DeviceManager.suitableDevices.get(value).deviceName)
                                )
                                .setTooltip(Component.nullToEmpty("%s: %s".formatted(
                                        Component.translatable("vulkanmod.options.deviceSelector.tooltip").getString(),
                                        DeviceManager.device.deviceName
                                ))
                        )
                })
        };

    }
}
