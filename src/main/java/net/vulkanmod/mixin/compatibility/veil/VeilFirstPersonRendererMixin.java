package net.vulkanmod.mixin.compatibility.veil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "foundry.veil.impl.client.render.pipeline.VeilFirstPersonRenderer", remap = false)
public class VeilFirstPersonRendererMixin {

    @Inject(method = "bind", at = @At("HEAD"), cancellable = true, require = 0)
    private static void vulkanMod$skipBind(int mask, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "unbind", at = @At("HEAD"), cancellable = true, require = 0)
    private static void vulkanMod$skipUnbind(CallbackInfo ci) {
        ci.cancel();
    }
}
