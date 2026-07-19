package net.vulkanmod.vulkan.shader.pipeline.definitions.core;

import net.vulkanmod.vulkan.shader.pipeline.CoreGfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.Sampler;
import net.vulkanmod.vulkan.shader.pipeline.Stage;
import net.vulkanmod.vulkan.shader.pipeline.Ubo;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@CoreGfxPipeline(name = "rendertype_beacon_beam")
public final class BeaconBeamPipeline implements PipelineDefinition {
    @Ubo(stage = Stage.ALL, binding = 0)
    static class SharedUbo {
        Matrix4f MVP;
        Matrix4f ProjMat;
    }

    @Ubo(stage = Stage.FRAGMENT, binding = 1)
    static class FragmentUbo {
        Vector4f ColorModulator;
        Vector4f FogColor;
        float FogStart;
        float FogEnd;
    }

    @Sampler(binding = 2) int Sampler0;
}
