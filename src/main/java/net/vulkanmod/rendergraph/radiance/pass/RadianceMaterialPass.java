package net.vulkanmod.rendergraph.radiance.pass;

import net.vulkanmod.render.framegraph.Output;
import net.vulkanmod.render.framegraph.Pass;
import net.vulkanmod.render.framegraph.Phase;
import net.vulkanmod.render.framegraph.Texture;
import net.vulkanmod.rendergraph.radiance.executor.MaterialExecutor;

@Pass(name = "material", phase = Phase.MID_RENDER, executor = MaterialExecutor.class)
public final class RadianceMaterialPass {
    @Output("material")      Texture material;
    @Output("materialdepth") Texture materialDepth;
}
