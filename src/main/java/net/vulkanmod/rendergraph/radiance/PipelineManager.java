package net.vulkanmod.rendergraph.radiance;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.render.chunk.build.thread.ThreadBuilderPack;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.rendergraph.radiance.pipeline.*;
import net.vulkanmod.rendergraph.radiance.pipeline.composite.RadianceCompositePipeline;
import net.vulkanmod.rendergraph.radiance.pipeline.lod.*;
import net.vulkanmod.rendergraph.radiance.pipeline.shadow.ShadowTerrainCutoutPipeline;
import net.vulkanmod.rendergraph.radiance.pipeline.shadow.ShadowTerrainSolidPipeline;
import net.vulkanmod.rendergraph.radiance.pipeline.shadow.ShadowTerrainTintPipeline;
import net.vulkanmod.rendergraph.radiance.pipeline.terrain.TerrainEarlyZPipeline;
import net.vulkanmod.rendergraph.radiance.pipeline.terrain.TerrainFadePipeline;
import net.vulkanmod.rendergraph.radiance.pipeline.terrain.TerrainPipeline;
import net.vulkanmod.render.pipeline.RenderPipeline;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class PipelineManager {
    public static VertexFormat TERRAIN_VERTEX_FORMAT;

    public static void setTerrainVertexFormat(VertexFormat format) {
        TERRAIN_VERTEX_FORMAT = format;
    }

    public static final String ROLE_TERRAIN_MAIN = "terrain.main";
    public static final String ROLE_TERRAIN_FADE = "terrain.fade";
    public static final String ROLE_SHADOW_TERRAIN = "terrain.shadow";
    public static final String ROLE_SHADOW_TINT = "terrain.shadow_tint";
    public static final String ROLE_MATERIAL = "terrain.material";

    private static final Map<String, Function<TerrainRenderType, GraphicsPipeline>> terrainRoleProviders = new HashMap<>();

    public static void registerTerrainRole(String role, Function<TerrainRenderType, GraphicsPipeline> provider) {
        terrainRoleProviders.put(role, provider);
    }

    public static RenderPipeline getPipeline(String role, TerrainRenderType renderType) {
        Function<TerrainRenderType, GraphicsPipeline> provider = terrainRoleProviders.get(role);
        if (provider == null)
            throw new IllegalStateException("No pipeline registered for role: " + role);
        return provider.apply(renderType);
    }

    static GraphicsPipeline terrainShaderEarlyZ, terrainShader, terrainFadeShader, fastBlitPipeline, renderScaleBlitPipeline, externalLodPipeline, externalLodTexturedPipeline, externalLodWaterPipeline, externalLodWaterTexturedPipeline, externalLodSolidPipeline, externalLodTexturedSolidPipeline;
    static GraphicsPipeline shadowTerrainSolidPipeline, shadowTerrainCutoutPipeline, shadowTerrainTintPipeline;
    static GraphicsPipeline materialPipeline;

    private static Function<TerrainRenderType, GraphicsPipeline> shaderGetter;

    public static void init() {
        setTerrainVertexFormat(CustomVertexFormat.COMPRESSED_TERRAIN);
        createBasicPipelines();
        setDefaultShader();
        ThreadBuilderPack.defaultTerrainBuilderConstructor();
    }

    public static void setDefaultShader() {
        setShaderGetter(renderType -> terrainShader);
    }

    private static void createBasicPipelines() {
        PipelineRegistry.register(
                TerrainPipeline.class,
                TerrainEarlyZPipeline.class,
                TerrainFadePipeline.class,
                FastBlitPipeline.class,
                RenderScaleBlitPipeline.class,
                ShadowTerrainSolidPipeline.class,
                ShadowTerrainCutoutPipeline.class,
                ShadowTerrainTintPipeline.class,
                RadianceLightPipeline.class,
                RadianceReflectionPipeline.class,
                RadianceMaterialPipeline.class,
                RadianceGlassReflectionPipeline.class,
                RadianceCompositePipeline.class,
                RadianceAaPipeline.class,
                RadianceOpaqueTintPipeline.class
        );

        terrainShaderEarlyZ = PipelineRegistry.get(TerrainEarlyZPipeline.class);
        terrainShader = PipelineRegistry.get(TerrainPipeline.class);
        terrainFadeShader = PipelineRegistry.get(TerrainFadePipeline.class);
        fastBlitPipeline = PipelineRegistry.get(FastBlitPipeline.class);
        renderScaleBlitPipeline = PipelineRegistry.get(RenderScaleBlitPipeline.class);
        shadowTerrainSolidPipeline = PipelineRegistry.get(ShadowTerrainSolidPipeline.class);
        shadowTerrainCutoutPipeline = PipelineRegistry.get(ShadowTerrainCutoutPipeline.class);
        shadowTerrainTintPipeline = PipelineRegistry.get(ShadowTerrainTintPipeline.class);
        materialPipeline = PipelineRegistry.get(RadianceMaterialPipeline.class);

        registerTerrainRole(ROLE_TERRAIN_MAIN, PipelineManager::getTerrainShader);
        registerTerrainRole(ROLE_TERRAIN_FADE, renderType -> terrainFadeShader);
        registerTerrainRole(ROLE_SHADOW_TERRAIN, PipelineManager::getShadowTerrainShader);
        registerTerrainRole(ROLE_SHADOW_TINT, renderType -> shadowTerrainTintPipeline);
        registerTerrainRole(ROLE_MATERIAL, renderType -> materialPipeline);

        PipelineRegistry.register(ExternalLodPipeline.class);
        externalLodPipeline = PipelineRegistry.get(ExternalLodPipeline.class);

        PipelineRegistry.register(ExternalLodTexturedPipeline.class);
        externalLodTexturedPipeline = PipelineRegistry.get(ExternalLodTexturedPipeline.class);

        PipelineRegistry.register(ExternalLodWaterPipeline.class);
        externalLodWaterPipeline = PipelineRegistry.get(ExternalLodWaterPipeline.class);

        PipelineRegistry.register(ExternalLodWaterTexturedPipeline.class);
        externalLodWaterTexturedPipeline = PipelineRegistry.get(ExternalLodWaterTexturedPipeline.class);

        PipelineRegistry.register(ExternalLodSolidPipeline.class);
        externalLodSolidPipeline = PipelineRegistry.get(ExternalLodSolidPipeline.class);

        PipelineRegistry.register(ExternalLodTexturedSolidPipeline.class);
        externalLodTexturedSolidPipeline = PipelineRegistry.get(ExternalLodTexturedSolidPipeline.class);
    }

    public static GraphicsPipeline getTerrainShader(TerrainRenderType renderType) {
        GraphicsPipeline sodiumPipeline = net.vulkanmod.render.sodium.SodiumShaderBridge.getPipeline(renderType);
        return sodiumPipeline != null ? sodiumPipeline : shaderGetter.apply(renderType);
    }

    public static GraphicsPipeline getNativeTerrainShader() {
        return terrainShader;
    }

    public static GraphicsPipeline getTerrainFadeShader() {
        return terrainFadeShader;
    }

    public static GraphicsPipeline getShadowTerrainShader(TerrainRenderType renderType) {
        return renderType == TerrainRenderType.SOLID ? shadowTerrainSolidPipeline : shadowTerrainCutoutPipeline;
    }

    public static GraphicsPipeline getShadowTerrainTintShader() {
        return shadowTerrainTintPipeline;
    }

    public static GraphicsPipeline getMaterialShader() {
        return materialPipeline;
    }

    public static void setShaderGetter(Function<TerrainRenderType, GraphicsPipeline> consumer) {
        shaderGetter = consumer;
    }

    public static GraphicsPipeline getTerrainDirectShader(RenderType renderType) {
        return terrainShader;
    }

    public static GraphicsPipeline getTerrainIndirectShader(RenderType renderType) {
        return terrainShaderEarlyZ;
    }

    public static GraphicsPipeline getFastBlitPipeline() { return fastBlitPipeline; }

    public static GraphicsPipeline getRenderScaleBlitPipeline() { return renderScaleBlitPipeline; }

    public static GraphicsPipeline getExternalLodPipeline() { return externalLodPipeline; }

    public static GraphicsPipeline getExternalLodTexturedPipeline() { return externalLodTexturedPipeline; }

    public static GraphicsPipeline getExternalLodWaterPipeline() { return externalLodWaterPipeline; }

    public static GraphicsPipeline getExternalLodWaterTexturedPipeline() { return externalLodWaterTexturedPipeline; }

    public static GraphicsPipeline getExternalLodSolidPipeline() { return externalLodSolidPipeline; }

    public static GraphicsPipeline getExternalLodTexturedSolidPipeline() { return externalLodTexturedSolidPipeline; }

    public static void destroyPipelines() {
        PipelineRegistry.cleanUp();
    }
}
