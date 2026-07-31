package net.vulkanmod.mixin.compatibility.sable;

import net.vulkanmod.compat.sable.SableCompat;
import net.vulkanmod.vulkan.VRenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData", remap = false)
public class VanillaChunkedSubLevelRenderDataMixin {

    @Inject(method = "renderChunkedSubLevel", at = @At("HEAD"), require = 0)
    private void vulkanMod$captureSubLevelLighting(CallbackInfo ci) {
        VRenderSystem.sableSkyLightScale = SableCompat.skyLightScale(this);
        VRenderSystem.sableEnableNormalLighting = 1.0f;
    }

    @Inject(method = "renderChunkedSubLevel", at = @At("RETURN"), require = 0)
    private void vulkanMod$resetSubLevelLighting(CallbackInfo ci) {
        VRenderSystem.sableSkyLightScale = 1.0f;
        VRenderSystem.sableEnableNormalLighting = 0.0f;
    }
}
