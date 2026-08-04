package net.vulkanmod.mixin.compatibility.fml;

import net.neoforged.fml.loading.ImmediateWindowHandler;
import net.vulkanmod.compat.EarlyWindowCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.lang.reflect.Field;

@Mixin(value = ImmediateWindowHandler.class, remap = false)
public class ImmediateWindowHandlerMixin {

    /**
     * @author RevoJava
     * @reason Render tick invoke
     */
    @Overwrite
    public static void renderTick() {
        if (EarlyWindowCompat.isHandoffComplete()) {
            return;
        }

        try {
            Field providerField = ImmediateWindowHandler.class.getDeclaredField("provider");
            providerField.setAccessible(true);
            Object provider = providerField.get(null);
            if (provider != null) {
                provider.getClass().getMethod("renderTick").invoke(provider);
            }
        } catch (Throwable t) {

        }
    }
}
