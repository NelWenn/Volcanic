package net.vulkanmod.render.ctm;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.vulkanmod.render.vertex.TerrainRenderType;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class CtmProperties {
    public final CtmMethod method;
    public final Set<ResourceLocation> matchTiles;
    public final Set<Block> matchBlocks;
    public final List<ResourceLocation> tileIds;
    public final int[] weights;
    public final EnumSet<Direction> faces;
    public final BiomeMatcher biomes;
    public final int minHeight;
    public final int maxHeight;
    public final int tintIndex;
    public final TerrainRenderType overlayLayer;
    public final String basePath;

    public CtmProperties(CtmMethod method, Set<ResourceLocation> matchTiles, Set<Block> matchBlocks,
                         List<ResourceLocation> tileIds, int[] weights, EnumSet<Direction> faces,
                         BiomeMatcher biomes, int minHeight, int maxHeight, int tintIndex,
                         TerrainRenderType overlayLayer, String basePath) {
        this.method = method;
        this.matchTiles = matchTiles;
        this.matchBlocks = matchBlocks;
        this.tileIds = tileIds;
        this.weights = weights;
        this.faces = faces;
        this.biomes = biomes;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.tintIndex = tintIndex;
        this.overlayLayer = overlayLayer;
        this.basePath = basePath;
    }

    public boolean matches(ResourceLocation baseSpriteId, Block block, Direction face, int y, String biomeId) {
        if (face != null && !faces.contains(face)) return false;
        if (y < minHeight || y > maxHeight) return false;
        if (!biomes.matches(biomeId)) return false;
        if (!matchBlocks.isEmpty() && matchBlocks.contains(block)) return true;
        if (!matchTiles.isEmpty() && matchTiles.contains(baseSpriteId)) return true;
        return matchBlocks.isEmpty() && matchTiles.isEmpty();
    }
}
