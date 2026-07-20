package net.vulkanmod.render.framegraph.radiance;

import net.vulkanmod.render.framegraph.Input;
import net.vulkanmod.render.framegraph.Output;
import net.vulkanmod.render.framegraph.Pass;
import net.vulkanmod.render.framegraph.Texture;

@Pass(name = "water_reflection", pipeline = RadianceReflectionPipeline.class)
public final class RadianceReflectionPass {
    @Input("scene")       Texture scene;
    @Input("depthtex")    Texture depth;
    @Input("fgdepth")     Texture fgDepth;
    @Input("opaquedepth") Texture opaqueDepth;
    @Input("material")    Texture material;

    @Output(value = "reflection", scale = 0.4f) Texture reflection;
}
