package net.vulkanmod.render.framegraph.radiance;

import net.vulkanmod.render.framegraph.Output;
import net.vulkanmod.render.framegraph.Pass;
import net.vulkanmod.render.framegraph.Phase;
import net.vulkanmod.render.framegraph.Texture;

@Pass(name = "shadow_map", phase = Phase.FRAME_START, executor = ShadowMapExecutor.class)
public final class ShadowMapPass {
    @Output("shadowtex0") Texture shadow0;
    @Output("shadowtex1") Texture shadow1;
    @Output("shadowtex2") Texture shadow2;
}
