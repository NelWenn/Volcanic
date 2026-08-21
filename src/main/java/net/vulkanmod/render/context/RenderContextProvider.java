package net.vulkanmod.render.context;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

public class RenderContextProvider {
    private RenderContext context;

    private RenderContextProvider() {}

    public static RenderContextProvider getInstance() {
        return INSTANCE;
    }

    private static final RenderContextProvider INSTANCE = new RenderContextProvider();

    public RenderContext getOrCreate() {
        if (this.context == null)
            this.context = build();
        return this.context;
    }

    public RenderContext refresh() {
        this.context = build();
        return this.context;
    }

    private RenderContext build() {
        Minecraft mc = Minecraft.getInstance();

        return new Snapshot(
                mc,
                mc.player,
                mc.gameRenderer.getMainCamera(),
                mc.level
        );
    }

    public record Snapshot(
            Minecraft client,
            LocalPlayer player,
            Camera camera,
            ClientLevel level
    ) implements RenderContext {}
}
