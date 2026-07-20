package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {

    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void volcanic$horizonFog(Camera camera, FogRenderer.FogMode fogMode, float farPlaneDistance, boolean shouldCreateFog, float partialTick, CallbackInfo ci) {
        if (fogMode != FogRenderer.FogMode.FOG_TERRAIN)
            return;

        Config config = Initializer.CONFIG;
        if (!config.shadersEnabled || !config.isCamille())
            return;
        if (shouldCreateFog || camera.getFluidInCamera() != FogType.NONE)
            return;

        Entity entity = camera.getEntity();
        if (entity instanceof LivingEntity living
                && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS)))
            return;

        float strength = config.horizonFog;
        if (strength <= 0.004f) {
            RenderSystem.setShaderFogStart(Float.MAX_VALUE);
            RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
            return;
        }

        float startFrac = 0.99f - 0.09f * strength;
        RenderSystem.setShaderFogStart(farPlaneDistance * startFrac);
        RenderSystem.setShaderFogEnd(farPlaneDistance);
    }
}
