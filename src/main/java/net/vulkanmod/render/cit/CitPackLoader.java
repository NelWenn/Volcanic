package net.vulkanmod.render.cit;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.vulkanmod.Initializer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class CitPackLoader {
    private static volatile Map<Item, List<CitRule>> byItem = Map.of();
    private static volatile Set<ResourceLocation> models = Set.of();
    private static volatile int ruleCount = 0;

    private CitPackLoader() {}

    public static void reload(ResourceManager rm) {
        Map<Item, List<CitRule>> index = new HashMap<>();
        Set<ResourceLocation> modelSet = new HashSet<>();
        int count = 0;
        try {
            Map<ResourceLocation, Resource> found = rm.listResources("optifine/cit",
                    p -> p.getPath().endsWith(".properties"));
            for (Map.Entry<ResourceLocation, Resource> e : found.entrySet()) {
                try (InputStream in = e.getValue().open()) {
                    Properties p = new Properties();
                    p.load(in);
                    CitRule rule = parse(p);
                    if (rule == null) continue;
                    for (Item it : rule.items()) index.computeIfAbsent(it, k -> new ArrayList<>()).add(rule);
                    modelSet.add(rule.model());
                    count++;
                } catch (Throwable t) {
                    Initializer.LOGGER.warn("CIT: failed to parse {}", e.getKey());
                }
            }
        } catch (Throwable t) {
            Initializer.LOGGER.warn("CIT: resource scan failed", t);
        }
        byItem = index;
        models = modelSet;
        ruleCount = count;
        Initializer.LOGGER.info("CIT: loaded {} rules, {} models", count, modelSet.size());
    }

    private static CitRule parse(Properties p) {
        if (!"item".equalsIgnoreCase(p.getProperty("type", "item"))) return null;
        String modelStr = p.getProperty("model");
        if (modelStr == null) return null;
        ResourceLocation model = ResourceLocation.parse(modelStr.trim());

        Set<Item> items = new HashSet<>();
        String matchItems = p.getProperty("matchItems");
        if (matchItems != null) {
            for (String s : matchItems.trim().split("\\s+")) {
                ResourceLocation id = ResourceLocation.parse(s.contains(":") ? s : "minecraft:" + s);
                Item it = BuiltInRegistries.ITEM.get(id);
                if (it != Items.AIR) items.add(it);
            }
        }
        ResourceLocation pattern = idOrNull(p.getProperty("components.trim.pattern"));
        ResourceLocation material = idOrNull(p.getProperty("components.trim.material"));
        if (items.isEmpty() && pattern == null && material == null) return null;
        return new CitRule(items, pattern, material, model);
    }

    private static ResourceLocation idOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        return ResourceLocation.parse(t.contains(":") ? t : "minecraft:" + t);
    }

    public static List<CitRule> rulesFor(Item item) { return byItem.getOrDefault(item, List.of()); }
    public static Set<ResourceLocation> modelsToRegister() { return models; }
    public static int ruleCount() { return ruleCount; }
}
