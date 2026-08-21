package net.vulkanmod.plugin.hooks.events.render;

import net.vulkanmod.render.chunk.ChunkArea;
import net.vulkanmod.render.context.RenderContext;

import java.util.Iterator;

public interface WorldSectionRenderBeginEvent {
    long                when();
    RenderContext       context();
    Iterator<ChunkArea> chunkAreas();
}
