package net.vulkanmod.mixin.render;

import net.minecraft.client.renderer.GameRenderer;
import net.vulkanmod.render.vtu.VtuJitter;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(GameRenderer.class)
public abstract class GameRendererJitterMixin {

    @Shadow
    private boolean panoramicMode;

    @ModifyArg(
            method = "renderLevel",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;resetProjectionMatrix(Lorg/joml/Matrix4f;)V"),
            index = 0)
    private Matrix4f volcanic$jitterWorldProjection(Matrix4f projection) {
        if (!this.panoramicMode) {
            VtuJitter.applyToWorldProjection(projection);
        }

        return projection;
    }
}
