package net.vulkanmod.rendergraph.radiance.pass;

import net.vulkanmod.render.framegraph.Input;
import net.vulkanmod.render.framegraph.Output;
import net.vulkanmod.render.framegraph.Pass;
import net.vulkanmod.render.framegraph.Texture;
import net.vulkanmod.rendergraph.radiance.pipeline.VtuPresentPipeline;

@Pass(name = "vtupresent", pipeline = VtuPresentPipeline.class)
public final class VtuPresentPass {
    @Input("vtu") Texture image;

    @Output("swapchain") Texture out;
}
