package net.vulkanmod.render.framegraph.radiance;

import net.vulkanmod.vulkan.shader.pipeline.GfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.PushConstantBlock;
import net.vulkanmod.vulkan.shader.pipeline.Sampler;
import net.vulkanmod.vulkan.shader.pipeline.Stage;
import net.vulkanmod.vulkan.shader.pipeline.Ubo;
import net.vulkanmod.vulkan.shader.pipeline.VertexFormatRef;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@GfxPipeline(basePath = "radiance_material", vertex = "radiance_material", fragment = "radiance_material", vertexFormat = VertexFormatRef.TERRAIN)
public final class RadianceMaterialPipeline implements PipelineDefinition {
    @Ubo(stage = Stage.VERTEX, binding = 0)
    static class VertexUbo {
        Matrix4f MVP;
        float WindTime;
        float WindStrength;
        Vector3f CameraWorldPos;
    }

    @Ubo(stage = Stage.FRAGMENT, binding = 1)
    static class FragUbo {
        Vector4f FogColor;
        float FogStart;
        float FogEnd;
        float AlphaCutout;
    }

    @PushConstantBlock
    static class Push {
        Vector3f ChunkOffset;
    }

    @Sampler(binding = 2) int Sampler0;
    @Sampler(binding = 3) int Sampler2;
}
