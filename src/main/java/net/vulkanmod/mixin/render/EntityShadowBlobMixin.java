package net.vulkanmod.mixin.render;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.vulkanmod.Initializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityShadowBlobMixin {

    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void volcanic$skipBlob(CallbackInfo ci) {
        if (Initializer.CONFIG.shadersEnabled && Initializer.CONFIG.isCamille()
                && Initializer.CONFIG.shadowsEnabled && Initializer.CONFIG.entityShadows) {
            ci.cancel();
        }
    }
}
