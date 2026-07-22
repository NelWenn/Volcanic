package net.vulkanmod.render.ctm;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.vulkanmod.render.chunk.WorldRenderer;

public final class Ctm {
    private static volatile CtmStore store;

    private Ctm() {}

    public static void install(CtmStore s) {
        store = (s == null || s.isEmpty()) ? null : s;
    }

    public static void clear() {
        store = null;
    }

    public static boolean isActive() {
        return store != null && net.vulkanmod.Initializer.CONFIG.ctmEnabled;
    }

    public static CtmResult resolve(TextureAtlasSprite sprite, BlockState state, BlockPos pos,
                                    Direction face, BlockAndTintGetter region) {
        CtmStore s = store;
        if (s == null) return CtmResult.none();
        try {
            ResourceLocation biome = null;
            var level = WorldRenderer.getLevel();
            if (level != null) biome = level.getBiome(pos).unwrapKey().map(k -> k.location()).orElse(null);
            return s.resolve(new CtmContext(sprite, state, pos, face, biome));
        } catch (Throwable t) {
            return CtmResult.none();
        }
    }
}
