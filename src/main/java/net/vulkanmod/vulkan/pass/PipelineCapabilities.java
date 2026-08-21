package net.vulkanmod.vulkan.pass;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PipelineCapabilities {
    private ShadowProvider shadowProvider;
    private MaterialProvider materialProvider;
    private DepthCaptureProvider depthCaptureProvider;

    private Map<Class<? extends PipelineFeature>, PipelineFeature> customCapabilities = new HashMap<>();

    public void setShadowProvider(ShadowProvider provider) {
        this.shadowProvider = provider;
    }

    public void setMaterialProvider(MaterialProvider provider) {
        this.materialProvider = provider;
    }

    public void setDepthCaptureProvider(DepthCaptureProvider provider) {
        this.depthCaptureProvider = provider;
    }

    public Optional<ShadowProvider> shadow() {
        return Optional.ofNullable(shadowProvider);
    }

    public Optional<MaterialProvider> material() {
        return Optional.ofNullable(materialProvider);
    }

    public Optional<DepthCaptureProvider> depthCapture() {
        return Optional.ofNullable(depthCaptureProvider);
    }

    public void initialize(EngineContext context) {
        if (shadowProvider != null) shadowProvider.initialize(context);
        if (materialProvider != null) materialProvider.initialize(context);
        if (depthCaptureProvider != null) depthCaptureProvider.initialize(context);
    }

    public void cleanup() {
        if (shadowProvider != null) shadowProvider.cleanup();
        if (materialProvider != null) materialProvider.cleanup();
        if (depthCaptureProvider != null) depthCaptureProvider.cleanup();
    }
}
