package net.vulkanmod.render.framegraph;

import static org.lwjgl.vulkan.VK10.*;

public enum Format {
    RGBA16F(VK_FORMAT_R16G16B16A16_SFLOAT),
    RGBA8(VK_FORMAT_R8G8B8A8_UNORM),
    R16F(VK_FORMAT_R16_SFLOAT),
    RG16F(VK_FORMAT_R16G16_SFLOAT);

    public final int vk;

    Format(int vk) {
        this.vk = vk;
    }
}
