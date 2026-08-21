package net.vulkanmod.rendergraph.radiance;

import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.pass.EngineContext;
import net.vulkanmod.vulkan.pass.MaterialProvider;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;

import static org.lwjgl.vulkan.VK10.*;

public final class RadianceMaterialProvider implements MaterialProvider {
    private EngineContext context;
    private Runnable rebindTarget;

    private Framebuffer materialFramebuffer;
    private RenderPass materialRenderPass;
    private int materialW = -1, materialH = -1;
    private final Matrix4f materialModelView = new Matrix4f();
    private final Matrix4f materialProjection = new Matrix4f();
    private double materialCamX, materialCamY, materialCamZ;
    private boolean materialViewReady;

    public void setRebindTarget(Runnable rebindTarget) {
        this.rebindTarget = rebindTarget;
    }

    @Override
    public void initialize(EngineContext context) {
        this.context = context;
    }

    private boolean glassMaterialActive() {
        return context.postShaderActive() && context.isScaledFramebuffer()
                && Initializer.CONFIG.glassReflections;
    }

    private void ensureMaterialTarget(int w, int h) {
        if (this.materialFramebuffer != null && this.materialW == w && this.materialH == h)
            return;

        if (this.materialFramebuffer != null) {
            this.materialFramebuffer.cleanUp();
            this.materialFramebuffer = null;
        }

        if (this.materialRenderPass != null) {
            this.materialRenderPass.cleanUp();
            this.materialRenderPass = null;
        }

        this.materialFramebuffer = Framebuffer.builder(w, h, 1, true)
                .setFormat(VK_FORMAT_R8_UNORM)
                .setDepthFormat(VK_FORMAT_D32_SFLOAT)
                .setLinearFiltering(false)
                .setDepthLinearFiltering(false)
                .build();

        RenderPass.Builder builder = RenderPass.builder(this.materialFramebuffer);

        builder.getColorAttachmentInfo()
                .setOps(VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE)
                .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

        builder.getDepthAttachmentInfo()
                .setOps(VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE)
                .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

        this.materialRenderPass = builder.build();

        this.materialW = w;
        this.materialH = h;
    }

    @Override
    public void prepareMaterialBuffer(double camX, double camY, double camZ,
                                      Matrix4f modelView, Matrix4f projection) {
        this.materialCamX = camX;
        this.materialCamY = camY;
        this.materialCamZ = camZ;
        this.materialModelView.set(modelView);
        this.materialProjection.set(projection);
        this.materialViewReady = true;
    }

    @Override
    public void renderMaterialBuffer() {
        boolean ready = this.materialViewReady;
        this.materialViewReady = false;
        if (!ready || !glassMaterialActive()) {
            clearMaterialBuffer();
            return;
        }

        WorldRenderer worldRenderer = WorldRenderer.getInstance();

        if (worldRenderer == null) {
            clearMaterialBuffer();
            return;
        }

        Framebuffer mainFb = context.mainFramebuffer();
        VulkanImage depthSrc = mainFb.getDepthAttachment();
        ensureMaterialTarget(depthSrc.width, depthSrc.height);

        if (this.materialFramebuffer == null)
            return;

        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
        Renderer.getInstance().endRenderPass(commandBuffer);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.materialFramebuffer.getColorAttachment().transitionImageLayout(
                    stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            this.materialFramebuffer.getDepthAttachment().transitionImageLayout(
                    stack, commandBuffer, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            Renderer.clearViewportScale();
            float cr = VRenderSystem.clearColor.get(0), cg = VRenderSystem.clearColor.get(1);
            float cb = VRenderSystem.clearColor.get(2), ca = VRenderSystem.clearColor.get(3);

            VRenderSystem.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            this.materialFramebuffer.beginRenderPass(commandBuffer, this.materialRenderPass, stack);
            VRenderSystem.setClearColor(cr, cg, cb, ca);

            VkViewport.Buffer viewport = this.materialFramebuffer.viewport(stack);
            vkCmdSetViewport(commandBuffer, 0, viewport);
            VkRect2D.Buffer scissor = this.materialFramebuffer.scissor(stack);
            vkCmdSetScissor(commandBuffer, 0, scissor);

            VRenderSystem.applyMVP(this.materialModelView, this.materialProjection);
            worldRenderer.renderMaterialTerrain(this.materialCamX, this.materialCamY, this.materialCamZ);

            Renderer.getInstance().endRenderPass(commandBuffer);
        }

        if (rebindTarget != null) rebindTarget.run();
    }

    private void clearMaterialBuffer() {
        if (this.materialFramebuffer == null || this.materialRenderPass == null)
            return;

        if (!context.postShaderActive() || !context.isScaledFramebuffer())
            return;

        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
        Renderer.getInstance().endRenderPass(commandBuffer);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.materialFramebuffer.getColorAttachment().transitionImageLayout(
                    stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            this.materialFramebuffer.getDepthAttachment().transitionImageLayout(
                    stack, commandBuffer, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            Renderer.clearViewportScale();
            float cr = VRenderSystem.clearColor.get(0), cg = VRenderSystem.clearColor.get(1);
            float cb = VRenderSystem.clearColor.get(2), ca = VRenderSystem.clearColor.get(3);

            VRenderSystem.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            this.materialFramebuffer.beginRenderPass(commandBuffer, this.materialRenderPass, stack);

            VRenderSystem.setClearColor(cr, cg, cb, ca);
            Renderer.getInstance().endRenderPass(commandBuffer);
        }

        if (rebindTarget != null)
            rebindTarget.run();
    }

    @Override
    public VulkanImage getMaterialImage() {
        return this.materialFramebuffer != null ? this.materialFramebuffer.getColorAttachment() : null;
    }

    @Override
    public VulkanImage getMaterialDepthImage() {
        return this.materialFramebuffer != null ? this.materialFramebuffer.getDepthAttachment() : null;
    }

    @Override
    public void cleanup() {
        if (this.materialRenderPass != null) {
            this.materialRenderPass.cleanUp();
            this.materialRenderPass = null;
        }

        if (this.materialFramebuffer != null) {
            this.materialFramebuffer.cleanUp();
            this.materialFramebuffer = null;
        }
    }

    @Override
    public boolean isReady() {
        return true;
    }
}
