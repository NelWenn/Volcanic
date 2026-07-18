package net.vulkanmod.vulkan.shader.pipeline;

import static org.lwjgl.vulkan.VK10.*;

public enum Stage {
    VERTEX(VK_SHADER_STAGE_VERTEX_BIT),
    FRAGMENT(VK_SHADER_STAGE_FRAGMENT_BIT),
    ALL(VK_SHADER_STAGE_ALL_GRAPHICS),
    COMPUTE(VK_SHADER_STAGE_COMPUTE_BIT);

    private final int bits;

    Stage(int bits) {
        this.bits = bits;
    }

    public int bits() {
        return bits;
    }
}
