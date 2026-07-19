package net.vulkanmod.render.framegraph;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Output {
    String value();

    Format format() default Format.RGBA16F;

    float scale() default 1.0f;

    float clear() default 0.0f;

    boolean pingpong() default false;
}
