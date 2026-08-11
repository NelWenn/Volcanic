package net.vulkanmod.io.plugin;

/**
 * Restricts a plugin jar's classloader to the JDK, the graphics libraries the engine itself uses
 * (LWJGL/JOML/fastutil), and the mod's public rendering API. Everything else resolves to
 * {@link ClassNotFoundException}, so a pipeline can only be written against the same surface
 * Radiance uses internally — it cannot reach into unrelated engine or game internals.
 */
final class ApiClassLoader extends ClassLoader {
    private static final String[] PACKAGE_PREFIXES = {
            "net.vulkanmod.vulkan.shader",
            "net.vulkanmod.render.framegraph",
            "net.vulkanmod.render.pipeline",
            "net.vulkanmod.render.plugin",
            "net.vulkanmod.render.vertex",
            "net.vulkanmod.vulkan.texture",
            "net.vulkanmod.vulkan.framebuffer",
            "net.vulkanmod.vulkan.util",
            "net.vulkanmod.vulkan.memory",
            "net.vulkanmod.vulkan.device",
    };

    private static final String[] EXACT_CLASSES = {
            "net.vulkanmod.vulkan.Renderer",
            "net.vulkanmod.vulkan.Vulkan",
            "net.vulkanmod.vulkan.VRenderSystem",
    };

    private final ClassLoader engine;

    ApiClassLoader(ClassLoader engine) {
        super(null);
        this.engine = engine;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!isPlatform(name) && !isEngineApi(name))
            throw new ClassNotFoundException(name + " is not part of the render pipeline API");

        Class<?> loaded = engine.loadClass(name);
        if (resolve)
            resolveClass(loaded);
        return loaded;
    }

    private static boolean isPlatform(String name) {
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.")
                || name.startsWith("org.lwjgl.") || name.startsWith("org.joml.")
                || name.startsWith("it.unimi.dsi.fastutil.");
    }

    private static boolean isEngineApi(String name) {
        for (String exact : EXACT_CLASSES)
            if (name.equals(exact))
                return true;

        for (String prefix : PACKAGE_PREFIXES)
            if (name.equals(prefix) || name.startsWith(prefix + "."))
                return true;

        return false;
    }
}
