package net.vulkanmod.render.framegraph.radiance;

import net.vulkanmod.render.framegraph.Input;
import net.vulkanmod.render.framegraph.Output;
import net.vulkanmod.render.framegraph.Pass;
import net.vulkanmod.render.framegraph.Texture;

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

    @Output("aatex") Texture aa;
}
