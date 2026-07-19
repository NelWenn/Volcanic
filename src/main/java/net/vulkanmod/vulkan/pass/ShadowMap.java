package net.vulkanmod.vulkan.pass;

import net.minecraft.client.Minecraft;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkViewport;
import org.lwjgl.vulkan.VkRect2D;

import static org.lwjgl.vulkan.VK10.*;

public class ShadowMap {
    public static final int CASCADES = 3;

    private static final int[] CASCADE_RES = { 1024, 1280, 1536, 2048, 2560 };
    private static final float[] NEAR_RADIUS = { 24.0f, 72.0f };

    public static int cascadeResolution() {
        int q = net.vulkanmod.Initializer.CONFIG.shadowQuality;
        return CASCADE_RES[Math.max(0, Math.min(CASCADE_RES.length - 1, q))];
    }

    public static int currentResolution() {
        return cascadeResolution();
    }

    private static final float EYE = 150.0f;
    private static final float HALF_DEPTH = 120.0f;

    public static float farRadius() {
        return Math.max(16, net.vulkanmod.Initializer.CONFIG.shadowDistance);
    }

    public static float cascadeRadius(int i) {
        float far = farRadius();
        if (i >= CASCADES - 1) {
            return far;
        }
        return Math.min(NEAR_RADIUS[i], far);
    }

    public static float shadowRange() {
        return farRadius() + 20.0f;
    }

    private Framebuffer[][] cascadeFb;
    private RenderPass renderPass;
    private RenderPass entityRenderPass;
    private RenderPass tintRenderPass;
    private int builtSize = -1;

    private final Matrix4f[] sunView = new Matrix4f[CASCADES];
    private final Matrix4f[] sunProj = new Matrix4f[CASCADES];

    public ShadowMap() {
        for (int i = 0; i < CASCADES; i++) {
            this.sunView[i] = new Matrix4f();
            this.sunProj[i] = new Matrix4f();
        }
    }

    private void ensureResources() {
        int size = cascadeResolution();
        int frames = Renderer.getFramesNum();
        if (this.cascadeFb != null && this.builtSize == size && this.cascadeFb[0].length == frames) {
            return;
        }
        if (this.cascadeFb != null) {
            for (Framebuffer[] perFrame : this.cascadeFb) {
                for (Framebuffer fb : perFrame) {
                    if (fb != null) fb.cleanUp();
                }
            }
        }
        if (this.renderPass != null) {
            this.renderPass.cleanUp();
            this.renderPass = null;
        }
        if (this.entityRenderPass != null) {
            this.entityRenderPass.cleanUp();
            this.entityRenderPass = null;
        }
        if (this.tintRenderPass != null) {
            this.tintRenderPass.cleanUp();
            this.tintRenderPass = null;
        }
        this.builtSize = size;

        this.cascadeFb = new Framebuffer[CASCADES][frames];
        for (int i = 0; i < CASCADES; i++) {
            for (int f = 0; f < frames; f++) {
                this.cascadeFb[i][f] = Framebuffer.builder(size, size, 1, true)
                        .setDepthFormat(VK_FORMAT_D32_SFLOAT)
                        .setDepthLinearFiltering(false)
                        .build();
            }
        }

        RenderPass.Builder builder = RenderPass.builder(this.cascadeFb[0][0]);
        builder.getColorAttachmentInfo()
                .setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        builder.getDepthAttachmentInfo()
                .setOps(VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE)
                .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        this.renderPass = builder.build();

        RenderPass.Builder entityBuilder = RenderPass.builder(this.cascadeFb[0][0]);
        entityBuilder.getColorAttachmentInfo()
                .setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        entityBuilder.getDepthAttachmentInfo()
                .setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE)
                .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        this.entityRenderPass = entityBuilder.build();

