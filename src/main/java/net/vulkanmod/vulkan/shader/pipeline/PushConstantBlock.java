package net.vulkanmod.vulkan.shader.pipeline;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a nested class as the pipeline's push-constant block; at most one is honored per pipeline. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PushConstantBlock {
}
