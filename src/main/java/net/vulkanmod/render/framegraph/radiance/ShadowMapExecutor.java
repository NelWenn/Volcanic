package net.vulkanmod.render.framegraph.radiance;

import net.vulkanmod.render.framegraph.PassExecutor;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.pass.DefaultMainPass;
import net.vulkanmod.vulkan.pass.MainPass;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

public final class ShadowMapExecutor implements PassExecutor {
    @Override
    public void execute(VkCommandBuffer commandBuffer, MemoryStack stack) {
        MainPass mainPass = Renderer.getInstance().getMainPass();
        if (mainPass instanceof DefaultMainPass pass) {
            pass.renderShadowMap(commandBuffer, stack);
        }
    }
}
