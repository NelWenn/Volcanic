package net.vulkanmod.render.framegraph.radiance;

import net.vulkanmod.render.framegraph.Input;
import net.vulkanmod.render.framegraph.Output;
import net.vulkanmod.render.framegraph.Pass;
import net.vulkanmod.render.framegraph.Phase;
import net.vulkanmod.render.framegraph.Texture;

@Pass(name = "colored_shadow_tint", phase = Phase.MID_RENDER, executor = ColoredShadowTintExecutor.class)
public final class ColoredShadowTintPass {
    @Input("opaquedepth")  Texture opaqueDepth;
    @Input("shadowcolor0") Texture tint0;
    @Input("shadowcolor1") Texture tint1;
    @Input("shadowcolor2") Texture tint2;

    @Output("scene") Texture scene;
}
