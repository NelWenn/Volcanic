package net.vulkanmod.render.ctm;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class CtmStore {
    private final Map<Block, List<CtmProperties>> byBlock = new HashMap<>();
    private final Map<ResourceLocation, List<CtmProperties>> byTile = new HashMap<>();
    private final Function<ResourceLocation, TextureAtlasSprite> spriteLookup;

    public CtmStore(List<CtmProperties> all, Function<ResourceLocation, TextureAtlasSprite> spriteLookup) {
        this.spriteLookup = spriteLookup;
        for (CtmProperties p : all) {
            for (Block b : p.matchBlocks) byBlock.computeIfAbsent(b, k -> new ArrayList<>()).add(p);
            for (ResourceLocation t : p.matchTiles) byTile.computeIfAbsent(t, k -> new ArrayList<>()).add(p);
        }
    }

    public boolean isEmpty() {
        return byBlock.isEmpty() && byTile.isEmpty();
    }

    public CtmResult resolve(CtmContext ctx) {
        ResourceLocation baseId = ctx.sprite().contents().name();
        Block block = ctx.state().getBlock();
        String biomeId = ctx.biome() == null ? "" : ctx.biome().toString();
        int y = ctx.pos().getY();

        CtmProperties match = firstMatch(byBlock.get(block), baseId, block, ctx.face(), y, biomeId);
        if (match == null) match = firstMatch(byTile.get(baseId), baseId, block, ctx.face(), y, biomeId);
        if (match == null) return CtmResult.none();

        int face = ctx.face() == null ? 6 : ctx.face().get3DDataValue();
        int idx = switch (match.method) {
            case FIXED, OVERLAY_FIXED -> 0;
            case RANDOM, OVERLAY_RANDOM -> WeightedPicker.pick(match.weights,
                    PositionRng.seed(ctx.pos().getX(), ctx.pos().getY(), ctx.pos().getZ(), face));
            case REPEAT -> Math.floorMod(ctx.pos().getX() * 31 + ctx.pos().getZ(), match.tileIds.size());
            default -> 0;
        };
        TextureAtlasSprite sprite = spriteLookup.apply(match.tileIds.get(idx));
        if (sprite == null) return CtmResult.none();

        if (match.method.isOverlay()) {
            return CtmResult.overlay(sprite, match.overlayLayer, match.tintIndex);
        }
        return CtmResult.swap(sprite);
    }

    private CtmProperties firstMatch(List<CtmProperties> list, ResourceLocation baseId, Block block,
                                     Direction face, int y, String biomeId) {
        if (list == null) return null;
        for (CtmProperties p : list) {
            if (p.matches(baseId, block, face, y, biomeId)) return p;
        }
        return null;
    }
}
