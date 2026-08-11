package net.vulkanmod.vulkan.shader;

import net.vulkanmod.render.plugin.RenderPipelineJarLoader;
import net.vulkanmod.render.plugin.RenderPipelinePlugin;
import net.vulkanmod.rendergraph.radiance.RadiancePipelinePlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of the render pipeline implementations the engine can drive
 */
public final class RenderPipelines {
    private static final Map<String, RenderPipelineProvider> PROVIDERS = new HashMap<>();
    private static String activeId;
    private static boolean bootstrapped = false;

    private RenderPipelines() {
    }

    public static void register(RenderPipelineProvider provider) {
        PROVIDERS.put(provider.id(), provider);
    }

    public static void registerPlugin(RenderPipelinePlugin plugin) {
        registerPlugin(plugin, null);
    }

    public static void registerPlugin(RenderPipelinePlugin plugin, ClassLoader resourceLoader) {
        register(new RenderPipelineProvider(plugin.id(), () -> {
            PipelineManager pipelineManager = plugin.createPipelineManager();
            pipelineManager.setResourceClassLoader(resourceLoader);
            return pipelineManager;
        }, plugin::createFrameGraph));
    }

    public static void setActive(String id) {
        activeId = id;
    }

    public static RenderPipelineProvider active() {
        bootstrap();
        RenderPipelineProvider provider = PROVIDERS.get(activeId);
        if (provider == null)
            throw new IllegalStateException("No render pipeline registered for id: " + activeId);
        return provider;
    }

    public static RenderPipelineProvider get(String id) {
        bootstrap();
        return PROVIDERS.get(id);
    }

    private static void bootstrap() {
        if (bootstrapped)
            return;
        bootstrapped = true;

        registerPlugin(new RadiancePipelinePlugin());
        setActive("radiance");

        RenderPipelineJarLoader.loadAll();
    }
}
