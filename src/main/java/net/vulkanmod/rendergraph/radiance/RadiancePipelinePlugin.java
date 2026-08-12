package net.vulkanmod.rendergraph.radiance;

import net.vulkanmod.Initializer;
import net.vulkanmod.config.plugin.PluginTargetType;
import net.vulkanmod.render.framegraph.FrameGraph;
import net.vulkanmod.render.framegraph.FrameGraphImpl;
import net.vulkanmod.plugin.RenderPipelinePlugin;
import net.vulkanmod.vulkan.pass.EngineContext;
import net.vulkanmod.vulkan.pass.EngineResourceRegistry;
import net.vulkanmod.vulkan.pass.PipelineCapabilities;
import net.vulkanmod.vulkan.pass.ShadowProvider;
import net.vulkanmod.vulkan.pass.MaterialProvider;
import net.vulkanmod.vulkan.shader.PipelineManager;
import net.vulkanmod.vulkan.texture.VTextureSelector;

public final class RadiancePipelinePlugin implements RenderPipelinePlugin {
    @Override
    public String id() {
        return "radiance";
    }

    @Override
    public String name() {
        return "Radiance";
    }

    @Override
    public String description() {
        return "The volcanic's official shader";
    }

    @Override
    public String version() {
        return "0.1-alpha";
    }

    @Override
    public String byline() {
        return ""; //TODO: complete byLine
    }

    @Override
    public String icon() {
        return ""; //TODO: complete icon path
    }

    @Override
    public String banner() {
        return ""; //TODO: complete banner path
    }

    @Override
    public String[] tags() {
        return new String[] { "shader", "visuals", "rendering" };
    }

    @Override
    public String[] authors() {
        return new String[]{ "NelWenn", "RevoJava" };
    }

    @Override
    public PluginTargetType type() {
        return PluginTargetType.SHADERS;
    }

    @Override
    public PipelineManager createPipelineManager() {
        return new RadiancePipeline();
    }

    @Override
    public FrameGraphImpl createFrameGraph() {
        return new RadianceGraph();
    }

    @Override
    public PipelineCapabilities createCapabilities() {
        PipelineCapabilities caps = new PipelineCapabilities();

        RadianceDepthCaptureProvider depthCapture = new RadianceDepthCaptureProvider();
        RadianceShadowProvider shadow = new RadianceShadowProvider();
        RadianceMaterialProvider material = new RadianceMaterialProvider();

        shadow.setDepthCapture(depthCapture);

        caps.setDepthCaptureProvider(depthCapture);
        caps.setShadowProvider(shadow);
        caps.setMaterialProvider(material);

        return caps;
    }

    @Override
    public void onActivate(EngineContext context, EngineResourceRegistry resources) {
        // Register shadow cascade resources
        resources.register("shadowtex0", () -> {
            ShadowProvider shadow = getActiveShadow(context);
            return shadow != null && shadow.isReady() ? shadow.getCascadeDepthImage(0) : null;
        });
        resources.register("shadowtex1", () -> {
            ShadowProvider shadow = getActiveShadow(context);
            return shadow != null && shadow.isReady() ? shadow.getCascadeDepthImage(1) : null;
        });
        resources.register("shadowtex2", () -> {
            ShadowProvider shadow = getActiveShadow(context);
            return shadow != null && shadow.isReady() ? shadow.getCascadeDepthImage(2) : null;
        });

        // Register material resources
        resources.register("material", () -> {
            MaterialProvider mat = getActiveMaterial(context);
            return mat != null ? mat.getMaterialImage() : VTextureSelector.getWhiteTexture();
        });
        resources.register("materialdepth", () -> {
            MaterialProvider mat = getActiveMaterial(context);
            return mat != null ? mat.getMaterialDepthImage() : null;
        });
    }

    @Override
    public void configureGraph(FrameGraph graph, EngineContext context) {
        graph.setTargetScale("light", Initializer.CONFIG.optimizedShadows ? 0.5f : 1.0f);
        graph.setTargetScale("vtu", context.renderWidth() > 0
                ? (float) context.swapChain().getWidth() / context.renderWidth()
                : 1.0f);
    }

    private static ShadowProvider getActiveShadow(EngineContext context) {
        net.vulkanmod.vulkan.Renderer renderer = net.vulkanmod.vulkan.Renderer.getInstance();
        if (renderer == null) return null;
        return renderer.getMainPass().getCapabilities().shadow().orElse(null);
    }

    private static MaterialProvider getActiveMaterial(EngineContext context) {
        net.vulkanmod.vulkan.Renderer renderer = net.vulkanmod.vulkan.Renderer.getInstance();
        if (renderer == null) return null;
        return renderer.getMainPass().getCapabilities().material().orElse(null);
    }
}
