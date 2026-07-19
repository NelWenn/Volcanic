package net.vulkanmod.render.framegraph;

import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Pass {
    String name();

    Phase phase() default Phase.POST_PROCESS;

    Class<? extends PipelineDefinition> pipeline() default PipelineDefinition.class;

    Class<? extends PassExecutor> executor() default PassExecutor.class;
}
