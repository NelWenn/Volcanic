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
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;

import static org.lwjgl.vulkan.VK10.*;

// Terrain rendered from the sun's POV into a depth texture; camera-relative like the main pass.
public class ShadowMap {
    private static final int[] RESOLUTIONS = { 1024, 2048, 3072, 4096, 6144 };

    public static int currentResolution() {
        int q = net.vulkanmod.Initializer.CONFIG.shadowQuality;
        return RESOLUTIONS[Math.max(0, Math.min(RESOLUTIONS.length - 1, q))];
    }

    private static final float EYE = 150.0f;        // eye distance along the sun axis
    private static final float HALF_DEPTH = 120.0f; // ortho half depth range; tight = high depth precision

    // shadow box half-extent (blocks)
    public static float radius() {
        return Math.max(16, net.vulkanmod.Initializer.CONFIG.shadowDistance);
    }

    // caster collection reach; past the box edge so border geometry still casts inward
    public static float shadowRange() {
        return radius() + 20.0f;
    }

    private Framebuffer framebuffer;
    private RenderPass renderPass;
    private RenderPass entityRenderPass;   // depth LOAD variant: adds entity depth on top of terrain depth
    private RenderPass tintRenderPass;     // colour CLEAR white + multiply: stained-glass tint of the sun light
    private int builtSize = -1;

    private static final int RSM_SIZE = 768;
    private Framebuffer rsmFramebuffer;
    private RenderPass rsmRenderPass;
    private int rsmCounter;
    private boolean rsmRendered;

    private final Matrix4f sunView = new Matrix4f();
    private final Matrix4f sunProj = new Matrix4f();

    private void ensureResources() {
        int size = currentResolution();
        if (this.framebuffer != null && this.builtSize == size) {
            return;
        }
        if (this.framebuffer != null) {
            this.framebuffer.cleanUp();
            this.framebuffer = null;
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
        if (this.rsmFramebuffer != null) {
            this.rsmFramebuffer.cleanUp();
            this.rsmFramebuffer = null;
        }
        if (this.rsmRenderPass != null) {
            this.rsmRenderPass.cleanUp();
            this.rsmRenderPass = null;
        }
        this.rsmRendered = false;
        this.builtSize = size;

        // colour is throwaway (DONT_CARE) to keep the terrain pipeline compatible; only depth is stored
        this.framebuffer = Framebuffer.builder(size, size, 1, true)
                .setDepthFormat(VK_FORMAT_D32_SFLOAT)
                .setDepthLinearFiltering(false)
                .build();

        RenderPass.Builder builder = RenderPass.builder(this.framebuffer);
        builder.getColorAttachmentInfo()
                .setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        builder.getDepthAttachmentInfo()
                .setOps(VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE)
                .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        this.renderPass = builder.build();

        // same target, but LOAD the terrain depth already written this frame so entities add to it
        RenderPass.Builder entityBuilder = RenderPass.builder(this.framebuffer);
        entityBuilder.getColorAttachmentInfo()
                .setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        entityBuilder.getDepthAttachmentInfo()
                .setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE)
                .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        this.entityRenderPass = entityBuilder.build();

        // tint pass: clear colour to white, multiply-blend glass tint; load depth read-only to occlude behind terrain
        RenderPass.Builder tintBuilder = RenderPass.builder(this.framebuffer);
        tintBuilder.getColorAttachmentInfo()
                .setOps(VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE)
                .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        tintBuilder.getDepthAttachmentInfo()
                .setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE)
                .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        this.tintRenderPass = tintBuilder.build();