        RenderPass.Builder tintBuilder = RenderPass.builder(this.cascadeFb[0][0]);
        tintBuilder.getColorAttachmentInfo()
                .setOps(VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE)
                .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        tintBuilder.getDepthAttachmentInfo()
                .setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE)
                .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        this.tintRenderPass = tintBuilder.build();
    }

    public void beginTintPass(int cascade, VkCommandBuffer commandBuffer, MemoryStack stack) {
        if (this.cascadeFb == null || this.tintRenderPass == null) {
            return;
        }
        Framebuffer fb = this.cascadeFb[cascade][Renderer.getCurrentFrame()];
        fb.getColorAttachment().transitionImageLayout(
                stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        fb.getDepthAttachment().transitionImageLayout(
                stack, commandBuffer, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

        Renderer.clearViewportScale();
        float r = VRenderSystem.clearColor.get(0), g = VRenderSystem.clearColor.get(1);
        float b = VRenderSystem.clearColor.get(2), a = VRenderSystem.clearColor.get(3);
        VRenderSystem.setClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        fb.beginRenderPass(commandBuffer, this.tintRenderPass, stack);
        VRenderSystem.setClearColor(r, g, b, a);

        VkViewport.Buffer viewport = fb.viewport(stack);
        vkCmdSetViewport(commandBuffer, 0, viewport);
        VkRect2D.Buffer scissor = fb.scissor(stack);
        vkCmdSetScissor(commandBuffer, 0, scissor);

        VRenderSystem.applyMVP(this.sunView[cascade], this.sunProj[cascade]);
    }

    public void endTintPass(VkCommandBuffer commandBuffer, MemoryStack stack) {
        if (this.cascadeFb == null || this.tintRenderPass == null) {
            return;
        }
        this.tintRenderPass.endRenderPass(commandBuffer);
    }

    public VulkanImage getTintImage(int cascade) {
        if (this.cascadeFb == null) return null;
        return this.cascadeFb[cascade][Renderer.getCurrentFrame()].getColorAttachment();
    }

    public void beginEntityPass(VkCommandBuffer commandBuffer, MemoryStack stack) {
        if (this.cascadeFb == null || this.entityRenderPass == null) {
            return;
        }
        Framebuffer fb = this.cascadeFb[0][Renderer.getCurrentFrame()];
        fb.getDepthAttachment().transitionImageLayout(
                stack, commandBuffer, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

        Renderer.clearViewportScale();
        fb.beginRenderPass(commandBuffer, this.entityRenderPass, stack);

        VkViewport.Buffer viewport = fb.viewport(stack);
        vkCmdSetViewport(commandBuffer, 0, viewport);
        VkRect2D.Buffer scissor = fb.scissor(stack);
        vkCmdSetScissor(commandBuffer, 0, scissor);

        VRenderSystem.applyMVP(this.sunView[0], this.sunProj[0]);
    }

    public void endEntityPass(VkCommandBuffer commandBuffer, MemoryStack stack) {
        if (this.cascadeFb == null || this.entityRenderPass == null) {
            return;
        }
        this.entityRenderPass.endRenderPass(commandBuffer);
    }

    private final Vector3f sunFwd = new Vector3f();
    private final Vector3f sunUp0 = new Vector3f();
    private final Vector3f sunRight = new Vector3f();

    private void computeCascade(int cascade, float sx, float sy, float sz, double camX, double camY, double camZ) {
        this.sunUp0.set(Math.abs(sy) > 0.99f ? 0.0f : 0.0f, Math.abs(sy) > 0.99f ? 0.0f : 1.0f, Math.abs(sy) > 0.99f ? 1.0f : 0.0f);
        this.sunFwd.set(-sx, -sy, -sz).normalize();
        this.sunFwd.cross(this.sunUp0, this.sunRight).normalize();

        Vector3f up = new Vector3f(this.sunRight).cross(this.sunFwd).normalize();
        float R = cascadeRadius(cascade);
        float texel = (2.0f * R) / this.builtSize;
        double cLat = camX * this.sunRight.x + camY * this.sunRight.y + camZ * this.sunRight.z;
        double cUp  = camX * up.x + camY * up.y + camZ * up.z;
        float dx = (float) (cLat - Math.round(cLat / texel) * texel);
        float dy = (float) (cUp - Math.round(cUp / texel) * texel);

        this.sunView[cascade].setLookAt(sx * EYE, sy * EYE, sz * EYE, 0.0f, 0.0f, 0.0f,
                this.sunUp0.x, this.sunUp0.y, this.sunUp0.z);
        this.sunView[cascade].translateLocal(dx, dy, 0.0f);
        this.sunProj[cascade].setOrtho(-R, R, -R, R, EYE - HALF_DEPTH, EYE + HALF_DEPTH);
    }

    public void render(VkCommandBuffer commandBuffer, MemoryStack stack, float sx, float sy, float sz) {
        ensureResources();

        WorldRenderer worldRenderer = WorldRenderer.getInstance();
        Minecraft mc = Minecraft.getInstance();
        if (worldRenderer == null || mc.level == null) {
            return;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();

        VRenderSystem.captureShadowCameraPos(cam.x, cam.y, cam.z);
        VRenderSystem.captureShadowSplits(cascadeRadius(0), cascadeRadius(1), cascadeRadius(2));

        int frame = Renderer.getCurrentFrame();
        for (int i = 0; i < CASCADES; i++) {
            computeCascade(i, sx, sy, sz, cam.x, cam.y, cam.z);

            Renderer.clearViewportScale();
            this.cascadeFb[i][frame].getColorAttachment().transitionImageLayout(
                    stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            this.cascadeFb[i][frame].beginRenderPass(commandBuffer, this.renderPass, stack);

            VRenderSystem.applyMVP(this.sunView[i], this.sunProj[i]);
            VRenderSystem.captureShadowCascadeMVP(i);

            worldRenderer.renderShadowTerrain(cam.x, cam.y, cam.z, i, cascadeRadius(i));

            this.renderPass.endRenderPass(commandBuffer);
        }
    }

    public VulkanImage getCascadeDepthImage(int i) {
        if (this.cascadeFb == null) return null;
        return this.cascadeFb[i][Renderer.getCurrentFrame()].getDepthAttachment();
    }

    public boolean isReady() {
        return this.cascadeFb != null;
    }
}
