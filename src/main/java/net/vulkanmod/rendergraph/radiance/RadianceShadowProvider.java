package net.vulkanmod.rendergraph.radiance;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.rendergraph.radiance.pipeline.RadianceOpaqueTintPipeline;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.pass.EngineContext;
import net.vulkanmod.vulkan.pass.ShadowProvider;
import net.vulkanmod.vulkan.pass.ShadowMap;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.PipelineState;
import net.vulkanmod.vulkan.shader.pipeline.PipelineRegistry;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.function.IntConsumer;

import static org.lwjgl.vulkan.VK10.*;

public final class RadianceShadowProvider implements ShadowProvider {
    public static boolean inEntityShadowPass = false;

    private EngineContext context;
    private final ShadowMap shadowMap = new ShadowMap();
    private RadianceDepthCaptureProvider depthCapture;

    private static final double SHADOW_MOVE_THRESHOLD_SQ = 0.35 * 0.35;
    private static final float SHADOW_DRIFT_TOLERANCE = 1.15f;
    private double lastShadowCamX, lastShadowCamY, lastShadowCamZ;
    private float lastShadowLx, lastShadowLy, lastShadowLz;
    private int shadowRefreshFrames;
    private int lastShadowGeometryVersion = -1;
    private int lastShadowQuality = -1, lastShadowDistance = -1;
    private boolean shadowRenderedOnce;

    public void setDepthCapture(RadianceDepthCaptureProvider depthCapture) {
        this.depthCapture = depthCapture;
    }

    @Override
    public void initialize(EngineContext context) {
        this.context = context;
    }

