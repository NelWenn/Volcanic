package net.vulkanmod.render.sodium;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Optional;

public final class SodiumShaderBridge {

    private static final ResourceLocation BLOCK_FRAGMENT =
            ResourceLocation.fromNamespaceAndPath("sodium", "shaders/blocks/block_layer_opaque.fsh");
    private static final ResourceLocation CLOUDS_FRAGMENT =
            ResourceLocation.fromNamespaceAndPath("sodium", "shaders/clouds.fsh");

    private static final EnumMap<TerrainRenderType, GraphicsPipeline> PIPELINES = new EnumMap<>(TerrainRenderType.class);
    private static boolean active;
    private static String cloudsFragment;

    private SodiumShaderBridge() {
    }

    public static void refresh(ResourceProvider provider) {
        refreshTerrain(provider);
        refreshClouds(provider);
    }

    private static void refreshTerrain(ResourceProvider provider) {
        Optional<Resource> fragment = provider.getResource(BLOCK_FRAGMENT);
        if (fragment.isEmpty() || !isResourcePackSource(fragment.get())) {
            disable();
            return;
        }

        try {
            String packFragment = read(fragment.get());
            EnumMap<TerrainRenderType, GraphicsPipeline> built = new EnumMap<>(TerrainRenderType.class);
            for (TerrainRenderType renderType : TerrainRenderType.VALUES) {
                built.put(renderType, SodiumTerrainCompiler.compile(renderType, packFragment, provider));
            }

            cleanup();
            PIPELINES.putAll(built);
            active = true;
            Initializer.LOGGER.info("Sodium core shader pack active: compiled block_layer_opaque for {} terrain passes", built.size());
        } catch (Throwable t) {
            Initializer.LOGGER.error("Failed to apply Sodium core shader pack, falling back to native terrain shader", t);
            disable();
        }
    }

    private static void refreshClouds(ResourceProvider provider) {
        Optional<Resource> fragment = provider.getResource(CLOUDS_FRAGMENT);
        if (fragment.isPresent() && isResourcePackSource(fragment.get())) {
            try {
                cloudsFragment = read(fragment.get());
                Initializer.LOGGER.info("Sodium core shader pack: clouds.fsh override detected");
            } catch (Exception e) {
                Initializer.LOGGER.error("Failed to read Sodium clouds fragment shader", e);
                cloudsFragment = null;
            }
        } else {
            cloudsFragment = null;
        }
    }

    private static boolean isResourcePackSource(Resource resource) {
        String source = resource.sourcePackId();
        return source != null && source.startsWith("file/");
    }

    public static GraphicsPipeline getPipeline(TerrainRenderType renderType) {
        return active ? PIPELINES.get(renderType) : null;
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean hasCloudsOverride() {
        return cloudsFragment != null;
    }

    public static GraphicsPipeline buildCloudsPipeline(VertexFormat format) {
        if (cloudsFragment == null) {
            return null;
        }
        try {
            return SodiumTerrainCompiler.compileClouds(cloudsFragment, format);
        } catch (Exception e) {
            Initializer.LOGGER.error("Failed to build Sodium clouds pipeline, using builtin", e);
            return null;
        }
    }

    private static void disable() {
        cleanup();
        active = false;
    }

    private static void cleanup() {
        for (GraphicsPipeline pipeline : PIPELINES.values()) {
            if (pipeline != null) {
                pipeline.cleanUp();
            }
        }
        PIPELINES.clear();
    }

    private static String read(Resource resource) {
        try (InputStream stream = resource.open()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read Sodium shader resource", e);
        }
    }
}
