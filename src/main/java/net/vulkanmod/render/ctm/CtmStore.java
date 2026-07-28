package net.vulkanmod.render.ctm;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

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
        net.vulkanmod.Initializer.LOGGER.info("CTM: indexed {} byBlock rules, {} byTile rules", byBlock.size(), byTile.size());
    }

    public boolean isEmpty() {
        return byBlock.isEmpty() && byTile.isEmpty();
    }

    public boolean hasCandidates(TextureAtlasSprite sprite, Block block) {
        return byBlock.containsKey(block) || byTile.containsKey(sprite.contents().name());
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
            case CTM -> computeCtmIndex(ctx, match);
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

    private static final int[] SPRITE_INDEX_MAP = new int[] {
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24, 43,
            16, 18, 16, 18, 6, 46, 6, 21, 16, 18, 16, 18, 28, 9, 28, 22,
            36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24, 43,
            37, 40, 37, 40, 30, 8, 30, 34, 37, 40, 37, 40, 25, 23, 25, 45,
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
            1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
            36, 39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27,
            16, 42, 16, 42, 6, 20, 6, 10, 16, 42, 16, 42, 28, 35, 28, 44,
            36, 39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27,
            37, 38, 37, 38, 30, 11, 30, 32, 37, 38, 37, 38, 25, 33, 25, 26,
    };

    private int computeCtmIndex(CtmContext ctx, CtmProperties m) {
        Direction face = ctx.face();
        if (face == null || ctx.region() == null) return 0;
        Direction up = textureUp(face);
        Direction left = textureLeft(face, up);
        Direction[] dirs = { left, up.getOpposite(), left.getOpposite(), up };

        int connections = 0;
        boolean[] edge = new boolean[4];
        BlockPos pos = ctx.pos();
        for (int i = 0; i < 4; i++) {
            if (connects(ctx, m, pos.relative(dirs[i]))) {
                connections |= 1 << (i * 2);
                edge[i] = true;
            }
        }
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            if (edge[i] && edge[j] && connects(ctx, m, pos.relative(dirs[i]).relative(dirs[j]))) {
                connections |= 1 << (i * 2 + 1);
            }
        }
        int t = SPRITE_INDEX_MAP[connections];
        return t < m.tileIds.size() ? t : 0;
    }

    private static boolean connects(CtmContext ctx, CtmProperties m, BlockPos neighbor) {
        BlockState ns = ctx.region().getBlockState(neighbor);
        Block nb = ns.getBlock();
        if (!m.matchBlocks.isEmpty()) return m.matchBlocks.contains(nb);
        return nb == ctx.state().getBlock();
    }

    private static Direction textureUp(Direction face) {
        return switch (face) {
            case UP -> Direction.NORTH;
            case DOWN -> Direction.SOUTH;
            default -> Direction.UP;
        };
    }

    private static Direction textureLeft(Direction face, Direction up) {
        return face.getAxisDirection() == Direction.AxisDirection.NEGATIVE
                ? up.getClockWise(face.getAxis())
                : up.getCounterClockWise(face.getAxis());
    }
}
