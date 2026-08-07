package net.vulkanmod.rendergraph.radiance.pass;

import net.vulkanmod.render.framegraph.Input;
import net.vulkanmod.render.framegraph.Output;
import net.vulkanmod.render.framegraph.Pass;
import net.vulkanmod.render.framegraph.Texture;
import net.vulkanmod.rendergraph.radiance.pipeline.RadianceGlassReflectionPipeline;

@Pass(name = "glass_reflection", pipeline = RadianceGlassReflectionPipeline.class)
public final class RadianceGlassReflectionPass {
    @Input("scene")         Texture scene;
    @Input("opaquedepth")   Texture opaqueDepth;
    @Input("material")      Texture material;
    @Input("materialdepth") Texture materialDepth;

    @Output("glassreflection") Texture glassReflection;
}
