package net.vulkanmod.vulkan.shader.pipeline;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Descriptor {@code ShaderInstance} shader.
 * Unlike {@link GfxPipeline}, the vertex format is supplied at runtime by the vanilla shader loader
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CoreGfxPipeline {
    /** Shader name matches the file name {@code assets/vulkanmod/shaders/minecraft/core/}. */
    String name();
}
