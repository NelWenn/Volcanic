package net.vulkanmod.render.framegraph.radiance;

import net.vulkanmod.render.framegraph.Input;
import net.vulkanmod.render.framegraph.Output;
import net.vulkanmod.render.framegraph.Pass;
import net.vulkanmod.render.framegraph.Texture;

@Pass(name = "light", pipeline = RadianceLightPipeline.class)
public final class RadianceLightPass {
    @Input("depthtex")      Texture depth;
    @Input("fgdepth")       Texture fgDepth;
    @Input("shadowtex0")    Texture shadow0;
    @Input("shadowtex1")    Texture shadow1;
    @Input("shadowtex2")    Texture shadow2;
    @Input("light_history") Texture history;
    @Input("gnormal")       Texture gnormal;

    @Output(value = "light", clear = 1.0f, pingpong = true) Texture light;
}
