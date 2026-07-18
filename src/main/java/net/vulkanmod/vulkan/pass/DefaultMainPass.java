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

    private VulkanImage capturedWorldDepth;
    private VulkanImage capturedForegroundDepth;
    private int scaledDepthClears;
    private boolean liveDepthIsForeground;

    private final ShadowMap shadowMap = new ShadowMap();

    // draws during this window are forced depth-only (blend off, colour masked) so translucent shards can't
    // re-enable blend against the DONT_CARE colour attachment (silently aborts the whole pass on MoltenVK)
    public static boolean inEntityShadowPass = false;

    private static final double SHADOW_MOVE_THRESHOLD_SQ = 0.35 * 0.35;
    private static final float SHADOW_DRIFT_TOLERANCE = 1.15f;
    private double lastShadowCamX, lastShadowCamY, lastShadowCamZ;
    private float lastShadowLx, lastShadowLy, lastShadowLz;
    private int windShadowCounter;
    private int lastShadowGeometryVersion = -1;
    private int lastShadowQuality = -1, lastShadowDistance = -1;
    private boolean shadowRenderedOnce;

    private RenderPass mainRenderPass;
    private RenderPass auxRenderPass;
    private RenderPass presentRenderPass;

    private final Framebuffer[] termsFramebuffers = new Framebuffer[2];
    private final RenderPass[] termsRenderPasses = new RenderPass[2];
    private int termsIndex;
    private int termsW = -1, termsH = -1;

    private final Framebuffer[] exposureFramebuffers = new Framebuffer[2];
    private final RenderPass[] exposureRenderPasses = new RenderPass[2];
    private int exposureIndex;

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

        if (postShaderActive())
            return base;

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

        if (postShaderActive()) {
            net.vulkanmod.vulkan.VRenderSystem.advanceTaaFrame();
            net.vulkanmod.render.PointLights.tick();
        }

        ensureMainFramebuffer();

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
        float a = mc.level.getTimeOfDay(1.0f) * ((float) Math.PI * 2.0f);
        float lx = -(float) Math.sin(a);
        float lh = (float) Math.cos(a);
        float ly = lh * net.vulkanmod.vulkan.VRenderSystem.SUN_TILT_COS;
        float lz = lh * net.vulkanmod.vulkan.VRenderSystem.SUN_TILT_SIN;
        if (ly < 0.0f) { lx = -lx; ly = -ly; lz = -lz; }

        if (ly <= 0.02f) {
            this.shadowRenderedOnce = false;
            return;
        }

        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        double dx = camPos.x - this.lastShadowCamX;
        double dy = camPos.y - this.lastShadowCamY;
        double dz = camPos.z - this.lastShadowCamZ;
        double movedSq = dx * dx + dy * dy + dz * dz;

        float dlx = lx - this.lastShadowLx;
        float dly = ly - this.lastShadowLy;
        float dlz = lz - this.lastShadowLz;
        float drift = (float) Math.sqrt(dlx * dlx + dly * dly + dlz * dlz);
        float driftThreshold = SHADOW_DRIFT_TOLERANCE * 2.0f / ShadowMap.currentResolution();

        int geometryVersion = net.vulkanmod.render.chunk.WorldRenderer.getGeometryVersion();
        int shadowQuality = Initializer.CONFIG.shadowQuality;
        int shadowDistance = Initializer.CONFIG.shadowDistance;

        boolean windDue = Initializer.CONFIG.windEnabled && ++this.windShadowCounter >= 2;
        if (windDue) this.windShadowCounter = 0;

        boolean due = !this.shadowRenderedOnce
                || Initializer.CONFIG.entityShadows   // fresh terrain depth every frame so the entity depth-LOAD is valid
                || ("radiance".equals(Initializer.CONFIG.selectedShader) && this.shadowMap.rsmPending())
                || windDue
                || drift > driftThreshold
                || movedSq > SHADOW_MOVE_THRESHOLD_SQ
                || geometryVersion != this.lastShadowGeometryVersion
                || shadowQuality != this.lastShadowQuality
                || shadowDistance != this.lastShadowDistance;
        if (!due) {
            return;
        }

        this.shadowMap.render(commandBuffer, stack, lx, ly, lz);
        this.lastShadowCamX = camPos.x;
        this.lastShadowCamY = camPos.y;
        this.lastShadowCamZ = camPos.z;
        this.lastShadowLx = lx;
        this.lastShadowLy = ly;
        this.lastShadowLz = lz;
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

    private void resolvePostShader(VkCommandBuffer commandBuffer, MemoryStack stack, boolean keepRendering) {
        VulkanImage worldDepth = null;
        VulkanImage fgDepth = null;
        if (this.capturedWorldDepth != null) {
            worldDepth = this.capturedWorldDepth;
            if (this.liveDepthIsForeground) {
                fgDepth = this.scaledFramebuffer.getDepthAttachment();
            } else {
                fgDepth = this.capturedForegroundDepth != null ? this.capturedForegroundDepth : this.capturedWorldDepth;
            }
        }
        boolean depthShader = worldDepth != null;
        if (depthShader) {
            worldDepth.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            fgDepth.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        }

        net.vulkanmod.render.pack.ShaderPacks.ensureBuiltins();
        boolean fogShader = Initializer.CONFIG.isCamille();

        if (!"radiance".equals(Initializer.CONFIG.selectedShader)) {
            net.vulkanmod.render.pack.PackPipeline.releaseTargets();
        }

        if (fogShader && depthShader
                && "radiance".equals(Initializer.CONFIG.selectedShader)
                && resolveRadiancePack(commandBuffer, stack, keepRendering, worldDepth, fgDepth)) {
            return;
        }

        if (fogShader) {
            ensureTermsTargets(commandBuffer, stack);
            ensureExposureTargets(commandBuffer, stack);
        }
        boolean termsReady = this.termsFramebuffers[0] != null;

        if (fogShader && Initializer.CONFIG.halfResTerms && depthShader && termsReady) {
            resolveFogTwoPass(commandBuffer, stack, keepRendering, worldDepth, fgDepth);
            return;
        }

        this.swapChain.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        this.swapChain.beginRenderPass(commandBuffer, this.presentRenderPass, stack);
        Renderer.clearViewportScale();
        Renderer.resetViewport();
        Renderer.resetScissor();
        VTextureSelector.bindTexture(0, this.mainFramebuffer.getColorAttachment());
        if (depthShader) {
            VTextureSelector.bindTexture(1, worldDepth);
            VTextureSelector.bindTexture(2, fgDepth);
            VulkanImage shadow = (Initializer.CONFIG.shadowsEnabled && this.shadowMap.isReady())
                    ? this.shadowMap.getDepthImage() : worldDepth;
            VTextureSelector.bindTexture(3, shadow);
            VTextureSelector.bindTexture(4, termsReady
                    ? this.termsFramebuffers[this.termsIndex ^ 1].getColorAttachment() : worldDepth);
        }
        net.vulkanmod.render.pack.ShaderPack pack = net.vulkanmod.render.pack.PackPipeline.get(Initializer.CONFIG.selectedShader);
        final VulkanImage sceneImg = this.mainFramebuffer.getColorAttachment();
        final VulkanImage wd = worldDepth, fd = fgDepth;
        boolean ranPack = pack != null && !pack.passes.isEmpty() && pack.targets.isEmpty()
                && net.vulkanmod.render.pack.PackPipeline.run(pack, name -> switch (name) {
                    case "scene" -> sceneImg;
                    case "depthtex" -> wd;
                    case "fgdepth" -> fd;
                    default -> null;
                });
        if (!ranPack) {
            DrawUtil.blitRenderScaleToScreen();
        }
        if (!keepRendering) {
            Renderer.getInstance().endRenderPass(commandBuffer);
        }
    }

    private void resolveFogTwoPass(VkCommandBuffer commandBuffer, MemoryStack stack, boolean keepRendering,
                                   VulkanImage worldDepth, VulkanImage fgDepth) {
        Framebuffer target = this.termsFramebuffers[this.termsIndex];
        Framebuffer history = this.termsFramebuffers[this.termsIndex ^ 1];
        VulkanImage shadow = (Initializer.CONFIG.shadowsEnabled && this.shadowMap.isReady())
                ? this.shadowMap.getDepthImage() : worldDepth;

        target.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        Renderer.clearViewportScale();
        target.beginRenderPass(commandBuffer, this.termsRenderPasses[this.termsIndex], stack);
        VTextureSelector.bindTexture(0, this.mainFramebuffer.getColorAttachment());
        VTextureSelector.bindTexture(1, worldDepth);
        VTextureSelector.bindTexture(2, fgDepth);
        VTextureSelector.bindTexture(3, shadow);
        VTextureSelector.bindTexture(4, history.getColorAttachment());
        DrawUtil.blit(net.vulkanmod.render.PipelineManager.getFogTermsPipeline());
        Renderer.getInstance().endRenderPass(commandBuffer);
        target.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

        Framebuffer expTarget = this.exposureFramebuffers[this.exposureIndex];
        Framebuffer expHistory = this.exposureFramebuffers[this.exposureIndex ^ 1];
        expTarget.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        Renderer.clearViewportScale();
        expTarget.beginRenderPass(commandBuffer, this.exposureRenderPasses[this.exposureIndex], stack);
        VTextureSelector.bindTexture(0, this.mainFramebuffer.getColorAttachment());
        VTextureSelector.bindTexture(1, worldDepth);
        VTextureSelector.bindTexture(2, fgDepth);
        VTextureSelector.bindTexture(3, shadow);
        VTextureSelector.bindTexture(4, expHistory.getColorAttachment());
        DrawUtil.blit(net.vulkanmod.render.PipelineManager.getFogExposurePipeline());
        Renderer.getInstance().endRenderPass(commandBuffer);
        expTarget.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

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
        VTextureSelector.bindTexture(5, expTarget.getColorAttachment());
        VulkanImage tintImg = (Initializer.CONFIG.coloredShadows && this.shadowMap.isReady() && this.shadowMap.getColorImage() != null)
                ? this.shadowMap.getColorImage() : this.mainFramebuffer.getColorAttachment();
        VTextureSelector.bindTexture(6, tintImg);
        DrawUtil.blit(net.vulkanmod.render.PipelineManager.getFogCompositePipeline());
        if (!keepRendering) {
            Renderer.getInstance().endRenderPass(commandBuffer);
        }

        this.termsIndex ^= 1;
        this.exposureIndex ^= 1;
    }

    private boolean resolveRadiancePack(VkCommandBuffer commandBuffer, MemoryStack stack, boolean keepRendering,
                                        VulkanImage worldDepth, VulkanImage fgDepth) {
        net.vulkanmod.render.pack.ShaderPack pack = net.vulkanmod.render.pack.PackPipeline.get(Initializer.CONFIG.selectedShader);
        if (pack == null || pack.targets.isEmpty() || !net.vulkanmod.render.pack.PackPipeline.structureValid(pack)) {
            return false;
        }
        if (!net.vulkanmod.render.pack.PackPipeline.pipelinesReady(pack)) {
            return false;
        }
        net.vulkanmod.render.pack.PackPipeline.ensureTargets(pack, commandBuffer, stack,
                this.mainFramebuffer.getWidth(), this.mainFramebuffer.getHeight());
        if (!net.vulkanmod.render.pack.PackPipeline.targetsReady(pack)) {
            return false;
        }

        final VulkanImage scene = this.mainFramebuffer.getColorAttachment();
        final VulkanImage shadow = (Initializer.CONFIG.shadowsEnabled && this.shadowMap.isReady())
                ? this.shadowMap.getDepthImage() : worldDepth;
        final VulkanImage tint = (Initializer.CONFIG.coloredShadows && this.shadowMap.isReady() && this.shadowMap.getColorImage() != null)
                ? this.shadowMap.getColorImage() : scene;
        final VulkanImage rsmFlux = this.shadowMap.getRsmColorImage() != null ? this.shadowMap.getRsmColorImage() : scene;
        final VulkanImage rsmDepth = this.shadowMap.getRsmDepthImage() != null ? this.shadowMap.getRsmDepthImage() : worldDepth;
        boolean ran = net.vulkanmod.render.pack.PackPipeline.runFrame(pack, commandBuffer, stack, name -> switch (name) {
            case "scene" -> scene;
            case "depthtex" -> worldDepth;
            case "fgdepth" -> fgDepth;
            case "shadowtex" -> shadow;
            case "shadowtint" -> tint;
            case "rsmflux" -> rsmFlux;
            case "rsmdepth" -> rsmDepth;
            default -> null;
        }, () -> {
            this.swapChain.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            this.swapChain.beginRenderPass(commandBuffer, this.presentRenderPass, stack);
            Renderer.clearViewportScale();
            Renderer.resetViewport();
            Renderer.resetScissor();
        });
        if (!ran) {
            return false;
        }
        if (!keepRendering) {
            Renderer.getInstance().endRenderPass(commandBuffer);
        }
        return true;
    }

    private void ensureTermsTargets(VkCommandBuffer commandBuffer, MemoryStack stack) {
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
                    .setLinearFiltering(false)
                    .setClamp(true)
                    .createVulkanImage();
            this.termsFramebuffers[i] = Framebuffer.builder(image, null).build();

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

    private void ensureExposureTargets(VkCommandBuffer commandBuffer, MemoryStack stack) {
        if (this.exposureFramebuffers[0] != null) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            VulkanImage image = VulkanImage.builder(1, 1)
                    .setFormat(VK_FORMAT_R16_SFLOAT)
                    .setUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT)
                    .setLinearFiltering(false)
                    .setClamp(true)
                    .createVulkanImage();
            this.exposureFramebuffers[i] = Framebuffer.builder(image, null).build();

            RenderPass.Builder builder = RenderPass.builder(this.exposureFramebuffers[i]);
            builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);
            this.exposureRenderPasses[i] = builder.build();

            image.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            VkClearColorValue clearColor = VkClearColorValue.calloc(stack);
            clearColor.float32(0, 1.0f);
            VkImageSubresourceRange.Buffer range = VkImageSubresourceRange.calloc(1, stack);
            range.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1);
            vkCmdClearColorImage(commandBuffer, image.getId(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, clearColor, range);
            image.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        }
        this.exposureIndex = 0;
    }

    private void disposeExposureTargets() {
        for (int i = 0; i < 2; i++) {
            if (this.exposureRenderPasses[i] != null) {
                this.exposureRenderPasses[i].cleanUp();
                this.exposureRenderPasses[i] = null;
            }
            if (this.exposureFramebuffers[i] != null) {
                this.exposureFramebuffers[i].cleanUp();
                this.exposureFramebuffers[i] = null;
            }
        }
    }

    @Override
    public void onDepthClear(Framebuffer framebuffer) {
        if (!postShaderActive() || framebuffer != this.scaledFramebuffer || !isUsingScaledFramebuffer()) {
            return;
        }
        this.scaledDepthClears++;
        if (this.scaledDepthClears == 2) {
            net.vulkanmod.vulkan.VRenderSystem.captureWorldReconstruction();
            this.capturedWorldDepth = snapshotScaledDepth(this.capturedWorldDepth);
        } else if (this.scaledDepthClears == 3) {
            this.liveDepthIsForeground = true;
        }
    }

    @Override
    public boolean suppressDepthClear(Framebuffer framebuffer) {
        return postShaderActive()
                && isUsingScaledFramebuffer()
                && framebuffer == this.scaledFramebuffer
                && this.scaledDepthClears >= 3;
    }

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

        if (boundRenderPass != null)
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
    public void renderEntityShadows(Runnable casters, Runnable tint) {
        if (!this.shadowMap.isReady() || (casters == null && tint == null)) {
            return;
        }

        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        // snapshot the full pipeline-selecting state (mirror WorldRenderer.renderShadowTerrain)
        final boolean sDepthTest = net.vulkanmod.vulkan.VRenderSystem.depthTest, sDepthMask = net.vulkanmod.vulkan.VRenderSystem.depthMask, sCull = net.vulkanmod.vulkan.VRenderSystem.cull;
        final int sDepthFun = net.vulkanmod.vulkan.VRenderSystem.depthFun, sCullFace = net.vulkanmod.vulkan.VRenderSystem.cullFace, sFrontFace = net.vulkanmod.vulkan.VRenderSystem.frontFace;
        final int sTopology = net.vulkanmod.vulkan.VRenderSystem.topology, sPolygonMode = net.vulkanmod.vulkan.VRenderSystem.polygonMode, sColorMask = net.vulkanmod.vulkan.VRenderSystem.colorMask;
        final boolean sStencilTest = net.vulkanmod.vulkan.VRenderSystem.stencilTest, sLogicOp = net.vulkanmod.vulkan.VRenderSystem.logicOp;
        final int sStencilFunc = net.vulkanmod.vulkan.VRenderSystem.stencilFunc, sStencilRef = net.vulkanmod.vulkan.VRenderSystem.stencilRef, sStencilFuncMask = net.vulkanmod.vulkan.VRenderSystem.stencilFuncMask;
        final int sStencilFail = net.vulkanmod.vulkan.VRenderSystem.stencilFailOp, sStencilDepthFail = net.vulkanmod.vulkan.VRenderSystem.stencilDepthFailOp;
        final int sStencilPass = net.vulkanmod.vulkan.VRenderSystem.stencilPassOp, sStencilWriteMask = net.vulkanmod.vulkan.VRenderSystem.stencilWriteMask, sLogicOpFun = net.vulkanmod.vulkan.VRenderSystem.logicOpFun;
        final net.vulkanmod.vulkan.shader.PipelineState.BlendInfo bi = net.vulkanmod.vulkan.shader.PipelineState.blendInfo;
        final boolean sBlendEnabled = bi.enabled;
        final int sSrcRgb = bi.srcRgbFactor, sDstRgb = bi.dstRgbFactor, sSrcA = bi.srcAlphaFactor, sDstA = bi.dstAlphaFactor;
        final int sBlendOp = bi.blendOp, sBlendOpRgb = bi.blendOpRgb, sBlendOpAlpha = bi.blendOpAlpha;

        // snapshot the active MVP (main camera) so the color pass resumes with it
        net.vulkanmod.vulkan.util.MappedBuffer mvBuf = net.vulkanmod.vulkan.VRenderSystem.modelViewMatrix;
        net.vulkanmod.vulkan.util.MappedBuffer pBuf = net.vulkanmod.vulkan.VRenderSystem.projectionMatrix;
        long mvBackupPtr = org.lwjgl.system.MemoryUtil.nmemAlloc(64);
        long pBackupPtr = org.lwjgl.system.MemoryUtil.nmemAlloc(64);
        org.lwjgl.system.MemoryUtil.memCopy(mvBuf.ptr, mvBackupPtr, 64L);
        org.lwjgl.system.MemoryUtil.memCopy(pBuf.ptr, pBackupPtr, 64L);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // leave the main color pass, enter the shadow depth-LOAD pass
            Renderer.getInstance().endRenderPass(commandBuffer);

            if (casters != null) {
                this.shadowMap.beginEntityPass(commandBuffer, stack);

                // write colour to the throwaway attachment (masking all channels leaves the encoder with no observable output, which MoltenVK silently drops)
                net.vulkanmod.vulkan.VRenderSystem.colorMask = net.vulkanmod.vulkan.shader.PipelineState.ColorMask.getColorMask(true, true, true, true);
                net.vulkanmod.vulkan.VRenderSystem.depthTest = true;
                net.vulkanmod.vulkan.VRenderSystem.depthMask = true;
                net.vulkanmod.vulkan.VRenderSystem.depthFun = 515;   // GL_LEQUAL

                inEntityShadowPass = true;
                try {
                    casters.run();
                } finally {
                    inEntityShadowPass = false;
                }

                this.shadowMap.endEntityPass(commandBuffer, stack);
            }

            if (tint != null) {
                this.shadowMap.beginTintPass(commandBuffer, stack);
                tint.run();
                this.shadowMap.endTintPass(commandBuffer, stack);
            }
        }

        // restore VRenderSystem state
        net.vulkanmod.vulkan.VRenderSystem.depthTest = sDepthTest; net.vulkanmod.vulkan.VRenderSystem.depthMask = sDepthMask; net.vulkanmod.vulkan.VRenderSystem.depthFun = sDepthFun;
        net.vulkanmod.vulkan.VRenderSystem.cull = sCull; net.vulkanmod.vulkan.VRenderSystem.cullFace = sCullFace; net.vulkanmod.vulkan.VRenderSystem.frontFace = sFrontFace;
        net.vulkanmod.vulkan.VRenderSystem.topology = sTopology; net.vulkanmod.vulkan.VRenderSystem.polygonMode = sPolygonMode; net.vulkanmod.vulkan.VRenderSystem.colorMask = sColorMask;
        net.vulkanmod.vulkan.VRenderSystem.stencilTest = sStencilTest; net.vulkanmod.vulkan.VRenderSystem.stencilFunc = sStencilFunc; net.vulkanmod.vulkan.VRenderSystem.stencilRef = sStencilRef;
        net.vulkanmod.vulkan.VRenderSystem.stencilFuncMask = sStencilFuncMask; net.vulkanmod.vulkan.VRenderSystem.stencilFailOp = sStencilFail;
        net.vulkanmod.vulkan.VRenderSystem.stencilDepthFailOp = sStencilDepthFail; net.vulkanmod.vulkan.VRenderSystem.stencilPassOp = sStencilPass;
        net.vulkanmod.vulkan.VRenderSystem.stencilWriteMask = sStencilWriteMask; net.vulkanmod.vulkan.VRenderSystem.logicOp = sLogicOp; net.vulkanmod.vulkan.VRenderSystem.logicOpFun = sLogicOpFun;
        bi.enabled = sBlendEnabled; bi.srcRgbFactor = sSrcRgb; bi.dstRgbFactor = sDstRgb;
        bi.srcAlphaFactor = sSrcA; bi.dstAlphaFactor = sDstA;
        bi.blendOp = sBlendOp; bi.blendOpRgb = sBlendOpRgb; bi.blendOpAlpha = sBlendOpAlpha;

        // resume the main color pass, then re-apply the camera MVP that was active
        rebindMainTarget();
        org.lwjgl.system.MemoryUtil.memCopy(mvBackupPtr, mvBuf.ptr, 64L);
        org.lwjgl.system.MemoryUtil.memCopy(pBackupPtr, pBuf.ptr, 64L);
        net.vulkanmod.vulkan.VRenderSystem.calculateMVP();
        org.lwjgl.system.MemoryUtil.nmemFree(mvBackupPtr);
        org.lwjgl.system.MemoryUtil.nmemFree(pBackupPtr);
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
