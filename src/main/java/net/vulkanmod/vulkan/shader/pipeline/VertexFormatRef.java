package net.vulkanmod.vulkan.shader.pipeline;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.render.vertex.CustomVertexFormat;

/** Indirection for v formats since annotations are unable to hold arbitrary object references. */
public enum VertexFormatRef {
    TERRAIN {
        public VertexFormat resolve() {
            return PipelineManager.TERRAIN_VERTEX_FORMAT;
        }
    },
    EXTERNAL_LOD {
        public VertexFormat resolve() {
            return CustomVertexFormat.EXTERNAL_LOD;
        }
    },
    NONE {
        public VertexFormat resolve() {
            return CustomVertexFormat.NONE;
        }
    };

    public abstract VertexFormat resolve();
}
