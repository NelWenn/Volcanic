package net.vulkanmod.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.compat.external.ExternalRenderPathSupport;
import net.vulkanmod.render.chunk.build.thread.ThreadBuilderPack;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.SPIRVUtils;

import java.util.function.Function;

import static net.vulkanmod.vulkan.shader.SPIRVUtils.compileShaderAbsoluteFile;

public abstract class PipelineManager {
    private static final String shaderPath = "/assets/vulkanmod/shaders/";
    public static VertexFormat TERRAIN_VERTEX_FORMAT;

    public static void setTerrainVertexFormat(VertexFormat format) {
        TERRAIN_VERTEX_FORMAT = format;
    }

    static GraphicsPipeline terrainShaderEarlyZ, terrainShader, fastBlitPipeline, renderScaleBlitPipeline, externalLodPipeline;
    static GraphicsPipeline colorGradePipeline, fogPipeline, fogTermsPipeline, fogCompositePipeline, fogExposurePipeline;
    static GraphicsPipeline shadowTerrainSolidPipeline, shadowTerrainCutoutPipeline, shadowTerrainTintPipeline, shadowTerrainRsmPipeline, shadowTerrainRsmSolidPipeline;

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
        terrainShaderEarlyZ = createPipeline("terrain","terrain", "terrain_z", TERRAIN_VERTEX_FORMAT);
        terrainShader = createPipeline("terrain", "terrain", "terrain", TERRAIN_VERTEX_FORMAT);
        fastBlitPipeline = createPipeline("blit", "blit", "blit", CustomVertexFormat.NONE);
        renderScaleBlitPipeline = createPipeline("render_scale_blit", "render_scale_blit", "render_scale_blit", CustomVertexFormat.NONE);
        colorGradePipeline = createPipeline("post_color_grade", "post_color_grade", "post_color_grade", CustomVertexFormat.NONE);
        fogPipeline = createPipeline("post_fog", "post_fog", "post_fog", CustomVertexFormat.NONE);
        fogTermsPipeline = createPipeline("post_fog_terms", "post_fog_terms", "post_fog_terms", CustomVertexFormat.NONE);
        fogCompositePipeline = createPipeline("post_fog_composite", "post_fog_composite", "post_fog_composite", CustomVertexFormat.NONE);
        fogExposurePipeline = createPipeline("post_exposure", "post_exposure", "post_exposure", CustomVertexFormat.NONE);
        shadowTerrainSolidPipeline = createPipeline("shadow_terrain", "shadow_terrain", "shadow_terrain_solid", TERRAIN_VERTEX_FORMAT);
        shadowTerrainCutoutPipeline = createPipeline("shadow_terrain", "shadow_terrain", "shadow_terrain_cutout", TERRAIN_VERTEX_FORMAT);
        shadowTerrainTintPipeline = createPipeline("shadow_terrain", "shadow_terrain", "shadow_terrain_tint", TERRAIN_VERTEX_FORMAT);
        shadowTerrainRsmPipeline = createPipeline("shadow_terrain", "shadow_terrain", "shadow_terrain_rsm", TERRAIN_VERTEX_FORMAT);
        shadowTerrainRsmSolidPipeline = createPipeline("shadow_terrain", "shadow_terrain", "shadow_terrain_rsm_solid", TERRAIN_VERTEX_FORMAT);
        if (ExternalRenderPathSupport.shouldCreateExternalLodPipeline()) {
            externalLodPipeline = createPipeline("external_lod", "lod", "lod", CustomVertexFormat.EXTERNAL_LOD);
        }
    }

    private static GraphicsPipeline createPipeline(String baseName, String vertName, String fragName,VertexFormat vertexFormat) {
        String pathB = String.format("basic/%s/%s", baseName, baseName);
        String pathV = String.format("basic/%s/%s", baseName, vertName);
        String pathF = String.format("basic/%s/%s", baseName, fragName);

        Pipeline.Builder pipelineBuilder = new Pipeline.Builder(vertexFormat, pathB);
        pipelineBuilder.parseBindingsJSON();

        SPIRVUtils.SPIRV vertShaderSPIRV = compileShaderAbsoluteFile(String.format("%s%s.vsh", shaderPath, pathV), SPIRVUtils.ShaderKind.VERTEX_SHADER);
        SPIRVUtils.SPIRV fragShaderSPIRV = compileShaderAbsoluteFile(String.format("%s%s.fsh", shaderPath, pathF), SPIRVUtils.ShaderKind.FRAGMENT_SHADER);
        pipelineBuilder.setSPIRVs(vertShaderSPIRV, fragShaderSPIRV);

        return pipelineBuilder.createGraphicsPipeline();
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

    public static GraphicsPipeline getShadowTerrainRsmShader(TerrainRenderType renderType) {
        return renderType == TerrainRenderType.SOLID ? shadowTerrainRsmSolidPipeline : shadowTerrainRsmPipeline;
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

    public static GraphicsPipeline getPostShaderPipeline(String shaderId) {
        return switch (shaderId) {
            case "color_grade" -> colorGradePipeline;
            case "fog" -> fogPipeline;
            case "radiance" -> fogPipeline;
            default -> null;
        };
    }

    public static GraphicsPipeline getFogTermsPipeline() { return fogTermsPipeline; }

    public static GraphicsPipeline getFogCompositePipeline() { return fogCompositePipeline; }

    public static GraphicsPipeline getFogExposurePipeline() { return fogExposurePipeline; }

    public static GraphicsPipeline getExternalLodPipeline() { return externalLodPipeline; }

    public static void destroyPipelines() {
        terrainShaderEarlyZ.cleanUp();
        terrainShader.cleanUp();
        fastBlitPipeline.cleanUp();
        renderScaleBlitPipeline.cleanUp();
        if (colorGradePipeline != null) {
            colorGradePipeline.cleanUp();
        }
        if (fogPipeline != null) {
            fogPipeline.cleanUp();
        }
        if (fogTermsPipeline != null) {
            fogTermsPipeline.cleanUp();
        }
        if (fogCompositePipeline != null) {
            fogCompositePipeline.cleanUp();
        }
        if (fogExposurePipeline != null) {
            fogExposurePipeline.cleanUp();
        }
        if (externalLodPipeline != null) {
            externalLodPipeline.cleanUp();
        }
        if (shadowTerrainSolidPipeline != null) {
            shadowTerrainSolidPipeline.cleanUp();
        }
        if (shadowTerrainCutoutPipeline != null) {
            shadowTerrainCutoutPipeline.cleanUp();
        }
        if (shadowTerrainTintPipeline != null) {
            shadowTerrainTintPipeline.cleanUp();
        }
        if (shadowTerrainRsmPipeline != null) {
            shadowTerrainRsmPipeline.cleanUp();
        }
        if (shadowTerrainRsmSolidPipeline != null) {
            shadowTerrainRsmSolidPipeline.cleanUp();
        }
    }
}
