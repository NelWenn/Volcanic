package net.vulkanmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.config.video.VideoModeSet;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;

public class Config {
    public int configVersion = ConfigVersion.CURRENT;

    public int frameQueueSize = 2;
    public VideoModeSet.VideoMode videoMode = VideoModeManager.getFirstAvailable().getVideoMode();
    public boolean windowedFullscreen = false;
    public int performancePreset = PerformancePreset.BALANCED.id;
    public int chunkUploadsPerFrame = PerformancePreset.BALANCED.chunkUploadsPerFrame;
    public boolean adaptiveChunkUploads = true;
    public boolean chunkFadeIn = true;
    public int renderScale = RenderScale.DEFAULT;
    public int vsrPreset = VsrPreset.OFF.id;
    public int vsrBackend = 0;
    public float vsrSharpness = 0.0f;
    public boolean vsrDebug = false;

    public boolean disableHiDPI = false;

    public int advCulling = 2;

    public boolean indirectDraw = true;
    public boolean lodGpuCulling = false;
    public boolean lodDepthSnapshot = true;

    public boolean uniqueOpaqueLayer = true;
    public boolean entityCulling = true;
    public boolean blockEntityCulling = true;
    public boolean leavesCulling = true;
    public int particleCulling = 2;
    public int device = -1;

    public boolean moltenvkAggressive = false;
    public String externalLod = "off";
    public boolean externalLodDraw = true;
    public boolean glLegacyBridge = true;
    public boolean glFboViewport = true;

    public int ambientOcclusion = 1;
    public boolean textureAnimations = true;
    public boolean ctmEnabled = true;
    public boolean citEnabled = true;

    public boolean shadersEnabled = false;
    public String selectedShader = "off";

    public boolean isCamille() {
        return "radiance".equals(selectedShader) && !net.vulkanmod.render.sodium.SodiumShaderBridge.isActive();
    }

    public boolean shadowsEnabled = true;
    public int shadowQuality = 2;
    public int shadowDistance = 48;
    public boolean entityShadows = true;
    public boolean coloredShadows = true;
    public boolean optimizedShadows = true;

    public int aaMode = 1;

    public boolean glassReflections = true;
    public boolean pbrDebugNormals = false;

    public float horizonFog = 1.0f;

    public boolean windEnabled = true;
    public float windStrength = 1.0f;

    public void write() {
        if (CONFIG_PATH == null) {
            if (!warnedNotSaving) {
                warnedNotSaving = true;
                Initializer.LOGGER.warn("Not saving: the config on disk could not be used, so it is"
                        + " being left alone. Settings apply for this session only.");
            }
            return;
        }

        Path parent = CONFIG_PATH.getParent();
        if(parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                System.err.println("[VulkanMod] Failed to create config directory: " + parent + " - " + e.getMessage());
                return;
            }
        }

        try {
            Files.write(CONFIG_PATH, Collections.singleton(GSON.toJson(this)));
        } catch (IOException e) {
            System.err.println("[VulkanMod] Failed to write config: " + CONFIG_PATH + " - " + e.getMessage());
        }
    }

    public java.util.List<String> disabledParticles = new java.util.ArrayList<>();
    public int chunkBuilderThreads = 0;
    public int showFps = 0;
    public boolean showCoordinates = false;
    public boolean perfLog = false;
    public boolean vulkanValidation = false;

    private static Path CONFIG_PATH;
    private static boolean warnedNotSaving;


    public static Config migrate(Config config) {
        config.configVersion = ConfigVersion.migrated(config.configVersion);
        return config;
    }

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();

    public static Config load(Path path) {
        Config.CONFIG_PATH = path;

        if (!Files.exists(path)) {
            return null;
        }

        Config config;
        try (FileReader fileReader = new FileReader(path.toFile())) {
            config = GSON.fromJson(fileReader, Config.class);
        }
        catch (IOException | RuntimeException exception) {
            return setAside(path, "could not be read (" + exception + ")");
        }

        if (config == null) {
            Initializer.LOGGER.warn("Config {} is empty, starting from defaults", path);
            return null;
        }
        if (config.configVersion > ConfigVersion.CURRENT) {
            Initializer.LOGGER.error("Config {} was written by a newer build (version {} > {})."
                            + " Running on defaults, and leaving that file untouched, so nothing will"
                            + " be saved this session.",
                    path, config.configVersion, ConfigVersion.CURRENT);
            Config.CONFIG_PATH = null;
            return null;
        }

        try {
            return migrate(config);
        }
        catch (RuntimeException exception) {
            return setAside(path, "could not be migrated (" + exception + ")");
        }
    }

    private static Config setAside(Path path, String reason) {
        Path backup = path.resolveSibling(path.getFileName() + ".bak");
        try {
            Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
            Initializer.LOGGER.warn("Config {} {}; kept it at {} and starting from defaults",
                    path, reason, backup);
        }
        catch (IOException failed) {
            Initializer.LOGGER.error("Config {} {}, and setting it aside failed ({}), so it will be"
                            + " left alone and nothing will be saved this session",
                    path, reason, failed.toString());
            Config.CONFIG_PATH = null;
        }
        return null;
    }
}
