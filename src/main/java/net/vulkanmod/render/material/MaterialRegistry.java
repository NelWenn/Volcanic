package net.vulkanmod.render.material;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.ConcurrentHashMap;

public final class MaterialRegistry {
    public static final int NONE = 0;
    public static final int GLASS = 1;

    private static final ConcurrentHashMap<Block, Integer> CACHE = new ConcurrentHashMap<>();

    private MaterialRegistry() {
    }

    public static int materialId(BlockState state) {
        return CACHE.computeIfAbsent(state.getBlock(), MaterialRegistry::classify);
    }

    private static int classify(Block block) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        if (key == null)
            return NONE;

        String path = key.getPath();
        if (path.contains("glass"))
            return GLASS;

        return NONE;
    }
}
