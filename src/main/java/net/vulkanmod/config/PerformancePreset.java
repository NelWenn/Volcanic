package net.vulkanmod.config;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.ParticleStatus;
import net.vulkanmod.render.chunk.build.light.LightMode;

public enum PerformancePreset {

    CUSTOM(0, "vulkanmod.options.performancePreset.custom"),

    PERFORMANCE(new Builder(1, "vulkanmod.options.performancePreset.performance")
            .indirectDraw(true)
            .adaptiveChunkUploads(true)
            .chunkUploadsPerFrame(3)
            .advCulling(1)
            .blockEntityCulling(true)
            .leavesQuality(1)
            .particleCulling(3)
            .uniqueOpaqueLayer(true)
            .renderDistance(8)
            .simulationDistance(5)
            .graphicsStatus(GraphicsStatus.FAST)
            .particleStatus(ParticleStatus.MINIMAL)
            .cloudStatus(CloudStatus.OFF)
            .entityShadows(false)
            .entityDistancePercent(50)
            .biomeBlendRadius(0)
            .ambientOcclusion(LightMode.FLAT)
            .mipmapLevels(0)),

    BALANCED(new Builder(2, "vulkanmod.options.performancePreset.balanced")
            .indirectDraw(true)
            .adaptiveChunkUploads(true)
            .chunkUploadsPerFrame(5)
            .advCulling(2)
            .blockEntityCulling(true)
            .leavesQuality(1)
            .particleCulling(2)
            .uniqueOpaqueLayer(true)
            .renderDistance(12)
            .simulationDistance(8)
            .graphicsStatus(GraphicsStatus.FAST)
            .particleStatus(ParticleStatus.DECREASED)
            .cloudStatus(CloudStatus.OFF)
            .entityShadows(false)
            .entityDistancePercent(75)
            .biomeBlendRadius(1)
            .ambientOcclusion(LightMode.SMOOTH)
            .mipmapLevels(2)),

    QUALITY(new Builder(3, "vulkanmod.options.performancePreset.quality")
            .indirectDraw(true)
            .adaptiveChunkUploads(true)
            .chunkUploadsPerFrame(16)
            .advCulling(2)
            .blockEntityCulling(true)
            .leavesQuality(1)
            .particleCulling(1)
            .uniqueOpaqueLayer(true)
            .renderDistance(16)
            .simulationDistance(16)
            .graphicsStatus(GraphicsStatus.FANCY)
            .particleStatus(ParticleStatus.ALL)
            .cloudStatus(CloudStatus.FANCY)
            .entityShadows(true)
            .entityDistancePercent(100)
            .biomeBlendRadius(5)
            .ambientOcclusion(LightMode.SMOOTH)
            .mipmapLevels(4)),

    ULTRA(new Builder(4, "vulkanmod.options.performancePreset.ultra")
            .indirectDraw(true)
            .adaptiveChunkUploads(true)
            .chunkUploadsPerFrame(16)
            .advCulling(2)
            .blockEntityCulling(true)
            .leavesQuality(1)
            .particleCulling(1)
            .uniqueOpaqueLayer(true)
            .renderDistance(24)
            .simulationDistance(24)
            .graphicsStatus(GraphicsStatus.FANCY)
            .particleStatus(ParticleStatus.ALL)
            .cloudStatus(CloudStatus.FANCY)
            .entityShadows(true)
            .entityDistancePercent(200)
            .biomeBlendRadius(7)
            .ambientOcclusion(LightMode.SMOOTH)
            .mipmapLevels(4));

    public final int id;
    public final String translationKey;
    public final boolean indirectDraw;
    public final boolean adaptiveChunkUploads;
    public final int chunkUploadsPerFrame;
    public final int advCulling;
    public final boolean blockEntityCulling;
    public final int leavesQuality;
    public final int particleCulling;
    public final boolean uniqueOpaqueLayer;
    public final int renderDistance;
    public final int simulationDistance;
    public final GraphicsStatus graphicsStatus;
    public final ParticleStatus particleStatus;
    public final CloudStatus cloudStatus;
    public final boolean entityShadows;
    public final int entityDistancePercent;
    public final int biomeBlendRadius;
    public final int ambientOcclusion;
    public final int mipmapLevels;

    PerformancePreset(int id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
        this.indirectDraw = false;
        this.adaptiveChunkUploads = false;
        this.chunkUploadsPerFrame = 0;
        this.advCulling = 0;
        this.blockEntityCulling = false;
        this.leavesQuality = 0;
        this.particleCulling = 0;
        this.uniqueOpaqueLayer = false;
        this.renderDistance = 0;
        this.simulationDistance = 0;
        this.graphicsStatus = null;
        this.particleStatus = null;
        this.cloudStatus = null;
        this.entityShadows = false;
        this.entityDistancePercent = 0;
        this.biomeBlendRadius = 0;
        this.ambientOcclusion = 0;
        this.mipmapLevels = 0;
    }

