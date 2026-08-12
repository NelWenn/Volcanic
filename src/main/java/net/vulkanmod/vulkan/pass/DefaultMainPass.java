package net.vulkanmod.vulkan.pass;

import net.minecraft.client.Minecraft;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.RenderScale;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.render.framegraph.FrameGraph;
import net.vulkanmod.render.framegraph.FrameGraphImpl;
import net.vulkanmod.render.framegraph.Phase;
import net.vulkanmod.render.pack.PackPipeline;
import net.vulkanmod.render.pack.ShaderPack;
import net.vulkanmod.render.sodium.SodiumShaderBridge;
import net.vulkanmod.render.vsr.Vsr;
import net.vulkanmod.rendergraph.radiance.RadianceDepthCaptureProvider;
import net.vulkanmod.rendergraph.radiance.RadianceMaterialProvider;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.shader.RenderPipelineProvider;
import net.vulkanmod.plugin.PluginRegistry;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.DrawUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class DefaultMainPass implements MainPass, EngineContext {

    public static DefaultMainPass create() {
        return new DefaultMainPass();
    }

    private final SwapChain swapChain;
    private Framebuffer mainFramebuffer;
    private Framebuffer scaledFramebuffer;

    private final DefaultEngineResourceRegistry resourceRegistry = new DefaultEngineResourceRegistry();
    private PipelineCapabilities capabilities = new PipelineCapabilities();

    private VulkanImage frameWorldDepth;
    private VulkanImage frameFgDepth;

    private RenderPass mainRenderPass;
    private RenderPass auxRenderPass;
    private RenderPass presentRenderPass;

    private RenderPass swapMainRenderPass;
    private RenderPass swapAuxRenderPass;
    private RenderPass scaledMainRenderPass;
    private RenderPass scaledAuxRenderPass;

    private int scaledFramebufferWidth = -1;
    private int scaledFramebufferHeight = -1;
    private int scaledFramebufferScale = RenderScale.DEFAULT;
    private int scaledColorAttachmentGlId = -1;
    private int scaledDepthAttachmentGlId = -1;
    private boolean renderScaleResolvedThisFrame;
    private boolean mainTargetResolvedThisFrame;
    private boolean scaledFramebufferPendingDispose;
    private static int targetSwitches;

    public FrameGraphImpl frameGraph;
    private final Map<String, FrameGraphImpl> pluginFrameGraphs = new HashMap<>();

    DefaultMainPass() {
        this.swapChain = Vulkan.getSwapChain();
        this.mainFramebuffer = this.swapChain;

        RenderPipelineProvider activeProvider = PluginRegistry.activeShader();
        this.frameGraph = activeProvider.frameGraph().get();

        initializeCapabilities(activeProvider);
        registerCoreResources();
        bindRenderPasses();
        createPresentRenderPass();
    }

    private void initializeCapabilities(RenderPipelineProvider provider) {
        if (provider.plugin() != null) {
            this.capabilities = provider.plugin().createCapabilities();
            this.capabilities.initialize(this);

            // rebindTarget callbacks
            capabilities.depthCapture().ifPresent(d -> {
                if (d instanceof RadianceDepthCaptureProvider rdcp) {
                    rdcp.setRebindTarget(this::rebindMainTarget);
                }
            });

            capabilities.material().ifPresent(m -> {
                if (m instanceof RadianceMaterialProvider rmp) {
                    rmp.setRebindTarget(this::rebindMainTarget);
                }
            });

            // Let plugin register its custom resources
            provider.plugin().onActivate(this, resourceRegistry);
        }
    }

    private void registerCoreResources() {
        resourceRegistry.register("scene", () -> this.mainFramebuffer.getColorAttachment());
        resourceRegistry.register("depthtex", () -> this.frameWorldDepth);
        resourceRegistry.register("fgdepth", () -> this.frameFgDepth);

        resourceRegistry.register("opaquedepth", () -> {
            VulkanImage opaque = capabilities.depthCapture()
                    .map(DepthCaptureProvider::getCapturedOpaqueDepth).orElse(null);
            return opaque != null ? opaque : this.frameWorldDepth;
        });
        resourceRegistry.register("gnormal", () ->
                (this.scaledFramebuffer != null && this.scaledFramebuffer.getColorAttachment2() != null)
                        ? this.scaledFramebuffer.getColorAttachment2() : this.frameWorldDepth);
    }

    public EngineResourceRegistry getResourceRegistry() {
        return resourceRegistry;
    }

    @Override
    public PipelineCapabilities getCapabilities() {
        return capabilities;
    }

    @Override
    public Framebuffer mainFramebuffer() {
        return this.mainFramebuffer;
    }

    @Override
    public SwapChain swapChain() {
        return this.swapChain;
    }

    @Override
    public boolean postShaderActive() {
        return postShaderActiveStatic();
    }

    @Override
    public boolean isScaledFramebuffer() {
        return isUsingScaledFramebuffer();
    }

    @Override
    public int renderWidth() {
        return this.mainFramebuffer.getWidth();
    }

    @Override
    public int renderHeight() {
        return this.mainFramebuffer.getHeight();
    }

    private RenderPass[] buildRenderPasses(Framebuffer framebuffer) {
        RenderPass.Builder builder = RenderPass.builder(framebuffer);
        builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getDepthAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);

        if (builder.getColorAttachmentInfo2() != null) {
            builder.getColorAttachmentInfo2().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            builder.getColorAttachmentInfo2().setOps(VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE);
        }

        RenderPass main = builder.build();

        builder = RenderPass.builder(framebuffer);
        builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getDepthAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        if (builder.getColorAttachmentInfo2() != null) {
            builder.getColorAttachmentInfo2().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            builder.getColorAttachmentInfo2().setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE);
        }

        return new RenderPass[]{main, builder.build()};
    }

    private void bindRenderPasses() {
        if (this.scaledFramebuffer != null && this.mainFramebuffer == this.scaledFramebuffer) {
            if (this.scaledMainRenderPass == null) {
                RenderPass[] passes = buildRenderPasses(this.scaledFramebuffer);
                this.scaledMainRenderPass = passes[0];
                this.scaledAuxRenderPass = passes[1];
            }

            this.mainRenderPass = this.scaledMainRenderPass;
            this.auxRenderPass = this.scaledAuxRenderPass;
            return;
        }

        if (this.swapMainRenderPass == null) {
            RenderPass[] passes = buildRenderPasses(this.swapChain);
            this.swapMainRenderPass = passes[0];
            this.swapAuxRenderPass = passes[1];
        }

        this.mainRenderPass = this.swapMainRenderPass;
        this.auxRenderPass = this.swapAuxRenderPass;
    }

    private void createPresentRenderPass() {
        RenderPass.Builder builder = RenderPass.builder(this.swapChain);

        builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getDepthAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_DONT_CARE);

        this.presentRenderPass = builder.build();
    }

    private void setMainFramebuffer(Framebuffer framebuffer) {
        if (this.mainFramebuffer == framebuffer)
            return;

        targetSwitches++;

        this.mainFramebuffer = framebuffer;
        bindRenderPasses();
    }

    private void ensureMainFramebuffer() {
        if (this.mainTargetResolvedThisFrame)
            return;

        resolveMainFramebuffer();
    }

    private void resolveMainFramebuffer() {
        this.mainTargetResolvedThisFrame = true;

        int scale = RenderScale.clamp(Initializer.CONFIG.renderScale);

        if (!shouldUseScaledFramebuffer(scale)) {
            setMainFramebuffer(this.swapChain);
            this.scaledFramebufferPendingDispose = true;
            return;
        }

        this.scaledFramebufferPendingDispose = false;

        int scaledWidth = RenderScale.scaleDimension(this.swapChain.getWidth(), scale);
        int scaledHeight = RenderScale.scaleDimension(this.swapChain.getHeight(), scale);

        if (this.scaledFramebuffer == null
                || this.scaledFramebufferWidth != scaledWidth
                || this.scaledFramebufferHeight != scaledHeight
                || this.scaledFramebufferScale != scale) {
            disposeScaledFramebuffer();

            this.scaledFramebuffer = Framebuffer.builder(scaledWidth, scaledHeight, 1, true)
                    .setLinearFiltering(true)
                    .setColorAttachment2Format(VK_FORMAT_R16G16B16A16_SFLOAT)
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
                && minecraft.level != null;

        if (postShaderActive())
            return base;

        return RenderScale.isScaled(scale) && base && minecraft.screen == null;
    }

    public static boolean postShaderActiveStatic() {
        return Initializer.CONFIG.shadersEnabled && !"off".equals(Initializer.CONFIG.selectedShader)
                && !SodiumShaderBridge.isActive();
    }

    private void disposeScaledFramebuffer() {
        if (this.scaledFramebuffer == null) {
            return;
        }

        if (this.scaledMainRenderPass != null) {
            this.scaledMainRenderPass.cleanUp();
            this.scaledMainRenderPass = null;
        }
        if (this.scaledAuxRenderPass != null) {
            this.scaledAuxRenderPass.cleanUp();
            this.scaledAuxRenderPass = null;
        }

        this.scaledFramebuffer.cleanUp();
        this.scaledFramebuffer = null;

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

    public static int consumeTargetSwitches() {
        int value = targetSwitches;
        targetSwitches = 0;
        return value;
    }

    @Override
    public int renderTargetWidth() {
        return this.mainFramebuffer.getWidth();
    }

    @Override
    public int renderTargetHeight() {
        return this.mainFramebuffer.getHeight();
    }

    @Override
    public String renderScaleStatus() {
        String target = this.scaledFramebuffer == null
                ? "none"
                : this.scaledFramebufferWidth + "x" + this.scaledFramebufferHeight;

        return String.format("scaledTarget=%s  swapchain=%dx%d  postShader=%b",
                target, this.swapChain.getWidth(), this.swapChain.getHeight(), postShaderActive());
    }

    @Override
    public void begin(VkCommandBuffer commandBuffer, MemoryStack stack) {
        this.renderScaleResolvedThisFrame = false;
        this.mainTargetResolvedThisFrame = false;
        this.frameWorldDepth = null;
        this.frameFgDepth = null;

        capabilities.depthCapture().ifPresent(d -> {
            if (d instanceof RadianceDepthCaptureProvider rdcp) {
                rdcp.beginFrame();
            }
        });

        if (this.scaledFramebufferPendingDispose) {
            disposeScaledFramebuffer();
            this.scaledFramebufferPendingDispose = false;
        }

        resolveMainFramebuffer();

        frameGraph.get().execute(
                Phase.FRAME_START, commandBuffer, stack, resourceRegistry::resolve, () -> {});

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
        if (result != VK_SUCCESS)
            throw new RuntimeException("Failed to record command buffer:" + result);
    }

    @Override
    public void resolveRenderScaleForGui() {
        if (!isUsingScaledFramebuffer() || this.renderScaleResolvedThisFrame)
            return;

        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
        Renderer.getInstance().endRenderPass(commandBuffer);
        resolveScaledFramebufferToSwapchain(commandBuffer, true);
        this.renderScaleResolvedThisFrame = true;
        setMainFramebuffer(this.swapChain);
    }

    private void updateVsrState() {
        Framebuffer source = this.mainFramebuffer;
        int backend = Vsr.clampBackend(Initializer.CONFIG.vsrBackend);

        boolean oneToOne = source.getWidth() == this.swapChain.getWidth()
                && source.getHeight() == this.swapChain.getHeight();

        if (oneToOne && backend == Vsr.FSR1) {
            backend = Vsr.SHARPEN_ONLY;
        }

        if (!postShaderActive() && backend == Vsr.VTU) {
            backend = Vsr.FSR1;
        }

        Vsr.update(source.getWidth(), source.getHeight(),
                source.getWidth(), source.getHeight(),
                this.swapChain.getWidth(), this.swapChain.getHeight(),
                backend, Initializer.CONFIG.vsrSharpness);
    }

    private void blitToSwapchain() {
        updateVsrState();
        VTextureSelector.bindTexture(1, VTextureSelector.getWhiteTexture());
        VTextureSelector.bindTexture(2, VTextureSelector.getWhiteTexture());
        DrawUtil.blitVsrToScreen();
    }

    private void resolveScaledFramebufferToSwapchain(VkCommandBuffer commandBuffer, boolean keepRendering) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.mainFramebuffer.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            if (this.scaledFramebuffer != null && this.scaledFramebuffer.getColorAttachment2() != null) {
                this.scaledFramebuffer.getColorAttachment2().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            }

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
            blitToSwapchain();

            if (!keepRendering)
                Renderer.getInstance().endRenderPass(commandBuffer);
        }
    }

    private void resolvePostShader(VkCommandBuffer commandBuffer, MemoryStack stack, boolean keepRendering) {
        VulkanImage worldDepth = capabilities.depthCapture()
                .map(DepthCaptureProvider::getWorldDepth).orElse(null);
        VulkanImage fgDepth = capabilities.depthCapture()
                .map(DepthCaptureProvider::getForegroundDepth).orElse(null);

        boolean depthShader = worldDepth != null;
        if (depthShader) {
            worldDepth.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            if (fgDepth != null)
                fgDepth.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            setFrameDepths(worldDepth, fgDepth);
        }

        if (depthShader && resolveFrameGraph(commandBuffer, stack, keepRendering))
            return;

        if (depthShader && resolveShaderPack(commandBuffer, stack, keepRendering))
            return;

        this.swapChain.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        this.swapChain.beginRenderPass(commandBuffer, this.presentRenderPass, stack);
        Renderer.clearViewportScale();
        Renderer.resetViewport();
        Renderer.resetScissor();
        VTextureSelector.bindTexture(0, this.mainFramebuffer.getColorAttachment());
        blitToSwapchain();
        if (!keepRendering)
            Renderer.getInstance().endRenderPass(commandBuffer);
    }

    private void setFrameDepths(VulkanImage worldDepth, VulkanImage fgDepth) {
        this.frameWorldDepth = worldDepth;
        this.frameFgDepth = fgDepth;
    }

    private Runnable presentBeginCallback(VkCommandBuffer commandBuffer, MemoryStack stack) {
        return () -> {
            this.swapChain.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            this.swapChain.beginRenderPass(commandBuffer, this.presentRenderPass, stack);

            Renderer.clearViewportScale();
            Renderer.resetViewport();
            Renderer.resetScissor();
        };
    }

    private boolean resolveFrameGraph(VkCommandBuffer commandBuffer, MemoryStack stack, boolean keepRendering) {
        String id = Initializer.CONFIG.selectedShader;
        FrameGraphImpl activeImpl;
        RenderPipelineProvider activeProvider = PluginRegistry.get(id);

        if (activeProvider == null) {
            return false;
        }

        if (id.equals(this.frameGraph.get().getId()))
            activeImpl = this.frameGraph;
        else {
            activeImpl = this.pluginFrameGraphs.computeIfAbsent(id, k -> {
                activeProvider.pipelineManager().get().initialize();
                return activeProvider.frameGraph().get();
            });
            this.frameGraph = activeImpl;
        }

        FrameGraph graph = activeImpl.get();

        if (!graph.pipelinesReady()) {
            return false;
        }

        if (activeProvider.plugin() != null) {
            activeProvider.plugin().configureGraph(graph, this);
        }

        graph.resize(commandBuffer, stack, this.mainFramebuffer.getWidth(), this.mainFramebuffer.getHeight());

        if (!graph.targetsReady()) {
            return false;
        }

        updateVsrState();

        boolean ran = graph.execute(Phase.POST_PROCESS, commandBuffer, stack,
                resourceRegistry::resolve,
                presentBeginCallback(commandBuffer, stack));

        if (!ran) return false;
        if (!keepRendering)
            Renderer.getInstance().endRenderPass(commandBuffer);

        return true;
    }

    private boolean resolveShaderPack(VkCommandBuffer commandBuffer, MemoryStack stack, boolean keepRendering) {
        ShaderPack pack = PackPipeline.get(Initializer.CONFIG.selectedShader);
        if (pack == null || !PackPipeline.structureValid(pack)) {
            return false;
        }

        PackPipeline.ensureTargets(pack, commandBuffer, stack, this.mainFramebuffer.getWidth(), this.mainFramebuffer.getHeight());

        if (!PackPipeline.pipelinesReady(pack) || !PackPipeline.targetsReady(pack)) {
            return false;
        }

        boolean ran = PackPipeline.runFrame(pack, commandBuffer, stack,
                resourceRegistry::resolve,
                presentBeginCallback(commandBuffer, stack));

        if (!ran)
            return false;

        if (!keepRendering)
            Renderer.getInstance().endRenderPass(commandBuffer);

        return true;
    }

    @Override
    public FrameGraphImpl getFrameGraph() {
        return this.frameGraph;
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

            if (isUsingScaledFramebuffer())
                Renderer.setViewportScale(this.swapChain.getWidth(), this.swapChain.getHeight());
            else
                Renderer.clearViewportScale();
        }

    }

    @Override
    public void bindAsTexture() {
        ensureMainFramebuffer();
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        RenderPass boundRenderPass = Renderer.getInstance().getBoundRenderPass();

        if (boundRenderPass == this.mainRenderPass || boundRenderPass == this.auxRenderPass)
            Renderer.getInstance().endRenderPass(commandBuffer);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.mainFramebuffer.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        }

        VTextureSelector.bindTexture(this.mainFramebuffer.getColorAttachment());
    }

    public int getColorAttachmentGlId() {
        ensureMainFramebuffer();

        if (isUsingScaledFramebuffer())
            return this.scaledColorAttachmentGlId;

        return Vulkan.getSwapChain().getColorAttachmentGlId();
    }

    @Override
    public int getDepthAttachmentGlId() {
        ensureMainFramebuffer();

        if (isUsingScaledFramebuffer())
            return this.scaledDepthAttachmentGlId;

        return Vulkan.getSwapChain().getDepthAttachmentGlId();
    }
}
