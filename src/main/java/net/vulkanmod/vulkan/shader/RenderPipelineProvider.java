package net.vulkanmod.vulkan.shader;

import net.vulkanmod.plugin.PluginRegistry;
import net.vulkanmod.plugin.RenderPipelinePlugin;
import net.vulkanmod.render.framegraph.FrameGraphImpl;

import java.util.function.Supplier;

/**
 * Everything the engine needs from a complete, compiled-in render pipeline implementation: its
 * {@link PipelineManager} (terrain/shadow/material shaders and named pipelines) and its
 * {@link FrameGraphImpl} (the post-process frame graph it drives). Register one via
 * {@link PluginRegistry#register} to make it selectable like Radiance.
 */
public record RenderPipelineProvider(
        String id,
        Supplier<PipelineManager> pipelineManager,
        Supplier<FrameGraphImpl> frameGraph,
        RenderPipelinePlugin plugin
) {
}
