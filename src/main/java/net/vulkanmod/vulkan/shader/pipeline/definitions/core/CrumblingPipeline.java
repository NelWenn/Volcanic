package net.vulkanmod.vulkan.shader.pipeline.definitions.core;

import net.vulkanmod.vulkan.shader.pipeline.CoreGfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.Sampler;
import net.vulkanmod.vulkan.shader.pipeline.Stage;
import net.vulkanmod.vulkan.shader.pipeline.Ubo;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@CoreGfxPipeline(name = "rendertype_crumbling")
public final class CrumblingPipeline implements PipelineDefinition {
    @Ubo(stage = Stage.VERTEX, binding = 0)
    static class VertexUbo {
        Matrix4f MVP;
    }

    @Ubo(stage = Stage.FRAGMENT, binding = 1)
    static class FragmentUbo {
        Vector4f ColorModulator;
    }

    @Sampler(binding = 2) int Sampler0;
}
