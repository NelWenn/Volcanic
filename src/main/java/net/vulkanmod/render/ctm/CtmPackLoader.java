package net.vulkanmod.render.ctm;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.vulkanmod.Initializer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class CtmPackLoader {
    private static List<CtmProperties> loaded = List.of();

    private CtmPackLoader() {}

    public static void reload(ResourceManager rm) {
        List<CtmProperties> all = new ArrayList<>();
        Set<ResourceLocation> tiles = new HashSet<>();
        try {
            Map<ResourceLocation, Resource> found = rm.listResources("optifine/ctm",
                    p -> p.getPath().endsWith(".properties"));
            for (Map.Entry<ResourceLocation, Resource> e : found.entrySet()) {
                try (InputStream in = e.getValue().open()) {
                    Properties props = new Properties();
                    props.load(in);
                    CtmProperties parsed = CtmPropertiesParser.parse(props, e.getKey());
                    if (parsed != null) {
                        all.add(parsed);
                        tiles.addAll(parsed.tileIds);
                    }
                } catch (Throwable t) {
                    Initializer.LOGGER.warn("CTM: failed to parse {}", e.getKey());
                }
            }
        } catch (Throwable t) {
            Initializer.LOGGER.warn("CTM: resource scan failed", t);
        }
        loaded = all;
        CtmAtlasRegistrar.setPending(tiles);
        Initializer.LOGGER.info("CTM: loaded {} properties, {} tiles", all.size(), tiles.size());
    }

    public static void buildStore(TextureAtlas blocksAtlas) {
        if (loaded.isEmpty()) { Ctm.clear(); return; }
        CtmStore store = new CtmStore(loaded, CtmAtlasRegistrar.spriteLookup(blocksAtlas));
        Ctm.install(store);
    }
}
