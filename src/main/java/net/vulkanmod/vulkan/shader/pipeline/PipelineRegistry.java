package net.vulkanmod.vulkan.shader.pipeline;

import net.vulkanmod.vulkan.shader.GraphicsPipeline;

import java.util.IdentityHashMap;
import java.util.Map;

/** Builds and caches pipelines from their {@link PipelineDefinition} class. */
public final class PipelineRegistry {
    private static final Map<Class<? extends PipelineDefinition>, GraphicsPipeline> PIPELINES = new IdentityHashMap<>();

    private PipelineRegistry() {
    }

    @SafeVarargs
    public static void register(Class<? extends PipelineDefinition>... defs) {
        for (Class<? extends PipelineDefinition> def : defs)
            PIPELINES.put(def, PipelineFactory.build(def));
    }

    public static GraphicsPipeline get(Class<? extends PipelineDefinition> def) {
        GraphicsPipeline pipeline = PIPELINES.get(def);
        if (pipeline == null)
            throw new IllegalStateException("Pipeline not registered: " + def.getName());
        return pipeline;
    }

    public static GraphicsPipeline getOrNull(Class<? extends PipelineDefinition> def) {
        return PIPELINES.get(def);
    }

    public static void cleanUp() {
        PIPELINES.values().forEach(GraphicsPipeline::cleanUp);
        PIPELINES.clear();
    }
}
