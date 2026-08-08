package net.vulkanmod.rendergraph.radiance.pass;

import net.vulkanmod.render.framegraph.Format;
import net.vulkanmod.render.framegraph.Input;
import net.vulkanmod.render.framegraph.Output;
import net.vulkanmod.render.framegraph.Pass;
import net.vulkanmod.render.framegraph.Texture;
import net.vulkanmod.rendergraph.radiance.pipeline.VsrUpscalePipeline;

@Pass(name = "vsr", pipeline = VsrUpscalePipeline.class)
public final class VsrUpscalePass {
    @Input("vsrtex")      Texture scene;
    @Input("depthtex")    Texture depth;
    @Input("vtu_history") Texture history;

    @Output(value = "vtu", format = Format.RGBA8, pingpong = true) Texture out;
}
