package net.vulkanmod.config.ui.settings;

import net.vulkanmod.Initializer;
import net.vulkanmod.api.MenuPlugin;
import net.vulkanmod.api.MenuSetting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * PROVISOIRE — le point de branchement à remplacer quand la pipeline déclarera ses paramètres.
 *
 * <p>REVO : c'est ici et nulle part ailleurs que le menu lit les plugins. Le jour où
 * {@code RenderPipelinePlugin} porte un discriminant et des paramètres, remplace
 * {@link #discover()} par un chargement de ton SPI, supprime {@code net.vulkanmod.api.MenuPlugin}
 * et {@code net.vulkanmod.api.MenuSetting}, et rien d'autre ne bouge : les pages, les onglets et
 * la sidebar consomment déjà le résultat de cette classe.
 *
 * <p>Un plugin qui implémente {@code RenderPipelinePlugin} est un shader et n'apparaît PAS ici : il
 * garde sa place dans la catégorie Shaders. C'est le seul discriminant dont on dispose aujourd'hui,
 * et il est bancal — un plugin non-shader qui déclarerait quand même un frame graph serait mal
 * classé. D'où la demande d'un {@code kind()} explicite.
 */
public final class MenuPlugins {
    private static final String PIPELINE_SPI = "net.vulkanmod.render.plugin.RenderPipelinePlugin";

    private static List<MenuPlugin> cached;

    private MenuPlugins() {
    }

    public static List<MenuPlugin> discover() {
        if (cached == null) {
            cached = load();
        }
        return cached;
    }

    public static void forget() {
        cached = null;
    }

    public static List<MenuSetting> settingsOf(MenuPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin must not be null");
        }
        try {
            List<MenuSetting> declared = plugin.settings();
            return declared == null ? List.of() : List.copyOf(declared);
        } catch (Throwable failure) {
            Initializer.LOGGER.warn("Plugin {} failed to declare its settings: {}",
                    plugin.id(), failure.toString());
            return List.of();
        }
    }

    public static String displayNameOf(MenuPlugin plugin) {
        try {
            String name = plugin.displayName();
            return name == null || name.isBlank() ? plugin.id() : name;
        } catch (Throwable failure) {
            Initializer.LOGGER.warn("Plugin {} failed to give its name: {}", safeId(plugin), failure.toString());
            return safeId(plugin) == null ? "?" : safeId(plugin);
        }
    }

    public static boolean enabledOf(MenuPlugin plugin) {
        try {
            return plugin.enabled();
        } catch (Throwable failure) {
            Initializer.LOGGER.warn("Plugin {} failed to report its state: {}",
                    safeId(plugin), failure.toString());
            return false;
        }
    }

    public static void applyTo(MenuPlugin plugin) {
        try {
            plugin.onApply();
        } catch (Throwable failure) {
            Initializer.LOGGER.warn("Plugin {} failed on apply: {}", safeId(plugin), failure.toString());
        }
    }

    public static List<String> groupsOf(MenuPlugin plugin) {
        Set<String> groups = new LinkedHashSet<>();
        for (MenuSetting setting : settingsOf(plugin)) {
            groups.add(setting.group().toLowerCase(Locale.ROOT));
        }
        return List.copyOf(groups);
    }

    private static List<MenuPlugin> load() {
        Set<String> shaderIds = pipelinePluginIds();
        List<MenuPlugin> found = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        try {
            for (MenuPlugin plugin : ServiceLoader.load(MenuPlugin.class)) {
                String id = safeId(plugin);
                if (id == null || shaderIds.contains(id) || !seen.add(id)) {
                    continue;
                }
                found.add(plugin);
            }
        } catch (Throwable failure) {
            Initializer.LOGGER.warn("Plugin discovery unavailable: {}", failure.toString());
            return List.of();
        }
        if (!found.isEmpty()) {
            Initializer.LOGGER.info("Menu plugins discovered: {}", seen);
        }
        return List.copyOf(found);
    }

    private static String safeId(MenuPlugin plugin) {
        try {
            String id = plugin.id();
            return id == null || id.isBlank() ? null : id;
        } catch (Throwable failure) {
            Initializer.LOGGER.warn("A plugin refused to give its id: {}", failure.toString());
            return null;
        }
    }

    private static Set<String> pipelinePluginIds() {
        Set<String> ids = new LinkedHashSet<>();
        try {
            Class<?> spi = Class.forName(PIPELINE_SPI);
            for (Object plugin : ServiceLoader.load(spi)) {
                Object id = spi.getMethod("id").invoke(plugin);
                if (id instanceof String text && !text.isBlank()) {
                    ids.add(text);
                }
            }
        } catch (ClassNotFoundException absent) {
            return ids;
        } catch (Throwable failure) {
            Initializer.LOGGER.warn("Could not list pipeline plugins: {}", failure.toString());
        }
        return ids;
    }
}
