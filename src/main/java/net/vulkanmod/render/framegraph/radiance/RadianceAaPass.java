package net.vulkanmod.render.framegraph.radiance;

import net.vulkanmod.render.framegraph.Input;
import net.vulkanmod.render.framegraph.Output;
import net.vulkanmod.render.framegraph.Pass;
import net.vulkanmod.render.framegraph.Texture;

@Pass(name = "aa", pipeline = RadianceAaPipeline.class)
public final class RadianceAaPass {
    @Input("aatex")    Texture aa;
    @Input("depthtex") Texture depth;
    @Input("fgdepth")  Texture fgDepth;

    @Output("swapchain") Texture out;
}
