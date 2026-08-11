package net.vulkanmod.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.minecraft.client.gui.screens.TitleScreen;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.ui.shell.UiKeybinds;
import net.vulkanmod.config.ui.shell.VolcanicScreen;
import net.vulkanmod.gui.debug.DebugOverlay;
import net.vulkanmod.gui.debug.FrameGraphOverlay;
import net.vulkanmod.gui.HUD;
import net.vulkanmod.gui.HudHandler;
import net.vulkanmod.render.profiling.FpsOverlay;
import net.vulkanmod.sound.UiSounds;

@EventBusSubscriber(value = Dist.CLIENT, modid = Initializer.MOD_ID)
public class ClientEvents {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        GuiGraphics guiGraphics = event.getGuiGraphics();
        HudHandler.getInstance().renderAll(guiGraphics);
    }

    @SubscribeEvent
    public static void onScreenOpened(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen) {
            UiSounds.playIntroOnce();
        }
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (HUD hud : HudHandler.getInstance().getHuds()) {
            event.register(hud.getToggleKeyMapping());
        }
        event.register(UiKeybinds.OPEN_OPTIONS);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        for (HUD hud : HudHandler.getInstance().getHuds()) {
            while (hud.getToggleKeyMapping().consumeClick()) {
                hud.toggle();
            }
        }

        while (UiKeybinds.OPEN_OPTIONS.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen == null) {
                minecraft.setScreen(new VolcanicScreen(
                        Component.translatable("vulkanmod.options.title"), null));
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
        HudHandler.getInstance().registerOrdered(new FpsOverlay(), 1);
        HudHandler.getInstance().registerOrdered(new FrameGraphOverlay(), 2);
    }
}
