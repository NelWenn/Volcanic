package net.vulkanmod.render.plugin;

import net.vulkanmod.render.framegraph.FrameGraphImpl;
import net.vulkanmod.vulkan.shader.PipelineManager;

import java.util.ServiceLoader;

/**
 * Entry point a render pipeline jar implements and advertises via
 * {@code META-INF/services/net.vulkanmod.render.plugin.RenderPipelinePlugin} ({@link ServiceLoader}
 * discovery, one implementation per jar).
 * The jar is the pipeline: it bundles its own {@code PipelineDefinition} classes (annotated with
 * {@code @GfxPipeline}/{@code @Ubo}/{@code @Sampler}/{@code @PushConstantBlock})
 * next to their {@code .vsh}/{@code .fsh} sources under
 * {@code assets/vulkanmod/shaders/basic/<basePath>/}. Volcanic compiles them the same way it
 * compiles its own, nothing in {@code PipelineManager#init()} needs to know it's running from a
 * jar. Any other resource a pipeline needs (LUTs, noise textures, ...) can be bundled anywhere in
 * the jar and loaded, since the pipeline's own classes are already loaded from that jar.
 */
public interface RenderPipelinePlugin {
    String id();

    String name();

    PipelineManager createPipelineManager();

    FrameGraphImpl createFrameGraph();
}
