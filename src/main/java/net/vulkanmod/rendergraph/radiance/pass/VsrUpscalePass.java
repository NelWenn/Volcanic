package net.vulkanmod.rendergraph.radiance.pass;

import net.vulkanmod.render.framegraph.Input;
import net.vulkanmod.render.framegraph.Output;
import net.vulkanmod.render.framegraph.Pass;
import net.vulkanmod.render.framegraph.Texture;
import net.vulkanmod.rendergraph.radiance.pipeline.VsrUpscalePipeline;

@Pass(name = "vsr", pipeline = VsrUpscalePipeline.class)
public final class VsrUpscalePass {
    @Input("vsrtex") Texture scene;

    @Output("swapchain") Texture out;
}
