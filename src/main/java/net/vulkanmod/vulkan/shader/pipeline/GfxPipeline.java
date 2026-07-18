package net.vulkanmod.vulkan.shader.pipeline;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Descriptor for createPipeline() call. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GfxPipeline {
    /** Shader folder under {@code assets/vulkanmod/shaders/basic/}, also used for the pipeline cache name. */
    String basePath();

    /** Vertex shader file name (witht ext) inside {@link #basePath()}. */
    String vertex();

    /** Fragment shader file name (witht ext) inside {@link #basePath()}. */
    String fragment();

    VertexFormatRef vertexFormat();
}
