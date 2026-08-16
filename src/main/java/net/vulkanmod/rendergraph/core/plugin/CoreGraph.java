package net.vulkanmod.rendergraph.core.plugin;

import net.vulkanmod.render.framegraph.FrameGraph;
import net.vulkanmod.render.framegraph.FrameGraphImpl;

public final class CoreGraph implements FrameGraphImpl {
    private static FrameGraph graph;

    /**
     * Get the current holded {@link FrameGraph}
     * @return the current framegraph
     */
    public FrameGraph get() {
        if (graph == null) {
            graph = FrameGraph.fromPasses("core");
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
