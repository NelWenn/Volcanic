package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.render.pipeline.RenderPipeline;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.shader.pipeline.PipelineRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class PipelineManager {
    public VertexFormat TERRAIN_VERTEX_FORMAT;

    public static final String ROLE_TERRAIN_MAIN = "terrain.main";
    public static final String ROLE_TERRAIN_FADE = "terrain.fade";
    public static final String ROLE_SHADOW_TERRAIN = "terrain.shadow";
    public static final String ROLE_SHADOW_TINT = "terrain.shadow_tint";
    public static final String ROLE_MATERIAL = "terrain.material";

    public static final String PIPELINE_FAST_BLIT = "core.fast_blit";
    public static final String PIPELINE_RENDER_SCALE_BLIT = "core.render_scale_blit";
    public static final String PIPELINE_EXTERNAL_LOD = "core.external_lod";
    public static final String PIPELINE_EXTERNAL_LOD_TEXTURED = "core.external_lod_textured";
    public static final String PIPELINE_EXTERNAL_LOD_WATER = "core.external_lod_water";
    public static final String PIPELINE_EXTERNAL_LOD_WATER_TEXTURED = "core.external_lod_water_textured";
    public static final String PIPELINE_EXTERNAL_LOD_SOLID = "core.external_lod_solid";
    public static final String PIPELINE_EXTERNAL_LOD_TEXTURED_SOLID = "core.external_lod_textured_solid";

    protected final Map<String, Function<TerrainRenderType, GraphicsPipeline>> terrainRoleProviders = new HashMap<>();
    private final Map<String, GraphicsPipeline> namedPipelines = new HashMap<>();

    protected Function<TerrainRenderType, GraphicsPipeline> shaderGetter;

    private ClassLoader resourceClassLoader;

    public abstract void init();

    public abstract void setDefaultShader();

    public final void setResourceClassLoader(ClassLoader resourceClassLoader) {
        this.resourceClassLoader = resourceClassLoader;
    }

    public final void initialize() {
        SPIRVUtils.withResourceClassLoader(resourceClassLoader, this::init);
    }

    public void registerTerrainRole(String role, Function<TerrainRenderType, GraphicsPipeline> provider) {
        terrainRoleProviders.put(role, provider);
    }

    public RenderPipeline getPipeline(String role, TerrainRenderType renderType) {
        Function<TerrainRenderType, GraphicsPipeline> provider = terrainRoleProviders.get(role);
        if (provider == null)
            throw new IllegalStateException("No pipeline registered for role: " + role);
        return provider.apply(renderType);
    }

    public void registerPipeline(String key, GraphicsPipeline pipeline) {
        namedPipelines.put(key, pipeline);
    }

    public GraphicsPipeline getPipeline(String key) {
        return namedPipelines.get(key);
    }

    public void setTerrainVertexFormat(VertexFormat format) {
        TERRAIN_VERTEX_FORMAT = format;
    }

    public void setShaderGetter(Function<TerrainRenderType, GraphicsPipeline> consumer) {
        shaderGetter = consumer;
    }

    public void destroyPipelines() {
        PipelineRegistry.cleanUp();
    }
}
