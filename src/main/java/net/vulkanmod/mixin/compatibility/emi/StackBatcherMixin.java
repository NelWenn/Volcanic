package net.vulkanmod.mixin.compatibility.emi;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.emi.emi.screen.StackBatcher", remap = false)
public class StackBatcherMixin {

    @Inject(method = "isEnabled", at = @At("HEAD"), cancellable = true, require = 0)
    private static void vulkanMod$forceUnbatched(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
