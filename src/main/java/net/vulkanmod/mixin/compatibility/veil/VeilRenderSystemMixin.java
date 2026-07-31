package net.vulkanmod.mixin.compatibility.veil;

import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "foundry.veil.api.client.render.VeilRenderSystem", remap = false)
public class VeilRenderSystemMixin {

    @Inject(method = "renderPost", at = @At("HEAD"), cancellable = true, require = 0)
    private static void vulkanMod$skipRenderPost(@Coerce Object stage, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "drawLights", at = @At("HEAD"), cancellable = true, require = 0)
    private static void vulkanMod$skipDrawLights(ProfilerFiller profiler, @Coerce Object cullFrustum, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "compositeLights", at = @At("HEAD"), cancellable = true, require = 0)
    private static void vulkanMod$skipCompositeLights(ProfilerFiller profiler, CallbackInfo ci) {
        ci.cancel();
    }
}
