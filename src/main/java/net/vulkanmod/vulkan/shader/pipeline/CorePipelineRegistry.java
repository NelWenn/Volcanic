package net.vulkanmod.vulkan.shader.pipeline;

import net.vulkanmod.vulkan.shader.pipeline.definitions.core.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps vanilla {@code ShaderInstance} names to their built-in {@link CoreGfxPipeline} definition class
 */
public final class CorePipelineRegistry {
    private static final Map<String, Class<? extends PipelineDefinition>> DEFINITIONS = new HashMap<>();

    static {
        register(ArmorCutoutNoCullPipeline.class);
        register(ArmorEntityGlintPipeline.class);
        register(ArmorGlintPipeline.class);
        register(BeaconBeamPipeline.class);
        register(BlitScreenPipeline.class);
        register(BreezeWindPipeline.class);
        register(CloudsPipeline.class);
        register(CrumblingPipeline.class);
        register(CutoutMippedPipeline.class);
        register(CutoutPipeline.class);
        register(EndPortalPipeline.class);
        register(EnergySwirlPipeline.class);
        register(EntityAlphaPipeline.class);
        register(EntityCutoutNoCullPipeline.class);
        register(EntityCutoutNoCullZOffsetPipeline.class);
        register(EntityCutoutPipeline.class);
        register(EntityDecalPipeline.class);
        register(EntityGlintDirectPipeline.class);
        register(EntityGlintPipeline.class);
        register(EntityNoOutlinePipeline.class);
        register(EntityShadowPipeline.class);
        register(EntitySmoothCutoutPipeline.class);
        register(EntitySolidPipeline.class);
        register(EntityTranslucentCullPipeline.class);
        register(EntityTranslucentEmissivePipeline.class);
        register(EntityTranslucentPipeline.class);
        register(EyesPipeline.class);
        register(GlintDirectPipeline.class);
        register(GlintPipeline.class);
        register(GlintTranslucentPipeline.class);
        register(GuiGhostRecipeOverlayPipeline.class);
        register(GuiOverlayPipeline.class);
        register(GuiPipeline.class);
        register(GuiTextHighlightPipeline.class);
        register(ItemEntityTranslucentCullPipeline.class);
        register(LeashPipeline.class);
        register(LightningPipeline.class);
        register(LinesPipeline.class);
        register(OutlinePipeline.class);
        register(ParticlePipeline.class);
        register(PositionColorLightmapPipeline.class);
        register(PositionColorNormalPipeline.class);
        register(PositionColorPipeline.class);
        register(PositionColorTexLightmapPipeline.class);
        register(PositionColorTexPipeline.class);
        register(PositionPipeline.class);
        register(PositionTexColorNormalPipeline.class);
        register(PositionTexColorPipeline.class);
        register(PositionTexPipeline.class);
        register(SolidPipeline.class);
        register(TextBackgroundPipeline.class);
        register(TextBackgroundSeeThroughPipeline.class);
        register(TextIntensityPipeline.class);
        register(TextIntensitySeeThroughPipeline.class);
        register(TextPipeline.class);
        register(TextSeeThroughPipeline.class);
        register(TranslucentMovingBlockPipeline.class);
        register(TranslucentNoCrumblingPipeline.class);
        register(TranslucentPipeline.class);
        register(TripwirePipeline.class);
        register(WaterMaskPipeline.class);
    }

    private CorePipelineRegistry() {
    }

    private static void register(Class<? extends PipelineDefinition> definition) {
        CoreGfxPipeline meta = definition.getAnnotation(CoreGfxPipeline.class);
        if (meta == null)
            throw new IllegalStateException(definition.getName() + " has no @CoreGfxPipeline annotation");
        DEFINITIONS.put(meta.name(), definition);
    }

    public static Class<? extends PipelineDefinition> get(String name) {
        return DEFINITIONS.get(name);
    }
}
