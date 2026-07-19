package net.vulkanmod.vulkan.shader.pipeline.definitions.radiance;

import net.vulkanmod.vulkan.shader.pipeline.GfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.Sampler;
import net.vulkanmod.vulkan.shader.pipeline.Stage;
import net.vulkanmod.vulkan.shader.pipeline.Ubo;
import net.vulkanmod.vulkan.shader.pipeline.VertexFormatRef;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@GfxPipeline(basePath = "radiance_opaque_tint", vertex = "radiance_opaque_tint", fragment = "radiance_opaque_tint", vertexFormat = VertexFormatRef.NONE)
public final class RadianceOpaqueTintPipeline implements PipelineDefinition {
    @Ubo(stage = Stage.FRAGMENT, binding = 0)
    static class FragUbo {
        Matrix4f FogInvMVPMat;
        Matrix4f FogShadowMVP0;
        Matrix4f FogShadowMVP1;
        Matrix4f FogShadowMVP2;
        Vector3f FogCameraPos;
        float FogColoredShadows;
        Vector3f FogShadowCameraPos;
        Vector3f FogShadowSplits;
    }

    @Sampler(binding = 1) int Sampler0;
    @Sampler(binding = 2) int Sampler1;
    @Sampler(binding = 3) int Sampler2;
    @Sampler(binding = 4) int Sampler3;
}
