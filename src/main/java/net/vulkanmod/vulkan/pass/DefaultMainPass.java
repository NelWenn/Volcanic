package net.vulkanmod.vulkan.pass;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.RenderScale;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
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
    private VulkanImage coloredShadowDepth;
    private VulkanImage capturedOpaqueDepth;
    private VulkanImage capturedForegroundDepth;
    private int scaledDepthClears;
    private boolean liveDepthIsForeground;

    private final ShadowMap shadowMap = new ShadowMap();
    public static boolean inEntityShadowPass = false;

    private static final double SHADOW_MOVE_THRESHOLD_SQ = 0.35 * 0.35;
    private static final float SHADOW_DRIFT_TOLERANCE = 1.15f;
    private double lastShadowCamX, lastShadowCamY, lastShadowCamZ;
    private float lastShadowLx, lastShadowLy, lastShadowLz;
    private int shadowRefreshFrames;
    private int lastShadowGeometryVersion = -1;
    private int lastShadowQuality = -1, lastShadowDistance = -1;
    private boolean shadowRenderedOnce;

    private RenderPass mainRenderPass;
    private RenderPass auxRenderPass;
    private RenderPass presentRenderPass;

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
        if (this.coloredShadowDepth != null) {
            this.coloredShadowDepth.free();
            this.coloredShadowDepth = null;
        }
        if (this.capturedOpaqueDepth != null) {
            this.capturedOpaqueDepth.free();
            this.capturedOpaqueDepth = null;
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

        ensureMainFramebuffer();

        net.vulkanmod.render.framegraph.radiance.RadianceGraph.get().execute(
                net.vulkanmod.render.framegraph.Phase.FRAME_START, commandBuffer, stack, name -> null, () -> {});

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

    public void renderShadowMap(VkCommandBuffer commandBuffer, MemoryStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (!Initializer.CONFIG.shadowsEnabled || !postShaderActive() || mc.level == null) {
            return;
        }
        float a = net.vulkanmod.vulkan.VRenderSystem.smoothTimeOfDay(mc) * ((float) Math.PI * 2.0f);
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

        boolean changed = !this.shadowRenderedOnce
                || Initializer.CONFIG.windEnabled
                || Initializer.CONFIG.entityShadows
                || drift > driftThreshold
                || movedSq > SHADOW_MOVE_THRESHOLD_SQ
                || geometryVersion != this.lastShadowGeometryVersion
                || shadowQuality != this.lastShadowQuality
                || shadowDistance != this.lastShadowDistance;
        if (changed) {
            this.shadowRefreshFrames = net.vulkanmod.vulkan.Renderer.getFramesNum();
        }
        if (this.shadowRefreshFrames <= 0) {
            return;
        }
        this.shadowRefreshFrames--;

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

    @Override
    public void renderEntityShadows(Runnable casters, java.util.function.IntConsumer tintCascade) {
        if (!this.shadowMap.isReady() || (casters == null && tintCascade == null)) {
            return;
        }

        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        final boolean sDepthTest = VRenderSystem.depthTest, sDepthMask = VRenderSystem.depthMask, sCull = VRenderSystem.cull;
        final int sDepthFun = VRenderSystem.depthFun, sCullFace = VRenderSystem.cullFace, sFrontFace = VRenderSystem.frontFace;
        final int sTopology = VRenderSystem.topology, sPolygonMode = VRenderSystem.polygonMode, sColorMask = VRenderSystem.colorMask;
        final net.vulkanmod.vulkan.shader.PipelineState.BlendInfo bi = net.vulkanmod.vulkan.shader.PipelineState.blendInfo;
        final boolean sBlendEnabled = bi.enabled;
        final int sSrcRgb = bi.srcRgbFactor, sDstRgb = bi.dstRgbFactor, sSrcA = bi.srcAlphaFactor, sDstA = bi.dstAlphaFactor;
        final int sBlendOp = bi.blendOp, sBlendOpRgb = bi.blendOpRgb, sBlendOpAlpha = bi.blendOpAlpha;

        net.vulkanmod.vulkan.util.MappedBuffer mvBuf = VRenderSystem.modelViewMatrix;
        net.vulkanmod.vulkan.util.MappedBuffer pBuf = VRenderSystem.projectionMatrix;
        long mvBackupPtr = org.lwjgl.system.MemoryUtil.nmemAlloc(64);
        long pBackupPtr = org.lwjgl.system.MemoryUtil.nmemAlloc(64);
        org.lwjgl.system.MemoryUtil.memCopy(mvBuf.ptr, mvBackupPtr, 64L);
        org.lwjgl.system.MemoryUtil.memCopy(pBuf.ptr, pBackupPtr, 64L);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            Renderer.getInstance().endRenderPass(commandBuffer);

            if (casters != null) {
                this.shadowMap.beginEntityPass(commandBuffer, stack);

                VRenderSystem.colorMask = net.vulkanmod.vulkan.shader.PipelineState.ColorMask.getColorMask(true, true, true, true);
                VRenderSystem.depthTest = true;
                VRenderSystem.depthMask = true;
                VRenderSystem.depthFun = 515;

                inEntityShadowPass = true;
                try {
                    casters.run();
                } finally {
                    inEntityShadowPass = false;
                }

                this.shadowMap.endEntityPass(commandBuffer, stack);
            }

            if (tintCascade != null) {
                for (int c = 0; c < ShadowMap.CASCADES; c++) {
                    this.shadowMap.beginTintPass(c, commandBuffer, stack);
                    tintCascade.accept(c);
                    this.shadowMap.endTintPass(commandBuffer, stack);
                }
            }
        }

        VRenderSystem.depthTest = sDepthTest; VRenderSystem.depthMask = sDepthMask; VRenderSystem.depthFun = sDepthFun;
        VRenderSystem.cull = sCull; VRenderSystem.cullFace = sCullFace; VRenderSystem.frontFace = sFrontFace;
        VRenderSystem.topology = sTopology; VRenderSystem.polygonMode = sPolygonMode; VRenderSystem.colorMask = sColorMask;
        bi.enabled = sBlendEnabled; bi.srcRgbFactor = sSrcRgb; bi.dstRgbFactor = sDstRgb;
        bi.srcAlphaFactor = sSrcA; bi.dstAlphaFactor = sDstA;
        bi.blendOp = sBlendOp; bi.blendOpRgb = sBlendOpRgb; bi.blendOpAlpha = sBlendOpAlpha;

        rebindMainTarget();
        org.lwjgl.system.MemoryUtil.memCopy(mvBackupPtr, mvBuf.ptr, 64L);
        org.lwjgl.system.MemoryUtil.memCopy(pBackupPtr, pBuf.ptr, 64L);
        VRenderSystem.calculateMVP();
        org.lwjgl.system.MemoryUtil.nmemFree(mvBackupPtr);
        org.lwjgl.system.MemoryUtil.nmemFree(pBackupPtr);
    }

    @Override
    public void captureOpaqueDepth() {
        if (!postShaderActive() || !isUsingScaledFramebuffer()
                || !"radiance".equals(Initializer.CONFIG.selectedShader)) {
            return;
        }
        this.capturedOpaqueDepth = snapshotScaledDepth(this.capturedOpaqueDepth);
    }

    @Override
    public void applyColoredShadow() {
        if (!Initializer.CONFIG.coloredShadows || !Initializer.CONFIG.shadowsEnabled
                || !this.shadowMap.isReady() || !postShaderActive() || !isUsingScaledFramebuffer()) {
            return;
        }
        VulkanImage tint0 = this.shadowMap.getTintImage(0);
        VulkanImage tint1 = this.shadowMap.getTintImage(1);
        VulkanImage tint2 = this.shadowMap.getTintImage(2);
        if (tint0 == null || tint1 == null || tint2 == null) {
            return;
        }

        VulkanImage opaqueDepth = this.capturedOpaqueDepth != null
                ? this.capturedOpaqueDepth
                : (this.coloredShadowDepth = snapshotScaledDepth(this.coloredShadowDepth));

        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        final boolean sDepthTest = VRenderSystem.depthTest, sDepthMask = VRenderSystem.depthMask, sCull = VRenderSystem.cull;
        final int sColorMask = VRenderSystem.colorMask, sTopology = VRenderSystem.topology;
        final net.vulkanmod.vulkan.shader.PipelineState.BlendInfo bi = net.vulkanmod.vulkan.shader.PipelineState.blendInfo;
        final boolean sBlend = bi.enabled;
        final int sSrcRgb = bi.srcRgbFactor, sDstRgb = bi.dstRgbFactor, sSrcA = bi.srcAlphaFactor, sDstA = bi.dstAlphaFactor;
        final int sBlendOp = bi.blendOp, sBlendOpRgb = bi.blendOpRgb, sBlendOpAlpha = bi.blendOpAlpha;

        VRenderSystem.depthTest = false;
        VRenderSystem.depthMask = false;
        VRenderSystem.cull = false;
        VRenderSystem.colorMask = net.vulkanmod.vulkan.shader.PipelineState.ColorMask.getColorMask(true, true, true, true);
        VRenderSystem.setPrimitiveTopologyGL(org.lwjgl.opengl.GL11.GL_TRIANGLES);
        bi.enabled = true;
        bi.srcRgbFactor = VK_BLEND_FACTOR_DST_COLOR;
        bi.dstRgbFactor = VK_BLEND_FACTOR_ZERO;
        bi.srcAlphaFactor = VK_BLEND_FACTOR_ONE;
        bi.dstAlphaFactor = VK_BLEND_FACTOR_ZERO;
        bi.blendOp = bi.blendOpRgb = bi.blendOpAlpha = VK_BLEND_OP_ADD;

        net.vulkanmod.vulkan.texture.VTextureSelector.bindTexture(0, opaqueDepth);
        net.vulkanmod.vulkan.texture.VTextureSelector.bindTexture(1, tint0);
        net.vulkanmod.vulkan.texture.VTextureSelector.bindTexture(2, tint1);
        net.vulkanmod.vulkan.texture.VTextureSelector.bindTexture(3, tint2);

        net.vulkanmod.vulkan.shader.GraphicsPipeline pipeline =
                net.vulkanmod.vulkan.shader.pipeline.PipelineRegistry.getOrNull(
                        net.vulkanmod.render.framegraph.radiance.RadianceOpaqueTintPipeline.class);
        if (pipeline != null) {
            Renderer.getInstance().bindGraphicsPipeline(pipeline);
            Renderer.getInstance().uploadAndBindUBOs(pipeline);
            vkCmdDraw(commandBuffer, 3, 1, 0, 0);
        }

        VRenderSystem.depthTest = sDepthTest; VRenderSystem.depthMask = sDepthMask; VRenderSystem.cull = sCull;
        VRenderSystem.colorMask = sColorMask; VRenderSystem.topology = sTopology;
        bi.enabled = sBlend; bi.srcRgbFactor = sSrcRgb; bi.dstRgbFactor = sDstRgb;
        bi.srcAlphaFactor = sSrcA; bi.dstAlphaFactor = sDstA;
        bi.blendOp = sBlendOp; bi.blendOpRgb = sBlendOpRgb; bi.blendOpAlpha = sBlendOpAlpha;
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

        if (depthShader && "radiance".equals(Initializer.CONFIG.selectedShader)
                && resolveRadianceGraph(commandBuffer, stack, keepRendering, worldDepth, fgDepth)) {
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

    private boolean resolveRadianceGraph(VkCommandBuffer commandBuffer, MemoryStack stack, boolean keepRendering,
                                         VulkanImage worldDepth, VulkanImage fgDepth) {
        net.vulkanmod.render.framegraph.FrameGraph graph = net.vulkanmod.render.framegraph.radiance.RadianceGraph.get();
        if (!graph.pipelinesReady()) {
            return false;
        }
        graph.setTargetScale("light", Initializer.CONFIG.optimizedShadows ? 0.5f : 1.0f);
        graph.resize(commandBuffer, stack, this.mainFramebuffer.getWidth(), this.mainFramebuffer.getHeight());
        if (!graph.targetsReady()) {
            return false;
        }

        final VulkanImage scene = this.mainFramebuffer.getColorAttachment();
        boolean shadowsOn = Initializer.CONFIG.shadowsEnabled && this.shadowMap.isReady();
        final VulkanImage sh0 = shadowsOn ? this.shadowMap.getCascadeDepthImage(0) : worldDepth;
        final VulkanImage sh1 = shadowsOn ? this.shadowMap.getCascadeDepthImage(1) : worldDepth;
        final VulkanImage sh2 = shadowsOn ? this.shadowMap.getCascadeDepthImage(2) : worldDepth;
        boolean ran = graph.execute(net.vulkanmod.render.framegraph.Phase.POST_PROCESS, commandBuffer, stack, name -> switch (name) {
            case "scene" -> scene;
            case "depthtex" -> worldDepth;
            case "fgdepth" -> fgDepth;
            case "shadowtex0" -> sh0;
            case "shadowtex1" -> sh1;
            case "shadowtex2" -> sh2;
            case "opaquedepth" -> this.capturedOpaqueDepth != null ? this.capturedOpaqueDepth : worldDepth;
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
