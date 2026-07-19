package net.vulkanmod.vulkan.shader.pipeline.definitions.core;

import net.vulkanmod.vulkan.shader.pipeline.CoreGfxPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.Sampler;
import net.vulkanmod.vulkan.shader.pipeline.Stage;
import net.vulkanmod.vulkan.shader.pipeline.Ubo;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@CoreGfxPipeline(name = "rendertype_entity_cutout_no_cull_z_offset")
public final class EntityCutoutNoCullZOffsetPipeline implements PipelineDefinition {
    @Ubo(stage = Stage.VERTEX, binding = 0)
    static class VertexUbo {
        Matrix4f MVP;
        Vector3f Light0_Direction;
        Vector3f Light1_Direction;
    }

    @Ubo(stage = Stage.FRAGMENT, binding = 1)
    static class FragmentUbo {
        Vector4f ColorModulator;
        Vector4f FogColor;
        float FogStart;
        float FogEnd;
    }

    @Sampler(binding = 2) int Sampler0;
    @Sampler(binding = 3) int Sampler1;
    @Sampler(binding = 4) int Sampler2;
}
