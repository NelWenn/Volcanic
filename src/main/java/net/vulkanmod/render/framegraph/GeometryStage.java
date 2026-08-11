package net.vulkanmod.render.framegraph;

import net.vulkanmod.render.framegraph.targets.Viewpoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// TODO: implement GeometryStage usage into the current pipeline, I need to also add an abstraction to interface Vulkan with a public simple API
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GeometryStage {
    String name();
    Phase phase();
    Viewpoint viewpoint() default Viewpoint.MAIN_CAMERA;
    Bucket[] buckets();
}