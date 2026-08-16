package net.vulkanmod.rendergraph.core.plugin.pipeline;

import net.vulkanmod.vulkan.shader.pipeline.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@GfxPipeline(basePath = "vsr_upscale", vertex = "vsr_upscale", fragment = "vsr_upscale", vertexFormat = VertexFormatRef.NONE)
public final class VsrUpscalePipeline implements PipelineDefinition {
    @Ubo(stage = Stage.FRAGMENT, binding = 0)
    static class FragUbo {
        Matrix4f FogInvMVPMat;
        Matrix4f FogPrevMVP;
        Vector3f FogCameraPos;
        Vector3f FogPrevCameraPos;
        Vector4f VsrInputInfo;
        Vector4f VsrOutputInfo;
        Vector4f VsrUvBounds;
        Vector4f VsrParams;
        Vector4f VsrJitter;
    }

    @Sampler(binding = 1) int Sampler0;
    @Sampler(binding = 2) int Sampler1;
    @Sampler(binding = 3) int Sampler2;
}
