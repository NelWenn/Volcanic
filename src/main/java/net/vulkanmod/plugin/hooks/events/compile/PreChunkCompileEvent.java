package net.vulkanmod.plugin.hooks.events.compile;

import net.vulkanmod.plugin.hooks.events.CancelableEvent;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.build.RenderRegion;

public abstract class PreChunkCompileEvent extends CancelableEvent {

    // Timestamp
    public abstract long            when();

    // Coords
    public abstract int             chunkX();
    public abstract int             chunkZ();

    // Region properties
    public abstract RenderRegion    region();
    public abstract RenderSection   section();

}
