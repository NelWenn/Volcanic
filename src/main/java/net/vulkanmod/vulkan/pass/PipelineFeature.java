package net.vulkanmod.vulkan.pass;

/**
 * A selfcontained rendering feature owned by a pipeline plugin
 */
public interface PipelineFeature {

    default void initialize(EngineContext context) {}
    default void cleanup() {}

    boolean isReady();
}
