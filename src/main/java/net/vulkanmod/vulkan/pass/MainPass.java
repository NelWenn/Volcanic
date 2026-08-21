package net.vulkanmod.vulkan.pass;

import net.vulkanmod.render.framegraph.FrameGraphImpl;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.function.IntConsumer;

public interface MainPass {

    FrameGraphImpl getFrameGraph();

    PipelineCapabilities getCapabilities();

    void begin(VkCommandBuffer commandBuffer, MemoryStack stack);

    void end(VkCommandBuffer commandBuffer);

    default void onDepthClear(Framebuffer framebuffer) {
        getCapabilities().depthCapture().ifPresent(d -> d.onDepthClear(framebuffer));
    }

    default boolean suppressDepthClear(Framebuffer framebuffer) {
        return getCapabilities().depthCapture()
                .map(d -> d.suppressDepthClear(framebuffer)).orElse(false);
    }

    default VulkanImage getCapturedOpaqueDepth() {
        return getCapabilities().depthCapture()
                .map(DepthCaptureProvider::getCapturedOpaqueDepth).orElse(null);
    }

    default void cleanup() {
        getFrameGraph().dispose();
        getCapabilities().cleanup();
    }

    default void rebindMainTarget() {}

    default void renderEntityShadows(Runnable casters, IntConsumer tintCascade) {
        getCapabilities().shadow().ifPresent(s -> {
            s.renderEntityShadows(casters, tintCascade);
            rebindMainTarget();
        });
    }

    default void applyColoredShadow() {
        getCapabilities().shadow().ifPresent(ShadowProvider::applyColoredShadow);
    }

    default void captureOpaqueDepth() {
        getCapabilities().depthCapture().ifPresent(DepthCaptureProvider::captureOpaqueDepth);
    }

    default void prepareMaterialBuffer(double camX, double camY, double camZ, Matrix4f modelView, Matrix4f projection) {
        getCapabilities().material().ifPresent(m -> m.prepareMaterialBuffer(camX, camY, camZ, modelView, projection));
    }

    default void bindAsTexture() {}

    default void resolveRenderScaleForGui() {}

    default int renderTargetWidth() { return 0; }

    default int renderTargetHeight() { return 0; }

    default String renderScaleStatus() { return "n/a"; }

    default int getDepthAttachmentGlId() { return -1; }

    default int getColorAttachmentGlId() { return -1; }
}
