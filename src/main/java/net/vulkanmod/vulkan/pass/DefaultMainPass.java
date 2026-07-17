package net.vulkanmod.vulkan.pass;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.RenderScale;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.DrawUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkClearColorValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkImageCopy;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;

import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class DefaultMainPass implements MainPass {

    public static DefaultMainPass create() {
        return new DefaultMainPass();
    }

    private RenderTarget mainTarget;
    private final SwapChain swapChain;
    private Framebuffer mainFramebuffer;
    private Framebuffer scaledFramebuffer;

    private VulkanImage capturedWorldDepth;        // world depth, before the hand's clear
    private VulkanImage capturedForegroundDepth;   // hand depth, before the pre-GUI clear
    private int scaledDepthClears;
    // pre-GUI clear suppressed: live depth still holds world+hand, sample it directly
    private boolean liveDepthIsForeground;

    private final ShadowMap shadowMap = new ShadowMap();

    private static final double SHADOW_MOVE_THRESHOLD_SQ = 0.35 * 0.35;   // blocks² since last render
    private static final float SHADOW_DRIFT_TOLERANCE = 1.15f;
    private double lastShadowCamX, lastShadowCamY, lastShadowCamZ;
    private float lastShadowLx, lastShadowLy;
    private int lastShadowGeometryVersion = -1;
    private int lastShadowQuality = -1, lastShadowDistance = -1;
    private boolean shadowRenderedOnce;

    private RenderPass mainRenderPass;
    private RenderPass auxRenderPass;
    private RenderPass presentRenderPass;

    // half-res terms ping-pong (RGBA16F); one target renders while the other is sampled as history
    private final Framebuffer[] termsFramebuffers = new Framebuffer[2];
    private final RenderPass[] termsRenderPasses = new RenderPass[2];
    private int termsIndex;
    private int termsW = -1, termsH = -1;

    private int scaledFramebufferWidth = -1;
    private int scaledFramebufferHeight = -1;
    private int scaledFramebufferScale = RenderScale.DEFAULT;
    private int scaledColorAttachmentGlId = -1;
    private int scaledDepthAttachmentGlId = -1;
    private boolean renderScaleResolvedThisFrame;
    private boolean guiResolveHandledThisFrame;

    DefaultMainPass() {
        this.mainTarget = Minecraft.getInstance().getMainRenderTarget();
        this.swapChain = Vulkan.getSwapChain();
        this.mainFramebuffer = this.swapChain;

        createRenderPasses();
        createPresentRenderPass();
    }

    private void createRenderPasses() {
        RenderPass.Builder builder = RenderPass.builder(this.mainFramebuffer);
        builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getDepthAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);

        this.mainRenderPass = builder.build();

        builder = RenderPass.builder(this.mainFramebuffer);
        builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getDepthAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        this.auxRenderPass = builder.build();
    }

    private void createPresentRenderPass() {
        RenderPass.Builder builder = RenderPass.builder(this.swapChain);
        builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);
        // clear swapchain depth: the GUI depth-tests in this pass and its depth is otherwise never cleared
        builder.getDepthAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_DONT_CARE);

        this.presentRenderPass = builder.build();
    }

    private void setMainFramebuffer(Framebuffer framebuffer) {
        if (this.mainFramebuffer == framebuffer) {
            return;
        }

        if (this.mainRenderPass != null) {
            this.mainRenderPass.cleanUp();
        }
        if (this.auxRenderPass != null) {
            this.auxRenderPass.cleanUp();
        }

        this.mainFramebuffer = framebuffer;
        createRenderPasses();
    }

    private void ensureMainFramebuffer() {
        int scale = RenderScale.clamp(Initializer.CONFIG.renderScale);

        if (!shouldUseScaledFramebuffer(scale)) {
            disposeScaledFramebuffer();
            setMainFramebuffer(this.swapChain);
            return;
        }

        int scaledWidth = RenderScale.scaleDimension(this.swapChain.getWidth(), scale);
        int scaledHeight = RenderScale.scaleDimension(this.swapChain.getHeight(), scale);

        if (this.scaledFramebuffer == null
                || this.scaledFramebufferWidth != scaledWidth
                || this.scaledFramebufferHeight != scaledHeight
                || this.scaledFramebufferScale != scale) {
            disposeScaledFramebuffer();

            this.scaledFramebuffer = Framebuffer.builder(scaledWidth, scaledHeight, 1, true)
                    .setLinearFiltering(true)
                    // depth-only format (no stencil) so post shaders can sample it as a sampler2D
                    .setDepthFormat(org.lwjgl.vulkan.VK10.VK_FORMAT_D32_SFLOAT)
                    .build();
            this.scaledColorAttachmentGlId = GlTexture.genTextureId();
            GlTexture.bindIdToImage(this.scaledColorAttachmentGlId, this.scaledFramebuffer.getColorAttachment());
            this.scaledDepthAttachmentGlId = GlTexture.genTextureId();
            GlTexture.bindIdToImage(this.scaledDepthAttachmentGlId, this.scaledFramebuffer.getDepthAttachment());
            this.scaledFramebufferWidth = scaledWidth;
            this.scaledFramebufferHeight = scaledHeight;
            this.scaledFramebufferScale = scale;
        }

        setMainFramebuffer(this.scaledFramebuffer);
    }

    private boolean shouldUseScaledFramebuffer(int scale) {
        Minecraft minecraft = Minecraft.getInstance();

        boolean base = this.swapChain.getWidth() > 0
                && this.swapChain.getHeight() > 0
                && !this.renderScaleResolvedThisFrame
                && minecraft.level != null;

        // post shader needs an offscreen color target; keep it on with a GUI open too
        if (postShaderActive())
            return base;

        // only downscale the world in-game (crisp menus)
        return RenderScale.isScaled(scale) && base && minecraft.screen == null;
    }

    public static boolean postShaderActive() {
        return Initializer.CONFIG.shadersEnabled && !"off".equals(Initializer.CONFIG.selectedShader);
    }

    private void disposeScaledFramebuffer() {
        if (this.scaledFramebuffer != null) {
            this.scaledFramebuffer.cleanUp();
            this.scaledFramebuffer = null;
        }

        if (this.capturedWorldDepth != null) {
            this.capturedWorldDepth.free();
            this.capturedWorldDepth = null;
        }

        if (this.capturedForegroundDepth != null) {
            this.capturedForegroundDepth.free();
            this.capturedForegroundDepth = null;
        }

        if (this.scaledColorAttachmentGlId != -1) {
            GlTexture.setVulkanImage(this.scaledColorAttachmentGlId, null);
            this.scaledColorAttachmentGlId = -1;
        }

        if (this.scaledDepthAttachmentGlId != -1) {
            GlTexture.setVulkanImage(this.scaledDepthAttachmentGlId, null);
            this.scaledDepthAttachmentGlId = -1;
        }

        this.scaledFramebufferWidth = -1;
        this.scaledFramebufferHeight = -1;
        this.scaledFramebufferScale = RenderScale.DEFAULT;
    }

    private boolean isUsingScaledFramebuffer() {
        return this.scaledFramebuffer != null && this.mainFramebuffer == this.scaledFramebuffer;
    }

    @Override
    public void begin(VkCommandBuffer commandBuffer, MemoryStack stack) {
        this.renderScaleResolvedThisFrame = false;
        this.guiResolveHandledThisFrame = false;
        this.scaledDepthClears = 0;
        this.liveDepthIsForeground = false;

        // roll current frame matrices into "previous" before the world render updates them
        if (postShaderActive()) {
            net.vulkanmod.vulkan.VRenderSystem.advanceTaaFrame();
            net.vulkanmod.render.PointLights.tick();
        }

        ensureMainFramebuffer();

        // shadow map at frame start, before the world render — doing it in the GUI resolve breaks the GUI on MoltenVK
        renderShadowMap(commandBuffer, stack);

        VulkanImage colorAttachment = this.mainFramebuffer.getColorAttachment();
        colorAttachment.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        this.mainFramebuffer.beginRenderPass(commandBuffer, this.mainRenderPass, stack);

        if (isUsingScaledFramebuffer()) {
            Renderer.setViewportScale(this.swapChain.getWidth(), this.swapChain.getHeight());
        } else {
            Renderer.clearViewportScale();
        }

        VkViewport.Buffer pViewport = this.mainFramebuffer.viewport(stack);
        vkCmdSetViewport(commandBuffer, 0, pViewport);

        VkRect2D.Buffer pScissor = this.mainFramebuffer.scissor(stack);
        vkCmdSetScissor(commandBuffer, 0, pScissor);
    }

    @Override
    public void end(VkCommandBuffer commandBuffer) {
        Renderer.getInstance().endRenderPass(commandBuffer);

        try(MemoryStack stack = MemoryStack.stackPush()) {
            SwapChain swapChain = Vulkan.getSwapChain();

            if (isUsingScaledFramebuffer()) {
                resolveScaledFramebufferToSwapchain(commandBuffer, false);
            }

            swapChain.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
        }

        int result = vkEndCommandBuffer(commandBuffer);
        if(result != VK_SUCCESS) {
            throw new RuntimeException("Failed to record command buffer:" + result);
        }
    }

    @Override
    public void resolveRenderScaleForGui() {
        if (!isUsingScaledFramebuffer() || this.renderScaleResolvedThisFrame) {
            return;
        }

        // resolve the world to the swapchain before the GUI, then the GUI draws into the present pass
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
        Renderer.getInstance().endRenderPass(commandBuffer);
        resolveScaledFramebufferToSwapchain(commandBuffer, true);
        this.renderScaleResolvedThisFrame = true;
        setMainFramebuffer(this.swapChain);
    }

    private void renderShadowMap(VkCommandBuffer commandBuffer, MemoryStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (!Initializer.CONFIG.shadowsEnabled || !postShaderActive() || mc.level == null) {
            return;
        }
        // shadow light = sun by day, moon (opposite) by night — whichever is above the horizon
        float a = mc.level.getTimeOfDay(1.0f) * ((float) Math.PI * 2.0f);
        float lx = -(float) Math.sin(a);
        float ly = (float) Math.cos(a);
        if (ly < 0.0f) { lx = -lx; ly = -ly; }

        if (ly <= 0.02f) {
            // below the horizon: no shadow, force a fresh render at next dawn
            this.shadowRenderedOnce = false;
            return;
        }

        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        double dx = camPos.x - this.lastShadowCamX;
        double dy = camPos.y - this.lastShadowCamY;
        double dz = camPos.z - this.lastShadowCamZ;
        double movedSq = dx * dx + dy * dy + dz * dz;

        // light drift vs the rotation that shifts the shadow by one texel: threshold angle = 2/res radians
        float dlx = lx - this.lastShadowLx;
        float dly = ly - this.lastShadowLy;
        float drift = (float) Math.sqrt(dlx * dlx + dly * dly);
        float driftThreshold = SHADOW_DRIFT_TOLERANCE * 2.0f / ShadowMap.currentResolution();

        int geometryVersion = net.vulkanmod.render.chunk.WorldRenderer.getGeometryVersion();
        int shadowQuality = Initializer.CONFIG.shadowQuality;
        int shadowDistance = Initializer.CONFIG.shadowDistance;

        // re-render only when the last capture was invalidated; otherwise reuse the depth texture as-is
        boolean due = !this.shadowRenderedOnce
                || drift > driftThreshold
                || movedSq > SHADOW_MOVE_THRESHOLD_SQ
                || geometryVersion != this.lastShadowGeometryVersion
                || shadowQuality != this.lastShadowQuality
                || shadowDistance != this.lastShadowDistance;
        if (!due) {
            return;
        }

        this.shadowMap.render(commandBuffer, stack, lx, ly, 0.0f);
        this.lastShadowCamX = camPos.x;
        this.lastShadowCamY = camPos.y;
        this.lastShadowCamZ = camPos.z;
        this.lastShadowLx = lx;
        this.lastShadowLy = ly;
        this.lastShadowGeometryVersion = geometryVersion;
        this.lastShadowQuality = shadowQuality;
        this.lastShadowDistance = shadowDistance;
        this.shadowRenderedOnce = true;
    }

    private void resolveScaledFramebufferToSwapchain(VkCommandBuffer commandBuffer, boolean keepRendering) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.mainFramebuffer.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

            if (postShaderActive()) {
                resolvePostShader(commandBuffer, stack, keepRendering);
                return;
            }

            // no post shader: upscale-blit into the present pass, left open for the GUI when requested
            this.swapChain.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            this.swapChain.beginRenderPass(commandBuffer, this.presentRenderPass, stack);
            Renderer.clearViewportScale();
            Renderer.resetViewport();
            Renderer.resetScissor();
            VTextureSelector.bindTexture(0, this.mainFramebuffer.getColorAttachment());
            DrawUtil.blitRenderScaleToScreen();
            if (!keepRendering) {
                Renderer.getInstance().endRenderPass(commandBuffer);
            }
        }
    }

    /** Post-shader resolve. The fog shader runs half-res terms + full-res composite (resolveFogTwoPass);
     *  everything else renders directly into the swapchain present pass. */
    private void resolvePostShader(VkCommandBuffer commandBuffer, MemoryStack stack, boolean keepRendering) {
        // depth shaders sample world depth + a foreground depth (with the hand); shader takes min() so the hand isn't fogged
        VulkanImage worldDepth = null;
        VulkanImage fgDepth = null;
        if (this.capturedWorldDepth != null) {
            worldDepth = this.capturedWorldDepth;
            if (this.liveDepthIsForeground) {
                // pre-GUI wipe suppressed: live depth still holds world+hand, sample it directly
                fgDepth = this.scaledFramebuffer.getDepthAttachment();
            } else {
                // clear #3 never fired (e.g. GUI hidden): fall back to the snapshot
                fgDepth = this.capturedForegroundDepth != null ? this.capturedForegroundDepth : this.capturedWorldDepth;
            }
        }
        boolean depthShader = worldDepth != null;
        if (depthShader) {
            worldDepth.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            fgDepth.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        }

        boolean fogShader = "fog".equals(Initializer.CONFIG.selectedShader);
        if (fogShader) {
            ensureTermsTargets(commandBuffer, stack);
        }
        boolean termsReady = this.termsFramebuffers[0] != null;

        if (fogShader && Initializer.CONFIG.halfResTerms && depthShader && termsReady) {
            resolveFogTwoPass(commandBuffer, stack, keepRendering, worldDepth, fgDepth);
            return;
        }

        // single-pass path: render straight into the present pass, left open for the GUI when requested
        this.swapChain.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        this.swapChain.beginRenderPass(commandBuffer, this.presentRenderPass, stack);
        Renderer.clearViewportScale();
        Renderer.resetViewport();
        Renderer.resetScissor();
        VTextureSelector.bindTexture(0, this.mainFramebuffer.getColorAttachment());
        if (depthShader) {
            VTextureSelector.bindTexture(1, worldDepth);
            VTextureSelector.bindTexture(2, fgDepth);
            // shadow map, or world depth when shadows are off so the sampler is always bound
            VulkanImage shadow = (Initializer.CONFIG.shadowsEnabled && this.shadowMap.isReady())
                    ? this.shadowMap.getDepthImage() : worldDepth;
            VTextureSelector.bindTexture(3, shadow);
            // fog-fallback TAA history = the previous terms target; non-fog shaders ignore it
            VTextureSelector.bindTexture(4, termsReady
                    ? this.termsFramebuffers[this.termsIndex ^ 1].getColorAttachment() : worldDepth);
        }
        DrawUtil.blitRenderScaleToScreen();
        if (!keepRendering) {
            Renderer.getInstance().endRenderPass(commandBuffer);
        }
    }

    /** Two-pass fog resolve: 1) the fog/shadow/god-ray amounts render at half res into the current terms
     *  target (sampling the other as TAA history), 2) the composite upsamples them and does the colour
     *  work at full res, straight into the swapchain present pass. */
    private void resolveFogTwoPass(VkCommandBuffer commandBuffer, MemoryStack stack, boolean keepRendering,
                                   VulkanImage worldDepth, VulkanImage fgDepth) {
        Framebuffer target = this.termsFramebuffers[this.termsIndex];
        Framebuffer history = this.termsFramebuffers[this.termsIndex ^ 1];
        // shadow map, or world depth when shadows are off so the sampler is always bound
        VulkanImage shadow = (Initializer.CONFIG.shadowsEnabled && this.shadowMap.isReady())
                ? this.shadowMap.getDepthImage() : worldDepth;

        // 1) half-res terms pass. clear the viewport scale first so the terms FBO's own viewport isn't rescaled
        target.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        Renderer.clearViewportScale();
        target.beginRenderPass(commandBuffer, this.termsRenderPasses[this.termsIndex], stack);
        VTextureSelector.bindTexture(0, this.mainFramebuffer.getColorAttachment());
        VTextureSelector.bindTexture(1, worldDepth);
        VTextureSelector.bindTexture(2, fgDepth);
        VTextureSelector.bindTexture(3, shadow);
        // TAA history = the other terms target (last frame's amounts); ignored when TAA is off
        VTextureSelector.bindTexture(4, history.getColorAttachment());
        DrawUtil.blit(net.vulkanmod.render.PipelineManager.getFogTermsPipeline());
        Renderer.getInstance().endRenderPass(commandBuffer);
        target.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

        // 2) full-res composite straight into the present pass, left open for the GUI when requested
        this.swapChain.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        this.swapChain.beginRenderPass(commandBuffer, this.presentRenderPass, stack);
        Renderer.clearViewportScale();
        Renderer.resetViewport();
        Renderer.resetScissor();
        VTextureSelector.bindTexture(0, this.mainFramebuffer.getColorAttachment());
        VTextureSelector.bindTexture(1, worldDepth);
        VTextureSelector.bindTexture(2, fgDepth);
        VTextureSelector.bindTexture(3, target.getColorAttachment());
        VTextureSelector.bindTexture(4, history.getColorAttachment());
        DrawUtil.blit(net.vulkanmod.render.PipelineManager.getFogCompositePipeline());
        if (!keepRendering) {
            Renderer.getInstance().endRenderPass(commandBuffer);
        }

        // ping-pong: this frame's terms become next frame's history
        this.termsIndex ^= 1;
    }

    /** Create (or resize) the two half-res ping-pong terms targets (RGBA16F), cleared to black once at
     *  creation so the first frame's history read is defined. Must be called outside a render pass. */
    private void ensureTermsTargets(VkCommandBuffer commandBuffer, MemoryStack stack) {
        // half the world render resolution; the 2:1 ratio must hold even under a render scale
        int w = Math.max(1, this.mainFramebuffer.getWidth() / 2);
        int h = Math.max(1, this.mainFramebuffer.getHeight() / 2);
        if (this.termsFramebuffers[0] != null && this.termsW == w && this.termsH == h) {
            return;
        }
        disposeTermsTargets();
        if (this.mainFramebuffer.getWidth() <= 0 || this.mainFramebuffer.getHeight() <= 0) {
            return;
        }

        for (int i = 0; i < 2; i++) {
            VulkanImage image = VulkanImage.builder(w, h)
                    .setFormat(VK_FORMAT_R16G16B16A16_SFLOAT)
                    .setUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT)
                    // nearest: the composite texelFetches the terms and the history read is fine with nearest
                    .setLinearFiltering(false)
                    .setClamp(true)
                    .createVulkanImage();
            this.termsFramebuffers[i] = Framebuffer.builder(image, null).build();

            // full-overwrite pass, no depth attachment; transitioned to SHADER_READ_ONLY after endRenderPass
            RenderPass.Builder builder = RenderPass.builder(this.termsFramebuffers[i]);
            builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);
            this.termsRenderPasses[i] = builder.build();

            image.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            VkClearColorValue clearColor = VkClearColorValue.calloc(stack);
            VkImageSubresourceRange.Buffer range = VkImageSubresourceRange.calloc(1, stack);
            range.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1);
            vkCmdClearColorImage(commandBuffer, image.getId(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, clearColor, range);
            image.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        }

        this.termsIndex = 0;
        this.termsW = w;
        this.termsH = h;
    }

    private void disposeTermsTargets() {
        for (int i = 0; i < 2; i++) {
            if (this.termsRenderPasses[i] != null) {
                this.termsRenderPasses[i].cleanUp();
                this.termsRenderPasses[i] = null;
            }
            if (this.termsFramebuffers[i] != null) {
                this.termsFramebuffers[i].cleanUp();
                this.termsFramebuffers[i] = null;
            }
        }
        this.termsW = -1;
        this.termsH = -1;
    }

    @Override
    public void onDepthClear(Framebuffer framebuffer) {
        // only the offscreen FBO clears matter, and only while a depth post shader is active
        if (!postShaderActive() || framebuffer != this.scaledFramebuffer || !isUsingScaledFramebuffer()) {
            return;
        }
        this.scaledDepthClears++;
        // #2 = pre-hand clear: snapshot world depth. #3 = pre-GUI clear: mark live depth as the foreground
        if (this.scaledDepthClears == 2) {
            net.vulkanmod.vulkan.VRenderSystem.captureWorldReconstruction();
            this.capturedWorldDepth = snapshotScaledDepth(this.capturedWorldDepth);
        } else if (this.scaledDepthClears == 3) {
            this.liveDepthIsForeground = true;
        }
    }

    @Override
    public boolean suppressDepthClear(Framebuffer framebuffer) {
        // #1 and #2 proceed; #3 (pre-GUI) and later are skipped so the resolve can sample world+hand live
        return postShaderActive()
                && isUsingScaledFramebuffer()
                && framebuffer == this.scaledFramebuffer
                && this.scaledDepthClears >= 3;
    }

    /** Copy the live offscreen depth into a persistent samplable image, bracketed by ending and
     *  re-opening the world render pass since a copy can't run inside one. */
    private VulkanImage snapshotScaledDepth(VulkanImage target) {
        VulkanImage src = this.scaledFramebuffer.getDepthAttachment();
        int w = src.width;
        int h = src.height;
        if (target == null || target.width != w || target.height != h) {
            if (target != null) target.free();
            target = VulkanImage.createDepthImage(
                    VK_FORMAT_D32_SFLOAT, w, h,
                    VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                    false, true);
        }

        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
        Renderer.getInstance().endRenderPass(commandBuffer);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            src.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
            target.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

            VkImageCopy.Buffer region = VkImageCopy.calloc(1, stack);
            region.srcSubresource().set(VK_IMAGE_ASPECT_DEPTH_BIT, 0, 0, 1);
            region.srcOffset().set(0, 0, 0);
            region.dstSubresource().set(VK_IMAGE_ASPECT_DEPTH_BIT, 0, 0, 1);
            region.dstOffset().set(0, 0, 0);
            region.extent().set(w, h, 1);

            vkCmdCopyImage(commandBuffer,
                    src.getId(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    target.getId(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    region);

            target.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            src.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            // re-open the world pass so the pending clear and later draws continue
            this.scaledFramebuffer.beginRenderPass(commandBuffer, this.auxRenderPass, stack);
            Renderer.setViewportScale(this.swapChain.getWidth(), this.swapChain.getHeight());
        }
        return target;
    }

    public void rebindMainTarget() {
        ensureMainFramebuffer();
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        RenderPass boundRenderPass = Renderer.getInstance().getBoundRenderPass();
        if(boundRenderPass == this.mainRenderPass || boundRenderPass == this.auxRenderPass)
            return;

        Renderer.getInstance().endRenderPass(commandBuffer);

        try(MemoryStack stack = MemoryStack.stackPush()) {
            this.mainFramebuffer.beginRenderPass(commandBuffer, this.auxRenderPass, stack);
            if (isUsingScaledFramebuffer()) {
                Renderer.setViewportScale(this.swapChain.getWidth(), this.swapChain.getHeight());
            } else {
                Renderer.clearViewportScale();
            }
        }

    }

    @Override
    public void bindAsTexture() {
        ensureMainFramebuffer();
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        RenderPass boundRenderPass = Renderer.getInstance().getBoundRenderPass();
        if(boundRenderPass == this.mainRenderPass || boundRenderPass == this.auxRenderPass)
            Renderer.getInstance().endRenderPass(commandBuffer);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.mainFramebuffer.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        }

        VTextureSelector.bindTexture(this.mainFramebuffer.getColorAttachment());
    }

    public int getColorAttachmentGlId() {
        ensureMainFramebuffer();

        if (isUsingScaledFramebuffer()) {
            return this.scaledColorAttachmentGlId;
        }

        return Vulkan.getSwapChain().getColorAttachmentGlId();
    }

    @Override
    public int getDepthAttachmentGlId() {
        ensureMainFramebuffer();

        if (isUsingScaledFramebuffer()) {
            return this.scaledDepthAttachmentGlId;
        }

        return Vulkan.getSwapChain().getDepthAttachmentGlId();
    }
}
