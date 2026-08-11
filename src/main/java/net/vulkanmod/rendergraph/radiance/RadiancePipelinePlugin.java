package net.vulkanmod.rendergraph.radiance;

import net.vulkanmod.render.framegraph.FrameGraphImpl;
import net.vulkanmod.render.plugin.RenderPipelinePlugin;
import net.vulkanmod.vulkan.shader.PipelineManager;

public final class RadiancePipelinePlugin implements RenderPipelinePlugin {
    @Override
    public String id() {
        return "radiance";
    }

    @Override
    public String name() {
        return "Radiance";
    }

    @Override
    public PipelineManager createPipelineManager() {
        return new RadiancePipeline();
    }

    @Override
    public FrameGraphImpl createFrameGraph() {
        return new RadianceGraph();
    }
}
