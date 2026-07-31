package net.vulkanmod.mixin.compatibility.sable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.vulkanmod.compat.sable.SableBiomeContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BiomeColors.class)
public class BiomeColorsMixin {

    @Inject(method = "getAverageGrassColor", at = @At("HEAD"), cancellable = true, require = 0)
    private static void vulkanMod$subLevelGrass(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int[] colors = vulkanMod$subLevelColors(level);
        if (colors != null) {
            cir.setReturnValue(colors[0]);
        }
    }

    @Inject(method = "getAverageFoliageColor", at = @At("HEAD"), cancellable = true, require = 0)
    private static void vulkanMod$subLevelFoliage(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int[] colors = vulkanMod$subLevelColors(level);
        if (colors != null) {
            cir.setReturnValue(colors[1]);
        }
    }

    @Inject(method = "getAverageWaterColor", at = @At("HEAD"), cancellable = true, require = 0)
    private static void vulkanMod$subLevelWater(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int[] colors = vulkanMod$subLevelColors(level);
        if (colors != null) {
            cir.setReturnValue(colors[2]);
        }
    }

    private static int[] vulkanMod$subLevelColors(BlockAndTintGetter level) {
        if (level instanceof RenderChunkRegion) {
            return SableBiomeContext.pending();
        }
        return null;
    }
}
