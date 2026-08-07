package net.vulkanmod.rendergraph.radiance.pass;

import net.vulkanmod.render.framegraph.Format;
import net.vulkanmod.render.framegraph.Input;
import net.vulkanmod.render.framegraph.Output;
import net.vulkanmod.render.framegraph.Pass;
import net.vulkanmod.render.framegraph.Texture;
import net.vulkanmod.rendergraph.radiance.pipeline.RadianceAaPipeline;

@Pass(name = "aa", pipeline = RadianceAaPipeline.class)
public final class RadianceAaPass {
    @Input("aatex")    Texture aa;
    @Input("depthtex") Texture depth;
    @Input("fgdepth")  Texture fgDepth;

    @Output(value = "vsrtex", format = Format.RGBA8) Texture out;
}
