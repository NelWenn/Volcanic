package net.vulkanmod.config.ui.settings;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.Initializer;
import net.vulkanmod.plugin.PluginEntry;
import net.vulkanmod.plugin.PluginRegistry;
import net.vulkanmod.plugin.RenderPipelinePlugin;
import net.vulkanmod.plugin.SettingsRegistry;

import java.io.InputStream;
import java.util.*;

public final class MenuPlugins {

    private MenuPlugins() {
    }

    public static List<PluginEntry> discover() {
        List<PluginEntry> entries = new ArrayList<>(PluginRegistry.getPlugins().values());
        entries.sort(Comparator.comparing(MenuPlugins::displayNameOf, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(entries);
    }

    public static PluginEntry byId(String id) {
        return id == null ? null : PluginRegistry.getPlugins().get(id);
    }

    public static String displayNameOf(PluginEntry entry) {
        if (entry == null)
            return "?";

        RenderPipelinePlugin plugin = entry.getPlugin();
        try {
            String name = plugin.name();
            return name == null || name.isBlank() ? plugin.id() : name;
        } catch (Throwable failure) {
            String id = safeId(plugin);
            Initializer.LOGGER.warn("Plugin {} failed to give its name: {}", id, failure.toString());
            return id == null ? "?" : id;
        }
    }

    public static boolean enabledOf(PluginEntry entry) {
        return entry != null && entry.isEnabled();
    }

    public static boolean toggleableOf(PluginEntry entry) {
        return entry != null && entry.isToggleable();
    }

    public static void setEnabled(PluginEntry entry, boolean enabled) {
        if (entry == null) {
            return;
        }
        if (!entry.isToggleable()) {
            if (enabled != entry.isEnabled()) {
                Initializer.LOGGER.warn("Plugin {} is not toggleable, ignoring state change",
                        safeId(entry.getPlugin()));
            }

            return;
        }
        entry.setEnabled(enabled);
    }

    public static Collection<SettingsRegistry.CategoryEntry> settingsOf(PluginEntry entry) {
        if (entry == null) {
            return List.of();
        }
        String id = safeId(entry.getPlugin());
        return id == null ? List.of() : SettingsRegistry.getCategories(id).values();
    }

    public static List<String> groupsOf(PluginEntry entry) {
        Set<String> groups = new LinkedHashSet<>();

        for (SettingsRegistry.CategoryEntry category : settingsOf(entry))
            groups.add(category.id.toLowerCase(Locale.ROOT));

        return List.copyOf(groups);
    }

    private static String safeId(RenderPipelinePlugin plugin) {
        try {
            String id = plugin.id();
            return id == null || id.isBlank() ? null : id;
        } catch (Throwable failure) {
            Initializer.LOGGER.warn("A plugin refused to give its id: {}", failure.toString());
            return null;
        }
    }

    public record Art(ResourceLocation texture, int width, int height, float midLuma) {}

    public record Showcase(String byline, String description, List<String> tags,
                           Art icon, Art banner) {}

    private static final Map<String, Showcase> SHOWCASES = new HashMap<>();

    public static Showcase showcaseOf(PluginEntry entry) {
        if (entry == null) {
            return new Showcase(null, null, List.of(), null, null);
        }
        String id = safeId(entry.getPlugin());
        if (id == null) {
            return new Showcase(null, null, List.of(), null, null);
        }
        return SHOWCASES.computeIfAbsent(id, key -> readShowcase(entry.getPlugin()));
    }

    private static Showcase readShowcase(RenderPipelinePlugin plugin) {
        String byline = null;
        String description = null;
        List<String> tags = List.of();
        Art icon = null;
        Art banner = null;

        try {
            byline = plugin.byline();
            description = plugin.description();
            tags = List.of(plugin.tags());
            icon = artOf(plugin.icon());
            banner = artOf(plugin.banner());
        } catch (Throwable failure) {
            Initializer.LOGGER.warn("Plugin {} failed to dress its showcase: {}",
                    safeId(plugin), failure.toString());
        }
        return new Showcase(byline, description, tags, icon, banner);
    }

    private static Art artOf(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        try {
            ResourceLocation texture = ResourceLocation.parse(path);
            var resource = Minecraft.getInstance().getResourceManager().getResource(texture);

            if (resource.isEmpty())
                return null;

            try (InputStream in = resource.get().open();
                 NativeImage image = NativeImage.read(in)) {

                int width = image.getWidth();
                int height = image.getHeight();

                if (width <= 0 || height <= 0)
                    return null;

                float total = 0.0f;
                int samples = 0;

                int stepX = Math.max(1, width / 32);
                int stepY = Math.max(1, height / 24);

                for (int y = Math.round(height * 0.30f); y < height * 0.75f; y += stepY) {
                    for (int x = 0; x < width; x += stepX) {
                        int abgr = image.getPixelRGBA(x, y);

                        int red = abgr & 0xFF;
                        int green = (abgr >> 8) & 0xFF;
                        int blue = (abgr >> 16) & 0xFF;

                        total += (0.299f * red + 0.587f * green + 0.114f * blue) / 255.0f;
                        samples++;
                    }
                }
                return new Art(texture, width, height, samples == 0 ? 0.0f : total / samples);
            }
        } catch (Throwable unavailable) {
            return null;
        }
    }
}