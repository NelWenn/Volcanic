package net.vulkanmod.compat.sable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.vulkanmod.compat.CompatDetector;

public final class SableReloadNotice {
    private static final String MOD_ID = "sable";
    private static final String TITLE = "vulkanmod.compat.sable.reload.title";
    private static final String MESSAGE = "vulkanmod.compat.sable.reload.message";

    private static boolean loadedOnce;

    private SableReloadNotice() {
    }

    public static void onResourceReload() {
        if (!loadedOnce) {
            loadedOnce = true;
            return;
        }
        try {
            if (!CompatDetector.isModLoaded(MOD_ID)) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.getToasts() == null) {
                return;
            }
            SystemToast.addOrUpdate(minecraft.getToasts(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    Component.translatable(TITLE), Component.translatable(MESSAGE));
        } catch (Throwable unavailable) {
            net.vulkanmod.Initializer.LOGGER.debug("Could not show the Sable reload notice", unavailable);
        }
    }
}