    PerformancePreset(Builder builder) {
        this.id = builder.id;
        this.translationKey = builder.translationKey;
        this.indirectDraw = builder.indirectDraw;
        this.adaptiveChunkUploads = builder.adaptiveChunkUploads;
        this.chunkUploadsPerFrame = builder.chunkUploadsPerFrame;
        this.advCulling = builder.advCulling;
        this.blockEntityCulling = builder.blockEntityCulling;
        this.leavesQuality = builder.leavesQuality;
        this.particleCulling = builder.particleCulling;
        this.uniqueOpaqueLayer = builder.uniqueOpaqueLayer;
        this.renderDistance = builder.renderDistance;
        this.simulationDistance = builder.simulationDistance;
        this.graphicsStatus = require(builder.graphicsStatus, "graphicsStatus", builder.translationKey);
        this.particleStatus = require(builder.particleStatus, "particleStatus", builder.translationKey);
        this.cloudStatus = require(builder.cloudStatus, "cloudStatus", builder.translationKey);
        this.entityShadows = builder.entityShadows;
        this.entityDistancePercent = builder.entityDistancePercent;
        this.biomeBlendRadius = builder.biomeBlendRadius;
        this.ambientOcclusion = builder.ambientOcclusion;
        this.mipmapLevels = builder.mipmapLevels;
    }

    private static <T> T require(T value, String field, String translationKey) {
        if (value == null) {
            throw new IllegalArgumentException(translationKey + " is missing " + field);
        }
        return value;
    }

    public static PerformancePreset byId(int id) {
        for (PerformancePreset preset : values()) {
            if (preset.id == id) {
                return preset;
            }
        }

        return CUSTOM;
    }

    static final class Builder {
        private final int id;
        private final String translationKey;
        private boolean indirectDraw;
        private boolean adaptiveChunkUploads;
        private int chunkUploadsPerFrame;
        private int advCulling;
        private boolean blockEntityCulling;
        private int leavesQuality;
        private int particleCulling;
        private boolean uniqueOpaqueLayer;
        private int renderDistance;
        private int simulationDistance;
        private GraphicsStatus graphicsStatus;
        private ParticleStatus particleStatus;
        private CloudStatus cloudStatus;
        private boolean entityShadows;
        private int entityDistancePercent;
        private int biomeBlendRadius;
        private int ambientOcclusion;
        private int mipmapLevels;

        Builder(int id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }

        Builder indirectDraw(boolean value) {
            this.indirectDraw = value;
            return this;
        }

        Builder adaptiveChunkUploads(boolean value) {
            this.adaptiveChunkUploads = value;
            return this;
        }

        Builder chunkUploadsPerFrame(int value) {
            this.chunkUploadsPerFrame = value;
            return this;
        }

        Builder advCulling(int value) {
            this.advCulling = value;
            return this;
        }

        Builder blockEntityCulling(boolean value) {
            this.blockEntityCulling = value;
            return this;
        }

        Builder leavesQuality(int value) {
            this.leavesQuality = value;
            return this;
        }

        Builder particleCulling(int value) {
            this.particleCulling = value;
            return this;
        }

        Builder uniqueOpaqueLayer(boolean value) {
            this.uniqueOpaqueLayer = value;
            return this;
        }

        Builder renderDistance(int value) {
            this.renderDistance = value;
            return this;
        }

        Builder simulationDistance(int value) {
            this.simulationDistance = value;
            return this;
        }

        Builder graphicsStatus(GraphicsStatus value) {
            this.graphicsStatus = value;
            return this;
        }

        Builder particleStatus(ParticleStatus value) {
            this.particleStatus = value;
            return this;
        }

        Builder cloudStatus(CloudStatus value) {
            this.cloudStatus = value;
            return this;
        }

        Builder entityShadows(boolean value) {
            this.entityShadows = value;
            return this;
        }

        Builder entityDistancePercent(int value) {
            this.entityDistancePercent = value;
            return this;
        }

        Builder biomeBlendRadius(int value) {
            this.biomeBlendRadius = value;
            return this;
        }

        Builder ambientOcclusion(int value) {
            this.ambientOcclusion = value;
            return this;
        }

        Builder mipmapLevels(int value) {
            this.mipmapLevels = value;
            return this;
        }
    }
}
