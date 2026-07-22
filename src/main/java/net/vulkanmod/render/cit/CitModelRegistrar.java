package net.vulkanmod.render.cit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class CitModelRegistrar {
    private CitModelRegistrar() {}

    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        try {
            CitPackLoader.reload(Minecraft.getInstance().getResourceManager());
            for (ResourceLocation id : CitPackLoader.modelsToRegister()) {
                event.register(ModelResourceLocation.standalone(id));
            }
        } catch (Throwable t) {
            net.vulkanmod.Initializer.LOGGER.warn("CIT: model registration failed", t);
        }
    }
}
