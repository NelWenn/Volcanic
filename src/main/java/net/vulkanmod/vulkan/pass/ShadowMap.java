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
    private int builtSize = -1;

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

    // call at frame start, before the main world pass, so the resolve can sample it
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
