package net.vulkanmod.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

/**
 * Provided in plugins to help developers accessing to game details
 * Immutable and updated each frame
 */
public interface RenderContext {
    Minecraft   client();
    LocalPlayer player();
    Camera      camera();
    ClientLevel level();
    float       partialTick();
    long        frameIndex();
}
