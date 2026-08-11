package net.vulkanmod.io.plugin;

import net.neoforged.fml.loading.FMLPaths;
import net.vulkanmod.Initializer;
import net.vulkanmod.plugin.PluginRegistry;
import net.vulkanmod.plugin.RenderPipelinePlugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * Loads render pipeline jars from {@code renderpipelines/} in the game directory. Each jar gets
 * its own classloader whose parent ({@link ApiClassLoader}) can only resolve JDK and graphics-library
 * classes and the mod's public rendering API itself isn't restricted to either.
 * This enforces a clean extension surface, it is <b>not</b> a
 * security sandbox against deliberately malicious code (the jar still runs with the game's full
 * JVM permissions), so the user should only install pipelines he trusts.
 * TODO: more sandboxing on the loaded jars
 */
public final class PluginJarLoader {
    private static boolean loaded = false;

    private PluginJarLoader() {}

    public static synchronized void loadAll() {
        if (loaded)
            return;
        loaded = true;

        Path dir = FMLPaths.GAMEDIR.get().resolve("renderpipelines");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            Initializer.LOGGER.error("Could not create renderpipelines dir", e);
            return;
        }

        List<Path> jars;
        try (Stream<Path> walk = Files.list(dir)) {
            jars = walk.filter(p -> p.toString().endsWith(".jar")).toList();
        } catch (IOException e) {
            Initializer.LOGGER.error("Could not list renderpipelines dir", e);
            return;
        }

        for (Path jar : jars) {
            loadJar(jar);
        }
    }

    private static void loadJar(Path jar) {
        try {
            URL url = jar.toUri().toURL();
            ClassLoader api = new ApiClassLoader(PluginJarLoader.class.getClassLoader());
            URLClassLoader pluginLoader = new URLClassLoader(jar.getFileName().toString(), new URL[]{url}, api);

            for (RenderPipelinePlugin plugin : ServiceLoader.load(RenderPipelinePlugin.class, pluginLoader)) {
                PluginRegistry.registerPlugin(plugin, pluginLoader);
                Initializer.LOGGER.info("Registered render pipeline '{}' ({}) from {}", plugin.id(), plugin.name(), jar.getFileName());
            }
        } catch (Throwable t) {
            // Broken or unallowed jar case
            Initializer.LOGGER.error("Failed to load render pipeline jar '{}'", jar.getFileName(), t);
        }
    }
}
