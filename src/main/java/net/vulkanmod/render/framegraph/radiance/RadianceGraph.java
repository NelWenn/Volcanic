package net.vulkanmod.render.framegraph.radiance;

import net.vulkanmod.render.framegraph.FrameGraph;

public final class RadianceGraph {
    private static FrameGraph graph;

    private RadianceGraph() {
    }

    public static FrameGraph get() {
        if (graph == null) {
            graph = FrameGraph.fromPasses("radiance",
                    RadianceLightPass.class,
                    RadianceCompositePass.class,
                    RadianceAaPass.class);
        }
        return graph;
    }

    public static void dispose() {
        if (graph != null) {
            graph.dispose();
            graph = null;
        }
    }
}
