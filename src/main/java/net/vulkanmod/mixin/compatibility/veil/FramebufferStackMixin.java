package net.vulkanmod.mixin.compatibility.veil;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "foundry.veil.api.client.render.framebuffer.FramebufferStack", remap = false)
public class FramebufferStackMixin {

    @Inject(method = "push", at = @At("HEAD"), cancellable = true, require = 0)
    private static void vulkanMod$skipPush(ResourceLocation name, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "pop", at = @At("HEAD"), cancellable = true, require = 0)
    private static void vulkanMod$skipPop(ResourceLocation name, CallbackInfo ci) {
        ci.cancel();
    }
}