        this.rsmFramebuffer = Framebuffer.builder(RSM_SIZE, RSM_SIZE, 1, true)
                .setDepthFormat(VK_FORMAT_D32_SFLOAT)
                .setDepthLinearFiltering(false)
                .build();
        RenderPass.Builder rsmBuilder = RenderPass.builder(this.rsmFramebuffer);
        rsmBuilder.getColorAttachmentInfo()
                .setOps(VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE)
                .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        rsmBuilder.getDepthAttachmentInfo()
                .setOps(VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE)
                .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        this.rsmRenderPass = rsmBuilder.build();
    }

    private final Vector3f sunFwd = new Vector3f();
    private final Vector3f sunUp0 = new Vector3f();
    private final Vector3f sunRight = new Vector3f();

    // sun ortho view-proj, camera-relative; texel-snapped to kill shimmer/crawl while moving
    private void computeSunMatrices(float sx, float sy, float sz, double camX, double camY, double camZ) {
        this.sunUp0.set(Math.abs(sy) > 0.99f ? 0.0f : 0.0f, Math.abs(sy) > 0.99f ? 0.0f : 1.0f, Math.abs(sy) > 0.99f ? 1.0f : 0.0f);
        this.sunFwd.set(-sx, -sy, -sz).normalize();
        this.sunFwd.cross(this.sunUp0, this.sunRight).normalize();

        // snap camera position to the shadow texel grid along the sun's lateral axes
        Vector3f up = new Vector3f(this.sunRight).cross(this.sunFwd).normalize();
        float R = radius();
        float texel = (2.0f * R) / this.builtSize;
        double cLat = camX * this.sunRight.x + camY * this.sunRight.y + camZ * this.sunRight.z;
        double cUp  = camX * up.x + camY * up.y + camZ * up.z;
        float dx = (float) (cLat - Math.round(cLat / texel) * texel);
        float dy = (float) (cUp - Math.round(cUp / texel) * texel);

        this.sunView.setLookAt(sx * EYE, sy * EYE, sz * EYE, 0.0f, 0.0f, 0.0f,
                this.sunUp0.x, this.sunUp0.y, this.sunUp0.z);
        // re-add sub-texel camera offset so the world texel grid stays fixed
        this.sunView.translateLocal(dx, dy, 0.0f);

        // GL-convention ortho (z in [-1,1]); applyProjectionMatrix converts to Vulkan
        this.sunProj.setOrtho(-R, R, -R, R, EYE - HALF_DEPTH, EYE + HALF_DEPTH);
    }

    // call at frame start, before the main world pass, so the fog resolve can sample it
    public void render(VkCommandBuffer commandBuffer, MemoryStack stack, float sx, float sy, float sz) {
        ensureResources();

        WorldRenderer worldRenderer = WorldRenderer.getInstance();
        Minecraft mc = Minecraft.getInstance();
        if (worldRenderer == null || mc.level == null) {
            return;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();

        computeSunMatrices(sx, sy, sz, cam.x, cam.y, cam.z);

        Renderer.clearViewportScale();   // no render-scale, full 1:1
        this.framebuffer.beginRenderPass(commandBuffer, this.renderPass, stack);

        VRenderSystem.applyMVP(this.sunView, this.sunProj);
        VRenderSystem.captureShadowMVP();
        VRenderSystem.captureShadowCameraPos(cam.x, cam.y, cam.z);

        worldRenderer.renderShadowTerrain(cam.x, cam.y, cam.z);

        this.renderPass.endRenderPass(commandBuffer);   // finalLayout: depth now SHADER_READ_ONLY

        renderRsm(commandBuffer, stack, worldRenderer, cam);
    }

    private void renderRsm(VkCommandBuffer commandBuffer, MemoryStack stack, WorldRenderer worldRenderer, Vec3 cam) {
        if (!"radiance".equals(net.vulkanmod.Initializer.CONFIG.selectedShader) || this.rsmFramebuffer == null) {
            return;
        }
        boolean due = !this.rsmRendered || ++this.rsmCounter >= 4;
        if (!due) {
            return;
        }
        this.rsmCounter = 0;

        this.rsmFramebuffer.getDepthAttachment().transitionImageLayout(
                stack, commandBuffer, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
        this.rsmFramebuffer.getColorAttachment().transitionImageLayout(
                stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        Renderer.clearViewportScale();
        float r = VRenderSystem.clearColor.get(0), g = VRenderSystem.clearColor.get(1),
                b = VRenderSystem.clearColor.get(2), a = VRenderSystem.clearColor.get(3);
        VRenderSystem.setClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        this.rsmFramebuffer.beginRenderPass(commandBuffer, this.rsmRenderPass, stack);
        VRenderSystem.setClearColor(r, g, b, a);

        VkViewport.Buffer viewport = this.rsmFramebuffer.viewport(stack);
        vkCmdSetViewport(commandBuffer, 0, viewport);
        VkRect2D.Buffer scissor = this.rsmFramebuffer.scissor(stack);
        vkCmdSetScissor(commandBuffer, 0, scissor);

        VRenderSystem.applyMVP(this.sunView, this.sunProj);
        VRenderSystem.captureRsmMVP();

        worldRenderer.renderShadowRsm(cam.x, cam.y, cam.z);

        this.rsmRenderPass.endRenderPass(commandBuffer);
        this.rsmRendered = true;
    }

    public boolean rsmPending() {
        return !this.rsmRendered;
    }

    public VulkanImage getRsmColorImage() {
        return this.rsmRendered && this.rsmFramebuffer != null ? this.rsmFramebuffer.getColorAttachment() : null;
    }

    public VulkanImage getRsmDepthImage() {
        return this.rsmRendered && this.rsmFramebuffer != null ? this.rsmFramebuffer.getDepthAttachment() : null;
    }

    // begin a second pass into the SAME depth texture, loading this frame's terrain depth
    public void beginEntityPass(VkCommandBuffer commandBuffer, MemoryStack stack) {
        if (this.framebuffer == null || this.entityRenderPass == null) {
            return;
        }

        this.framebuffer.getDepthAttachment().transitionImageLayout(
                stack, commandBuffer, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

        Renderer.clearViewportScale();
        this.framebuffer.beginRenderPass(commandBuffer, this.entityRenderPass, stack);

        // explicit 1:1 viewport/scissor: the main pass left them at the main-framebuffer size
        VkViewport.Buffer viewport = this.framebuffer.viewport(stack);
        vkCmdSetViewport(commandBuffer, 0, viewport);
        VkRect2D.Buffer scissor = this.framebuffer.scissor(stack);
        vkCmdSetScissor(commandBuffer, 0, scissor);

        VRenderSystem.applyMVP(this.sunView, this.sunProj);
    }

    public void endEntityPass(VkCommandBuffer commandBuffer, MemoryStack stack) {
        if (this.framebuffer == null || this.entityRenderPass == null) {
            return;
        }
        this.entityRenderPass.endRenderPass(commandBuffer);   // finalLayout: depth back to SHADER_READ_ONLY
    }

    public void beginTintPass(VkCommandBuffer commandBuffer, MemoryStack stack) {
        if (this.framebuffer == null || this.tintRenderPass == null) {
            return;
        }
        this.framebuffer.getDepthAttachment().transitionImageLayout(
                stack, commandBuffer, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

        Renderer.clearViewportScale();
        float r = VRenderSystem.clearColor.get(0), g = VRenderSystem.clearColor.get(1),
                b = VRenderSystem.clearColor.get(2), a = VRenderSystem.clearColor.get(3);
        VRenderSystem.setClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        this.framebuffer.beginRenderPass(commandBuffer, this.tintRenderPass, stack);
        VRenderSystem.setClearColor(r, g, b, a);

        VkViewport.Buffer viewport = this.framebuffer.viewport(stack);
        vkCmdSetViewport(commandBuffer, 0, viewport);
        VkRect2D.Buffer scissor = this.framebuffer.scissor(stack);
        vkCmdSetScissor(commandBuffer, 0, scissor);

        VRenderSystem.applyMVP(this.sunView, this.sunProj);
    }

    public void endTintPass(VkCommandBuffer commandBuffer, MemoryStack stack) {
        if (this.framebuffer == null || this.tintRenderPass == null) {
            return;
        }
        this.tintRenderPass.endRenderPass(commandBuffer);
    }

    public VulkanImage getColorImage() {
        return this.framebuffer != null ? this.framebuffer.getColorAttachment() : null;
    }

    public Matrix4f getSunView() {
        return this.sunView;
    }

    public Matrix4f getSunProj() {
        return this.sunProj;
    }

    public VulkanImage getDepthImage() {
        return this.framebuffer != null ? this.framebuffer.getDepthAttachment() : null;
    }

    public boolean isReady() {
        return this.framebuffer != null;
    }
}
