package net.vulkanmod.vulkan.shader.pipeline;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Only required on {@code float[]} fields, to declare count. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Uniform {
    int count() default -1;
}
