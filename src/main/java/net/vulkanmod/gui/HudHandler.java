package net.vulkanmod.gui;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HudHandler {

    private static final HudHandler INSTANCE = new HudHandler();

    private final List<HUD> huds = new ArrayList<>();

    public static HudHandler getInstance() {
        return INSTANCE;
    }

    public void register(HUD hud) {
        if (hud == null) throw new IllegalArgumentException("HUD cannot be null");

        if (!huds.contains(hud)) {
            huds.add(hud);
        }
    }

    public void registerOrdered(HUD hud, int priority) {
        register(hud);
        hud.setPriority(priority);
        huds.sort(Comparator.comparingInt(HUD::getPriority));
    }

    public void unregister(HUD hud) {
        huds.remove(hud);
    }

    public void renderAll(GuiGraphics guiGraphics) {
        for (HUD hud : huds) {
            if (hud.shouldRender()) {
                hud.render(guiGraphics);
            }
        }
    }

    public List<HUD> getHuds() {
        return List.copyOf(huds);
    }
}
