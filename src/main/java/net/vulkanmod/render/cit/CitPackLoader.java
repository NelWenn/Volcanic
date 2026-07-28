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
import java.util.regex.Pattern;

public final class CitPackLoader {
    private static volatile Map<Item, List<CitRule>> byItem = Map.of();
    private static volatile Set<ResourceLocation> models = Set.of();
    private static volatile Set<ResourceLocation> renderable = Set.of();
    private static volatile int ruleCount = 0;

    private CitPackLoader() {}

    public static void reload(ResourceManager rm) {
        Map<Item, List<CitRule>> index = new HashMap<>();
        Set<ResourceLocation> modelSet = new HashSet<>();
        Set<ResourceLocation> renderableSet = new HashSet<>();
        int count = 0;
        try {
            Map<ResourceLocation, Resource> found = rm.listResources("optifine/cit",
                    p -> p.getPath().endsWith(".properties"));
            for (Map.Entry<ResourceLocation, Resource> e : found.entrySet()) {
                try (InputStream in = e.getValue().open()) {
                    Properties p = new Properties();
                    p.load(in);
                    CitRule rule = parse(p, e.getKey());
                    if (rule == null) continue;
                    for (Item it : rule.items()) index.computeIfAbsent(it, k -> new ArrayList<>()).add(rule);
                    modelSet.add(rule.model());
                    if (modelExists(rm, rule.model())) renderableSet.add(rule.model());
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
        renderable = renderableSet;
        ruleCount = count;
        Initializer.LOGGER.info("CIT: loaded {} rules, {} models, {} renderable", count, modelSet.size(), renderableSet.size());
    }

    private static CitRule parse(Properties p, ResourceLocation propsId) {
        if (!"item".equalsIgnoreCase(p.getProperty("type", "item"))) return null;
        String modelStr = p.getProperty("model");
        if (modelStr == null) return null;
        ResourceLocation model = resolveModel(modelStr.trim(), propsId);
        if (model == null) return null;

        Set<Item> items = new HashSet<>();
        String matchItems = p.getProperty("matchItems");
        if (matchItems != null) {
            for (String s : matchItems.trim().split("\\s+")) {
                if (s.isEmpty()) continue;
                ResourceLocation id = ResourceLocation.parse(s.contains(":") ? s : "minecraft:" + s);
                Item it = BuiltInRegistries.ITEM.get(id);
                if (it != Items.AIR) items.add(it);
            }
        }
        Pattern namePattern = compileNameMatcher(p.getProperty("nbt.display.Name"));
        if (namePattern == null) namePattern = compileNameMatcher(p.getProperty("components.custom_name"));
        ResourceLocation pattern = idOrNull(p.getProperty("components.trim.pattern"));
        ResourceLocation material = idOrNull(p.getProperty("components.trim.material"));
        if (items.isEmpty() && namePattern == null && pattern == null && material == null) return null;
        return new CitRule(items, namePattern, pattern, material, model);
    }

    private static ResourceLocation resolveModel(String raw, ResourceLocation propsId) {
        if (raw.isEmpty()) return null;
        if (raw.contains(":")) return ResourceLocation.tryParse(raw);

        String dir = propsId.getPath();
        int slash = dir.lastIndexOf('/');
        dir = slash >= 0 ? dir.substring(0, slash) : "";

        String s = raw;
        if (s.endsWith(".json")) s = s.substring(0, s.length() - 5);
        while (s.startsWith("./")) s = s.substring(2);
        while (s.startsWith("../")) {
            s = s.substring(3);
            int sl = dir.lastIndexOf('/');
            dir = sl >= 0 ? dir.substring(0, sl) : "";
        }
        String path = dir.isEmpty() ? s : dir + "/" + s;
        return ResourceLocation.fromNamespaceAndPath(propsId.getNamespace(), path);
    }

    private static boolean modelExists(ResourceManager rm, ResourceLocation model) {
        ResourceLocation json = ResourceLocation.fromNamespaceAndPath(model.getNamespace(), model.getPath() + ".json");
        return rm.getResource(json).isPresent();
    }

    private static Pattern compileNameMatcher(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        if (s.startsWith("ipattern:")) return Pattern.compile(globToRegex(s.substring(9)), Pattern.CASE_INSENSITIVE);
        if (s.startsWith("pattern:")) return Pattern.compile(globToRegex(s.substring(8)));
        if (s.startsWith("iregex:")) return Pattern.compile(s.substring(7), Pattern.CASE_INSENSITIVE);
        if (s.startsWith("regex:")) return Pattern.compile(s.substring(6));
        return Pattern.compile(Pattern.quote(s));
    }

    private static String globToRegex(String glob) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*' -> sb.append(".*");
                case '?' -> sb.append('.');
                default -> {
                    if ("\\.[]{}()+-^$|".indexOf(c) >= 0) sb.append('\\');
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private static ResourceLocation idOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        return ResourceLocation.parse(t.contains(":") ? t : "minecraft:" + t);
    }

    public static List<CitRule> rulesFor(Item item) { return byItem.getOrDefault(item, List.of()); }
    public static Set<ResourceLocation> modelsToRegister() { return models; }
    public static boolean isRenderable(ResourceLocation model) { return renderable.contains(model); }
    public static int ruleCount() { return ruleCount; }
}
