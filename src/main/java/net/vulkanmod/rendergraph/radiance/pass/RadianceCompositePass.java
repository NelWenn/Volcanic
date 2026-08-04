package net.vulkanmod.rendergraph.radiance.pass;

import net.vulkanmod.render.framegraph.Input;
import net.vulkanmod.render.framegraph.Output;
import net.vulkanmod.render.framegraph.Pass;
import net.vulkanmod.render.framegraph.Texture;
import net.vulkanmod.rendergraph.radiance.pipeline.composite.RadianceCompositePipeline;

@Pass(name = "composite", pipeline = RadianceCompositePipeline.class)
public final class RadianceCompositePass {
    @Input("scene")       Texture scene;
    @Input("depthtex")    Texture depth;
    @Input("fgdepth")     Texture fgDepth;
    @Input("light")       Texture light;
    @Input("reflection")  Texture reflection;
    @Input("opaquedepth") Texture opaqueDepth;
    @Input("glassreflection") Texture glassReflection;
    @Input("material")    Texture material;
    @Input("gnormal")     Texture gnormal;

    @Output("aatex") Texture aa;
}
