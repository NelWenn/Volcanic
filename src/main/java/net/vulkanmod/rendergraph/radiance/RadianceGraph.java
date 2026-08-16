package net.vulkanmod.rendergraph.radiance;

import net.vulkanmod.render.framegraph.FrameGraph;
import net.vulkanmod.render.framegraph.FrameGraphImpl;
import net.vulkanmod.rendergraph.radiance.pass.*;

public final class RadianceGraph implements FrameGraphImpl {
    private static FrameGraph graph;

    /**
     * Get the current holded {@link FrameGraph}
     * @return the current framegraph
     */
    public FrameGraph get() {
        if (graph == null) {
            graph = FrameGraph.fromPasses("radiance",
                    ShadowMapPass.class,
                    ColoredShadowTintPass.class,
                    RadianceMaterialPass.class,
                    RadianceLightPass.class,
                    RadianceReflectionPass.class,
                    RadianceGlassReflectionPass.class,
                    RadianceCompositePass.class,
                    RadianceAaPass.class,
                    VsrUpscalePass.class,
                    VtuPresentPass.class
            );
        }
        return graph;
    }

    /**
     * Disposes the current {@link FrameGraph}
     */
    public void dispose() {
        if (graph != null) {
            graph.dispose();
            graph = null;
        }
    }
}
