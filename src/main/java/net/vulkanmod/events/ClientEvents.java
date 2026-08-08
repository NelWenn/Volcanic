package net.vulkanmod.events;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.vulkanmod.Initializer;
import net.vulkanmod.gui.debug.DebugOverlay;
import net.vulkanmod.gui.debug.FrameGraphOverlay;
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
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        for (HUD hud : HudHandler.getInstance().getHuds()) {
            while (hud.getToggleKeyMapping().consumeClick()) {
                hud.toggle();
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        for (HUD hud : HudHandler.getInstance().getHuds()) {
            if (hud.shouldRender() && hud.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(), event.getScrollDeltaY())) {
                event.setCanceled(true);
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        int button = -1;

        if (event.isAttack()) button = 0;
        else if (event.isUseItem()) button = 1;

        if (button == -1) return;

        for (HUD hud : HudHandler.getInstance().getHuds()) {
            if (hud.shouldRender() && hud.mouseButton(button)) {
                event.setCanceled(true);
                event.setSwingHand(false);
                break;
            }
        }
    }

    static {
        HudHandler.getInstance().registerOrdered(new DebugOverlay(), 0);
        HudHandler.getInstance().registerOrdered(new FrameGraphOverlay(), 1);
    }
}
