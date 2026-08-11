package net.vulkanmod.render.framegraph;

import net.vulkanmod.render.framegraph.targets.RenderBucket;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//TODO: add autoregistry of pipeline holded by the bucket
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Bucket {
    RenderBucket type();
    Class<? extends PipelineDefinition> pipeline();
}
