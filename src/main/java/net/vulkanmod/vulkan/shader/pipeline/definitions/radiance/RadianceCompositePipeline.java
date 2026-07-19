package net.vulkanmod.vulkan.shader.pipeline.definitions.radiance;

import net.vulkanmod.vulkan.shader.pipeline.GfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.Sampler;
import net.vulkanmod.vulkan.shader.pipeline.Stage;
import net.vulkanmod.vulkan.shader.pipeline.Ubo;
import net.vulkanmod.vulkan.shader.pipeline.VertexFormatRef;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@GfxPipeline(basePath = "radiance_composite", vertex = "radiance_composite", fragment = "radiance_composite", vertexFormat = VertexFormatRef.NONE)
public final class RadianceCompositePipeline implements PipelineDefinition {
    @Ubo(stage = Stage.FRAGMENT, binding = 0)
    static class FragUbo {
        Matrix4f FogInvMVPMat;
        Matrix4f FogMVPMat;
        Vector3f FogSunDir;
        float FogShadowIntensity;
    }

    @Sampler(binding = 1) int Sampler0;
    @Sampler(binding = 2) int Sampler1;
    @Sampler(binding = 3) int Sampler2;
    @Sampler(binding = 4) int Sampler3;
    @Sampler(binding = 5) int Sampler4;
}
