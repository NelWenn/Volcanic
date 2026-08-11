package net.vulkanmod.rendergraph.radiance;

import net.vulkanmod.config.plugin.PluginTargetType;
import net.vulkanmod.render.framegraph.FrameGraphImpl;
import net.vulkanmod.plugin.RenderPipelinePlugin;
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
    public String description() {
        return "The volcanic's official shader";
    }

    @Override
    public String version() {
        return "0.1-alpha";
    }

    @Override
    public String byline() {
        return ""; //TODO: complete byLine
    }

    @Override
    public String icon() {
        return ""; //TODO: complete icon path
    }

    @Override
    public String banner() {
        return ""; //TODO: complete banner path
    }

    @Override
    public String[] tags() {
        return new String[] { "shader", "visuals", "rendering" };
    }

    @Override
    public String[] authors() {
        return new String[]{ "NelWenn", "RevoJava" };
    }

    @Override
    public PluginTargetType type() {
        return PluginTargetType.SHADERS;
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