    @Override
    public void renderShadowMap(VkCommandBuffer commandBuffer, MemoryStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (!Initializer.CONFIG.shadowsEnabled || !context.postShaderActive() || mc.level == null) {
            return;
        }

        float a = VRenderSystem.smoothTimeOfDay(mc) * ((float) Math.PI * 2.0f);
        float lx = -(float) Math.sin(a);
        float lh = (float) Math.cos(a);
        float ly = lh * VRenderSystem.SUN_TILT_COS;
        float lz = lh * VRenderSystem.SUN_TILT_SIN;

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

        if (changed)
            this.shadowRefreshFrames = Renderer.getFramesNum();

        if (this.shadowRefreshFrames <= 0)
            return;

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
    public void renderEntityShadows(Runnable casters, IntConsumer tintCascade) {
        if (!this.shadowMap.isReady() || (casters == null && tintCascade == null))
            return;

        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        final boolean sDepthTest = VRenderSystem.depthTest, sDepthMask = VRenderSystem.depthMask, sCull = VRenderSystem.cull;
        final int sDepthFun = VRenderSystem.depthFun, sCullFace = VRenderSystem.cullFace, sFrontFace = VRenderSystem.frontFace;
        final int sTopology = VRenderSystem.topology, sPolygonMode = VRenderSystem.polygonMode, sColorMask = VRenderSystem.colorMask;

        final PipelineState.BlendInfo bi = PipelineState.blendInfo;

        final boolean sBlendEnabled = bi.enabled;
        final int sSrcRgb = bi.srcRgbFactor, sDstRgb = bi.dstRgbFactor, sSrcA = bi.srcAlphaFactor, sDstA = bi.dstAlphaFactor;
        final int sBlendOp = bi.blendOp, sBlendOpRgb = bi.blendOpRgb, sBlendOpAlpha = bi.blendOpAlpha;

        MappedBuffer mvBuf = VRenderSystem.modelViewMatrix;
        MappedBuffer pBuf = VRenderSystem.projectionMatrix;

        long mvBackupPtr = MemoryUtil.nmemAlloc(64);
        long pBackupPtr = MemoryUtil.nmemAlloc(64);

        MemoryUtil.memCopy(mvBuf.ptr, mvBackupPtr, 64L);
        MemoryUtil.memCopy(pBuf.ptr, pBackupPtr, 64L);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            Renderer.getInstance().endRenderPass(commandBuffer);

            if (casters != null) {
                this.shadowMap.beginEntityPass(commandBuffer, stack);

                VRenderSystem.colorMask = PipelineState.ColorMask.getColorMask(true, true, true, true);
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

        MemoryUtil.memCopy(mvBackupPtr, mvBuf.ptr, 64L);
        MemoryUtil.memCopy(pBackupPtr, pBuf.ptr, 64L);
        VRenderSystem.calculateMVP();
        MemoryUtil.nmemFree(mvBackupPtr);
        MemoryUtil.nmemFree(pBackupPtr);
    }

    @Override
    public void applyColoredShadow() {
        if (!Initializer.CONFIG.coloredShadows || !Initializer.CONFIG.shadowsEnabled
                || !this.shadowMap.isReady() || !context.postShaderActive() || !context.isScaledFramebuffer())
            return;

        VulkanImage tint0 = this.shadowMap.getTintImage(0);
        VulkanImage tint1 = this.shadowMap.getTintImage(1);
        VulkanImage tint2 = this.shadowMap.getTintImage(2);

        if (tint0 == null || tint1 == null || tint2 == null)
            return;

        VulkanImage opaqueDepth = depthCapture != null ? depthCapture.ensureColoredShadowDepth() : null;
        if (opaqueDepth == null) return;

        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        final boolean sDepthTest = VRenderSystem.depthTest, sDepthMask = VRenderSystem.depthMask, sCull = VRenderSystem.cull;
        final int sColorMask = VRenderSystem.colorMask, sTopology = VRenderSystem.topology;
        final PipelineState.BlendInfo bi = PipelineState.blendInfo;
        final boolean sBlend = bi.enabled;
        final int sSrcRgb = bi.srcRgbFactor, sDstRgb = bi.dstRgbFactor, sSrcA = bi.srcAlphaFactor, sDstA = bi.dstAlphaFactor;
        final int sBlendOp = bi.blendOp, sBlendOpRgb = bi.blendOpRgb, sBlendOpAlpha = bi.blendOpAlpha;

        VRenderSystem.depthTest = false;
        VRenderSystem.depthMask = false;
        VRenderSystem.cull = false;
        VRenderSystem.colorMask = PipelineState.ColorMask.getColorMask(true, true, true, true);
        VRenderSystem.setPrimitiveTopologyGL(org.lwjgl.opengl.GL11.GL_TRIANGLES);

        bi.enabled = true;
        bi.srcRgbFactor = VK_BLEND_FACTOR_DST_COLOR;
        bi.dstRgbFactor = VK_BLEND_FACTOR_ZERO;
        bi.srcAlphaFactor = VK_BLEND_FACTOR_ONE;
        bi.dstAlphaFactor = VK_BLEND_FACTOR_ZERO;
        bi.blendOp = bi.blendOpRgb = bi.blendOpAlpha = VK_BLEND_OP_ADD;

        VTextureSelector.bindTexture(0, opaqueDepth);
        VTextureSelector.bindTexture(1, tint0);
        VTextureSelector.bindTexture(2, tint1);
        VTextureSelector.bindTexture(3, tint2);
        VTextureSelector.bindTexture(4, this.shadowMap.getCascadeDepthImage(0));
        VTextureSelector.bindTexture(5, this.shadowMap.getCascadeDepthImage(1));
        VTextureSelector.bindTexture(6, this.shadowMap.getCascadeDepthImage(2));

        GraphicsPipeline pipeline = PipelineRegistry.getOrNull(RadianceOpaqueTintPipeline.class);
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

    @Override
    public boolean isReady() {
        return this.shadowMap.isReady();
    }

    @Override
    public VulkanImage getCascadeDepthImage(int cascade) {
        return this.shadowMap.getCascadeDepthImage(cascade);
    }

    @Override
    public VulkanImage getTintImage(int cascade) {
        return this.shadowMap.getTintImage(cascade);
    }

    @Override
    public void cleanup() {
        // ShadowMap resources are handled internally
    }
}
