package net.vulkanmod.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.compat.external.ExternalRenderPathSupport;
import net.vulkanmod.render.chunk.build.thread.ThreadBuilderPack;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineRegistry;
import net.vulkanmod.vulkan.shader.pipeline.definitions.*;
import net.vulkanmod.render.framegraph.radiance.*;

import java.util.function.Function;

public abstract class PipelineManager {
    public static VertexFormat TERRAIN_VERTEX_FORMAT;

    public static void setTerrainVertexFormat(VertexFormat format) {
        TERRAIN_VERTEX_FORMAT = format;
    }

    static GraphicsPipeline terrainShaderEarlyZ, terrainShader, fastBlitPipeline, renderScaleBlitPipeline, externalLodPipeline;
    static GraphicsPipeline shadowTerrainSolidPipeline, shadowTerrainCutoutPipeline, shadowTerrainTintPipeline;

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
                FastBlitPipeline.class,
                RenderScaleBlitPipeline.class,
                ShadowTerrainSolidPipeline.class,
                ShadowTerrainCutoutPipeline.class,
                ShadowTerrainTintPipeline.class,
                RadianceLightPipeline.class,
                RadianceReflectionPipeline.class,
                RadianceCompositePipeline.class,
                RadianceAaPipeline.class,
                RadianceOpaqueTintPipeline.class
        );

        terrainShaderEarlyZ = PipelineRegistry.get(TerrainEarlyZPipeline.class);
        terrainShader = PipelineRegistry.get(TerrainPipeline.class);
        fastBlitPipeline = PipelineRegistry.get(FastBlitPipeline.class);
        renderScaleBlitPipeline = PipelineRegistry.get(RenderScaleBlitPipeline.class);
        shadowTerrainSolidPipeline = PipelineRegistry.get(ShadowTerrainSolidPipeline.class);
        shadowTerrainCutoutPipeline = PipelineRegistry.get(ShadowTerrainCutoutPipeline.class);
        shadowTerrainTintPipeline = PipelineRegistry.get(ShadowTerrainTintPipeline.class);

        if (ExternalRenderPathSupport.shouldCreateExternalLodPipeline()) {
            PipelineRegistry.register(ExternalLodPipeline.class);
            externalLodPipeline = PipelineRegistry.get(ExternalLodPipeline.class);
        }
    }

    public static GraphicsPipeline getTerrainShader(TerrainRenderType renderType) {
        return shaderGetter.apply(renderType);
    }

    public static GraphicsPipeline getShadowTerrainShader(TerrainRenderType renderType) {
        return renderType == TerrainRenderType.SOLID ? shadowTerrainSolidPipeline : shadowTerrainCutoutPipeline;
    }

    public static GraphicsPipeline getShadowTerrainTintShader() {
        return shadowTerrainTintPipeline;
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

    public static void destroyPipelines() {
        PipelineRegistry.cleanUp();
    }
}
