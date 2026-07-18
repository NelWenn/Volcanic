package net.vulkanmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.config.video.VideoModeSet;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class Config {
    public int frameQueueSize = 2;
    public VideoModeSet.VideoMode videoMode = VideoModeManager.getFirstAvailable().getVideoMode();
    public boolean windowedFullscreen = false;
    public int performancePreset = PerformancePreset.BALANCED.id;
    public int chunkUploadsPerFrame = PerformancePreset.BALANCED.chunkUploadsPerFrame;
    public boolean adaptiveChunkUploads = true;
    public int renderScale = RenderScale.DEFAULT;

    public boolean disableHiDPI = false;

    public int advCulling = 2;

    public boolean indirectDraw = true;

    public boolean uniqueOpaqueLayer = true;
    public boolean entityCulling = true;
    public boolean blockEntityCulling = true;
    public boolean leavesCulling = true;
    public int particleCulling = 2;
    public int device = -1;

    public int ambientOcclusion = 1;
    public boolean textureAnimations = true;

    public boolean shadersEnabled = false;
    public String selectedShader = "off";

    // Camille shaders share the same render path (fog = Visual, radiance = Radiance) while Radiance's extra passes are built out
    public boolean isCamille() {
        return "fog".equals(selectedShader) || "radiance".equals(selectedShader);
    }
    public float cgExposure = 1.0f;
    public float cgContrast = 1.0f;
    public float cgSaturation = 1.0f;
    public float cgTemperature = 0.0f;
    public float fogDensity = 0.06f;
    public float fogHeight = 80.0f;
    public boolean shadowsEnabled = true;
    public boolean entityShadows = true;
    public boolean coloredShadows = false;
    public boolean taaEnabled = true;
    public boolean halfResTerms = true;
    public int shadowQuality = 2;
    public int shadowDistance = 48;

    public boolean customLightmap = true;
    public float lightBrightness = 1.0f;
    public float nightDarkness = 0.7f;
    public float torchIntensity = 1.0f;
    public float caveAmbient = 0.02f;
    public float glowStrength = 1.0f;
    public boolean pointLightsEnabled = true;
    public float pointLightStrength = 1.0f;

    public boolean windEnabled = true;
    public float windStrength = 1.0f;

    public boolean autoExposure = true;
    public float exposureStrength = 1.0f;

    public void write() {

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

    private static Path CONFIG_PATH;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();

    public static Config load(Path path) {
        Config config;
        Config.CONFIG_PATH = path;

        if (Files.exists(path)) {
            try (FileReader fileReader = new FileReader(path.toFile())) {
                config = GSON.fromJson(fileReader, Config.class);
            }
            catch (IOException exception) {
                throw new RuntimeException(exception.getMessage());
            }
        }
        else {
            config = null;
        }

        return config;
    }
}
