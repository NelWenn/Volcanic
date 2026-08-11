package net.vulkanmod.rendergraph.radiance;

import net.vulkanmod.render.chunk.build.thread.ThreadBuilderPack;
import net.vulkanmod.render.sodium.SodiumShaderBridge;
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
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.PipelineManager;
import net.vulkanmod.vulkan.shader.pipeline.PipelineRegistry;

public class RadiancePipeline extends PipelineManager {
    private GraphicsPipeline terrainShaderEarlyZ, terrainShader, terrainFadeShader, fastBlitPipeline, renderScaleBlitPipeline, externalLodPipeline, externalLodTexturedPipeline, externalLodWaterPipeline, externalLodWaterTexturedPipeline, externalLodSolidPipeline, externalLodTexturedSolidPipeline;
    private GraphicsPipeline shadowTerrainSolidPipeline, shadowTerrainCutoutPipeline, shadowTerrainTintPipeline;
    private GraphicsPipeline materialPipeline;

    @Override
    public void init() {
        setTerrainVertexFormat(CustomVertexFormat.COMPRESSED_TERRAIN);
        createBasicPipelines();
        setDefaultShader();
        ThreadBuilderPack.defaultTerrainBuilderConstructor();
    }

    @Override
    public void setDefaultShader() {
        setShaderGetter(renderType -> terrainShader);
    }

    private void createBasicPipelines() {
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

        registerTerrainRole(ROLE_TERRAIN_MAIN, this::getTerrainShader);
        registerTerrainRole(ROLE_TERRAIN_FADE, renderType -> terrainFadeShader);
        registerTerrainRole(ROLE_SHADOW_TERRAIN, this::getShadowTerrainShader);
        registerTerrainRole(ROLE_SHADOW_TINT, renderType -> shadowTerrainTintPipeline);
        registerTerrainRole(ROLE_MATERIAL, renderType -> materialPipeline);

        registerPipeline(PIPELINE_FAST_BLIT, fastBlitPipeline);
        registerPipeline(PIPELINE_RENDER_SCALE_BLIT, renderScaleBlitPipeline);

        PipelineRegistry.register(ExternalLodPipeline.class);
        externalLodPipeline = PipelineRegistry.get(ExternalLodPipeline.class);
        registerPipeline(PIPELINE_EXTERNAL_LOD, externalLodPipeline);

        PipelineRegistry.register(ExternalLodTexturedPipeline.class);
        externalLodTexturedPipeline = PipelineRegistry.get(ExternalLodTexturedPipeline.class);
        registerPipeline(PIPELINE_EXTERNAL_LOD_TEXTURED, externalLodTexturedPipeline);

        PipelineRegistry.register(ExternalLodWaterPipeline.class);
        externalLodWaterPipeline = PipelineRegistry.get(ExternalLodWaterPipeline.class);
        registerPipeline(PIPELINE_EXTERNAL_LOD_WATER, externalLodWaterPipeline);

        PipelineRegistry.register(ExternalLodWaterTexturedPipeline.class);
        externalLodWaterTexturedPipeline = PipelineRegistry.get(ExternalLodWaterTexturedPipeline.class);
        registerPipeline(PIPELINE_EXTERNAL_LOD_WATER_TEXTURED, externalLodWaterTexturedPipeline);

        PipelineRegistry.register(ExternalLodSolidPipeline.class);
        externalLodSolidPipeline = PipelineRegistry.get(ExternalLodSolidPipeline.class);
        registerPipeline(PIPELINE_EXTERNAL_LOD_SOLID, externalLodSolidPipeline);

        PipelineRegistry.register(ExternalLodTexturedSolidPipeline.class);
        externalLodTexturedSolidPipeline = PipelineRegistry.get(ExternalLodTexturedSolidPipeline.class);
        registerPipeline(PIPELINE_EXTERNAL_LOD_TEXTURED_SOLID, externalLodTexturedSolidPipeline);
    }

    public GraphicsPipeline getTerrainShader(TerrainRenderType renderType) {
        GraphicsPipeline sodiumPipeline = SodiumShaderBridge.getPipeline(renderType);
        return sodiumPipeline != null ? sodiumPipeline : shaderGetter.apply(renderType);
    }

    public GraphicsPipeline getShadowTerrainShader(TerrainRenderType renderType) {
        return renderType == TerrainRenderType.SOLID ? shadowTerrainSolidPipeline : shadowTerrainCutoutPipeline;
    }
}
