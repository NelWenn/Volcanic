package net.vulkanmod.rendergraph.core.plugin;

import net.vulkanmod.Initializer;
import net.vulkanmod.config.plugin.PluginTargetType;
import net.vulkanmod.plugin.RenderPipelinePlugin;
import net.vulkanmod.render.framegraph.FrameGraph;
import net.vulkanmod.render.framegraph.FrameGraphImpl;
import net.vulkanmod.vulkan.pass.*;
import net.vulkanmod.vulkan.shader.PipelineManager;

public final class CorePipelinePlugin implements RenderPipelinePlugin {

    @Override
    public String id() {
        return "core";
    }

    @Override
    public String name() {
        return "Minecraft";
    }

    @Override
    public String description() {
        return "The Minecraft core shader";
    }

    @Override
    public String version() {
        return "1.0";
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
        return new String[]{ "RevoJava" };
    }

    @Override
    public PluginTargetType type() {
        return PluginTargetType.SHADERS;
    }

    @Override
    public PipelineManager createPipelineManager() {
        return new CorePipeline();
    }

    @Override
    public FrameGraphImpl createFrameGraph() {
        return new CoreGraph();
    }

    @Override
    public void configureGraph(FrameGraph graph, EngineContext context) {
        graph.setTargetScale("light", Initializer.CONFIG.optimizedShadows ? 0.5f : 1.0f);
        graph.setTargetScale("vtu", context.renderWidth() > 0
                ? (float) context.swapChain().getWidth() / context.renderWidth()
                : 1.0f);
    }
}
