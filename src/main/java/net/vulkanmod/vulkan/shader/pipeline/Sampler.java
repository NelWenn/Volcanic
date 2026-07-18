package net.vulkanmod.vulkan.shader.pipeline;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks a field as a sampler */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Sampler {
    int binding();
}
