package net.vulkanmod.render.framegraph.radiance;

import net.vulkanmod.render.framegraph.FrameGraph;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_R16G16B16A16_SFLOAT;

public final class RadianceGraph {
    private static FrameGraph graph;

    private RadianceGraph() {
    }

    public static FrameGraph get() {
        if (graph == null) {
            graph = FrameGraph.builder("radiance")
                    .target("light", VK_FORMAT_R16G16B16A16_SFLOAT, 1.0f, 1.0f, true)
                    .target("aatex", VK_FORMAT_R16G16B16A16_SFLOAT, 1.0f, 0.0f, false)
                    .pass(RadianceLightPipeline.class)
                        .in(0, "depthtex").in(1, "fgdepth")
                        .in(2, "shadowtex0").in(3, "shadowtex1").in(4, "shadowtex2")
                        .in(5, "light_history")
                        .out("light")
                    .pass(RadianceCompositePipeline.class)
                        .in(0, "scene").in(1, "depthtex").in(2, "fgdepth")
                        .in(3, "light").in(4, "opaquedepth")
                        .out("aatex")
                    .pass(RadianceAaPipeline.class)
                        .in(0, "aatex").in(1, "depthtex").in(2, "fgdepth")
                        .out(FrameGraph.SWAPCHAIN)
                    .build();
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
