package net.vulkanmod.plugin.hooks.events.render;

import net.vulkanmod.render.context.RenderContext;

public interface WorldRenderSectionEndEvent {
    long            when();
    RenderContext   context();
}
