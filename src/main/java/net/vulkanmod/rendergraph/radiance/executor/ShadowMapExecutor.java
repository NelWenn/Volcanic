package net.vulkanmod.rendergraph.radiance.executor;

import net.vulkanmod.render.framegraph.PassExecutor;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.pass.MainPass;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

public final class ShadowMapExecutor implements PassExecutor {
    @Override
    public void execute(VkCommandBuffer commandBuffer, MemoryStack stack) {
        MainPass mainPass = Renderer.getInstance().getMainPass();
        mainPass.getCapabilities().shadow().ifPresent(
                shadow -> shadow.renderShadowMap(commandBuffer, stack));
    }
}
