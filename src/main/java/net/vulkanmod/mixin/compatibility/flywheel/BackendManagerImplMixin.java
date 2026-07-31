package net.vulkanmod.mixin.compatibility.flywheel;

import dev.engine_room.flywheel.api.backend.Backend;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.engine_room.flywheel.impl.BackendManagerImpl", remap = false)
public class BackendManagerImplMixin {

    @Shadow @Final public static Backend OFF_BACKEND;

    @Shadow private static Backend backend;

    @Inject(method = "chooseBackend", at = @At("TAIL"), require = 0)
    private static void vulkanMod$forceOff(CallbackInfo ci) {
        backend = OFF_BACKEND;
    }
}
