package net.vulkanmod.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.ui.shell.VolcanicScreen;
import net.vulkanmod.gui.DebugOverlay;
import net.vulkanmod.gui.HUD;
import net.vulkanmod.gui.HudHandler;

@EventBusSubscriber(value = Dist.CLIENT, modid = Initializer.MOD_ID)
public class ClientEvents {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        GuiGraphics guiGraphics = event.getGuiGraphics();
        HudHandler.getInstance().renderAll(guiGraphics);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (HUD hud : HudHandler.getInstance().getHuds()) {
            event.register(hud.getToggleKeyMapping());
        }
        event.register(HUD.optionsKeyMapping);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        for (HUD hud : HudHandler.getInstance().getHuds()) {
            while (hud.getToggleKeyMapping().consumeClick()) {
                hud.toggle();
            }
        }

        while (HUD.optionsKeyMapping.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen == null) {
                minecraft.setScreen(new VolcanicScreen(
                        Component.translatable("vulkanmod.options.title"), null));
            }
        }
    }

    static {
        HudHandler.getInstance().registerOrdered(new DebugOverlay(), 0);
    }
}
