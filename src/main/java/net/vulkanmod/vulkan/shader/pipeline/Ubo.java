package net.vulkanmod.vulkan.shader.pipeline;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marks nested UBO */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Ubo {
    Stage stage();

    int binding();
}
