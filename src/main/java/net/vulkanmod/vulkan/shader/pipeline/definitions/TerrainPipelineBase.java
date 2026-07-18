package net.vulkanmod.vulkan.shader.pipeline.definitions;

import net.vulkanmod.vulkan.shader.pipeline.PushConstantBlock;
import net.vulkanmod.vulkan.shader.pipeline.Sampler;
import net.vulkanmod.vulkan.shader.pipeline.Stage;
import net.vulkanmod.vulkan.shader.pipeline.Ubo;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Bindings shared by the terrain draw pipeline. */
abstract class TerrainPipelineBase {

    @Ubo(stage = Stage.VERTEX, binding = 0)
    static class VertexUbo {
        Matrix4f MVP;
        float WindTime;
        float WindStrength;
        Vector3f CameraWorldPos;
    }

    @Ubo(stage = Stage.FRAGMENT, binding = 1)
    static class FragmentUbo {
        Vector4f FogColor;
        float FogStart;
        float FogEnd;
        float AlphaCutout;
    }

    @PushConstantBlock
    static class PushConstantsBlock {
        Vector3f ChunkOffset;
    }

    @Sampler(binding = 2) int Sampler0;
    @Sampler(binding = 3) int Sampler2;
}
