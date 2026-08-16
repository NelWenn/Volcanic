package net.vulkanmod.plugin;

import net.vulkanmod.io.plugin.PluginJarLoader;
import net.vulkanmod.rendergraph.core.plugin.CorePipelinePlugin;
import net.vulkanmod.rendergraph.radiance.RadiancePipelinePlugin;
import net.vulkanmod.vulkan.shader.PipelineManager;
import net.vulkanmod.vulkan.shader.RenderPipelineProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Plugin registries that hold all the installed Volcanic plugins
 */
public final class PluginRegistry {

    private static final Map<String, PluginEntry>               PLUGINS     = new HashMap<>();
    private static final Map<String, RenderPipelineProvider>    PROVIDERS   = new HashMap<>();

    private static String activeShaderId = null;
    private static boolean bootstrapped = false;

    private PluginRegistry() {}

    public static void register(RenderPipelineProvider provider) {
        PROVIDERS.put(provider.id(), provider);
    }

    public static void registerPlugin(RenderPipelinePlugin plugin) {
        registerPlugin(plugin, null);
    }

    public static void registerPlugin(RenderPipelinePlugin plugin, ClassLoader resourceLoader) {
        PLUGINS.put(plugin.id(), new PluginEntry(plugin, plugin.toggleable()));

        register(new RenderPipelineProvider(
                plugin.id(),
                () -> {
                    PipelineManager pipelineManager = plugin.createPipelineManager();
                    pipelineManager.setResourceClassLoader(resourceLoader);
                    return pipelineManager;
                },
                plugin::createFrameGraph,
                plugin
        ));
    }

    public static void setActiveShader(String id) {
        activeShaderId = id;
    }

    public static RenderPipelineProvider activeShader() {
        bootstrap();

        RenderPipelineProvider provider = PROVIDERS.get(activeShaderId);
        if (provider == null) {
            throw new IllegalStateException("No render pipeline registered for id: " + activeShaderId);
        }

        return provider;
    }

    public static RenderPipelineProvider get(String id) {
        bootstrap();
        return PROVIDERS.get(id);
    }

    public static Map<String, PluginEntry> getPlugins() {
        return Collections.unmodifiableMap(PLUGINS);
    }

    private static void bootstrap() {
        if (bootstrapped) {
            return;
        }

        bootstrapped = true;

        registerPlugin(new CorePipelinePlugin());
        PluginJarLoader.loadAll();
        setActiveShader("core");
    }
}