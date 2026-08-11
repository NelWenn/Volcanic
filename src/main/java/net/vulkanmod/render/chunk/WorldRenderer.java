package net.vulkanmod.render.chunk;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import net.vulkanmod.render.chunk.util.StaticQueue;
import net.vulkanmod.render.material.PbrAtlas;
import net.vulkanmod.render.pipeline.RenderPipeline;
import net.vulkanmod.render.pipeline.RenderStateSnapshot;
import net.vulkanmod.render.chunk.buffer.DrawBuffers;
import net.vulkanmod.render.chunk.frustum.VFrustum;
import net.vulkanmod.render.chunk.build.BlockRenderer;
import net.vulkanmod.render.chunk.build.RenderRegionBuilder;
import net.vulkanmod.render.chunk.build.TaskDispatcher;
import net.vulkanmod.render.chunk.build.task.ChunkTask;
import net.vulkanmod.render.chunk.graph.SectionGraph;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.memory.Buffer;
import net.vulkanmod.vulkan.memory.IndexBuffer;
import net.vulkanmod.vulkan.memory.IndirectBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.pass.ShadowMap;
import net.vulkanmod.vulkan.shader.PipelineManager;
import net.vulkanmod.vulkan.shader.PipelineState;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.*;

public class WorldRenderer {
    private static WorldRenderer INSTANCE;

    private final Minecraft minecraft;
    private ClientLevel level;
    private int renderDistance;
    private final RenderBuffers renderBuffers;

    private Vec3 cameraPos;
    private int lastCameraSectionX;
    private int lastCameraSectionY;
    private int lastCameraSectionZ;
    private float lastCameraX;
    private float lastCameraY;
    private float lastCameraZ;
    private float lastCamRotX;
    private float lastCamRotY;

    private SectionGrid sectionGrid;

    private SectionGraph sectionGraph;
    private boolean graphNeedsUpdate;

    private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();

    private final TaskDispatcher taskDispatcher;

    private double xTransparentOld;
    private double yTransparentOld;
    private double zTransparentOld;

    IndirectBuffer[] indirectBuffers;
    private IndirectBuffer[][] shadowIndirectBuffers;
    private IndirectBuffer[][] shadowTintIndirectBuffers;

    public RenderRegionBuilder renderRegionCache;

    private final List<Runnable> onAllChangedCallbacks = new ObjectArrayList<>();

    private PipelineManager pipelineManager;

    private WorldRenderer(RenderBuffers renderBuffers) {
        this.minecraft = Minecraft.getInstance();
        this.renderBuffers = renderBuffers;
        this.renderRegionCache = new RenderRegionBuilder();
        this.taskDispatcher = new TaskDispatcher();

        ChunkTask.setTaskDispatcher(this.taskDispatcher);
        allocateIndirectBuffers();

        BlockRenderer.setBlockColors(this.minecraft.getBlockColors());

        Renderer.getInstance().addOnResizeCallback(() -> {
            if (this.indirectBuffers.length != Renderer.getFramesNum())
                allocateIndirectBuffers();
        });

        pipelineManager = Renderer.getInstance().getPipelineManager();
    }

    private void allocateIndirectBuffers() {
        if (this.indirectBuffers != null)
            Arrays.stream(this.indirectBuffers).forEach(Buffer::freeBuffer);

        this.indirectBuffers = new IndirectBuffer[Renderer.getFramesNum()];

        for (int i = 0; i < this.indirectBuffers.length; ++i) {
            this.indirectBuffers[i] = new IndirectBuffer(1000000, MemoryTypes.HOST_MEM);

        }

        if (this.shadowIndirectBuffers != null)
            for (IndirectBuffer[] perCascade : this.shadowIndirectBuffers)
                Arrays.stream(perCascade).forEach(Buffer::freeBuffer);

        this.shadowIndirectBuffers = new IndirectBuffer[ShadowMap.CASCADES][Renderer.getFramesNum()];

        for (int c = 0; c < this.shadowIndirectBuffers.length; ++c) {
            for (int i = 0; i < this.shadowIndirectBuffers[c].length; ++i) {
                this.shadowIndirectBuffers[c][i] = new IndirectBuffer(1000000, MemoryTypes.HOST_MEM);
            }
        }

        if (this.shadowTintIndirectBuffers != null)
            for (IndirectBuffer[] perCascade : this.shadowTintIndirectBuffers)
                Arrays.stream(perCascade).forEach(Buffer::freeBuffer);
        this.shadowTintIndirectBuffers = new IndirectBuffer[ShadowMap.CASCADES][Renderer.getFramesNum()];
        for (int c = 0; c < this.shadowTintIndirectBuffers.length; ++c) {
            for (int i = 0; i < this.shadowTintIndirectBuffers[c].length; ++i) {
                this.shadowTintIndirectBuffers[c][i] = new IndirectBuffer(1000000, MemoryTypes.HOST_MEM);
            }
        }
    }

    public static WorldRenderer init(RenderBuffers renderBuffers) {
        if (INSTANCE != null)
            return INSTANCE;
        else
            return INSTANCE = new WorldRenderer(renderBuffers);
    }

    public static WorldRenderer getInstance() {
        return INSTANCE;
    }

    public static ClientLevel getLevel() {
        return INSTANCE.level;
    }

    public static Vec3 getCameraPos() {
        return INSTANCE.cameraPos;
    }

    public void setupRenderer(Camera camera, Frustum frustum, boolean isCapturedFrustum, boolean spectator) {
        this.cameraPos = camera.getPosition();
        if (this.minecraft.options.getEffectiveRenderDistance() != this.renderDistance) {
            this.allChanged();
        }

        this.level.getProfiler().push("camera");
        float cameraX = (float) cameraPos.x();
        float cameraY = (float) cameraPos.y();
        float cameraZ = (float) cameraPos.z();

        int sectionX = SectionPos.posToSectionCoord(cameraX);
        int sectionY = SectionPos.posToSectionCoord(cameraY);
        int sectionZ = SectionPos.posToSectionCoord(cameraZ);

        if (this.lastCameraSectionX != sectionX || this.lastCameraSectionY != sectionY || this.lastCameraSectionZ != sectionZ) {
            this.lastCameraSectionX = sectionX;
            this.lastCameraSectionY = sectionY;
            this.lastCameraSectionZ = sectionZ;
            this.sectionGrid.repositionCamera(cameraX, cameraZ);
        }

        double entityDistanceScaling = this.minecraft.options.entityDistanceScaling().get();
        Entity.setViewScale(Mth.clamp((double) this.renderDistance / 8.0D, 1.0D, 2.5D) * entityDistanceScaling);

        this.level.getProfiler().popPush("cull");
        this.minecraft.getProfiler().popPush("culling");

        this.minecraft.getProfiler().popPush("update");

        boolean cameraMoved = false;

        float d_xRot = Math.abs(camera.getXRot() - this.lastCamRotX);
        float d_yRot = Math.abs(camera.getYRot() - this.lastCamRotY);

        cameraMoved |= d_xRot > 2.0f || d_yRot > 2.0f;
        cameraMoved |= cameraX != this.lastCameraX || cameraY != this.lastCameraY || cameraZ != this.lastCameraZ;

        this.graphNeedsUpdate |= cameraMoved;

        if (!isCapturedFrustum) {

            if (this.graphNeedsUpdate) {
                this.graphNeedsUpdate = false;
                this.lastCameraX = cameraX;
                this.lastCameraY = cameraY;
                this.lastCameraZ = cameraZ;
                this.lastCamRotX = camera.getXRot();
                this.lastCamRotY = camera.getYRot();

                this.sectionGraph.update(camera, frustum, spectator);
            }
        }

        this.indirectBuffers[Renderer.getCurrentFrame()].reset();

        this.minecraft.getProfiler().pop();
    }

    public void uploadSections() {
        if (this.sectionGrid == null)
            return;

        this.minecraft.getProfiler().push("upload");

        try {
            if (this.taskDispatcher.updateSections()) {
                this.scheduleGraphUpdate();
            }
        } catch (Exception e) {

            Initializer.LOGGER.error("Failed to upload chunk sections; resetting renderer", e);
            allChanged();
        }

        this.minecraft.getProfiler().pop();
    }

    public boolean isSectionCompiled(BlockPos blockPos) {
        RenderSection renderSection = this.sectionGrid.getSectionAtBlockPos(blockPos);
        return renderSection != null && renderSection.isCompiled();
    }

    public void allChanged() {
        if (this.level != null) {

            this.level.clearTintCaches();

            this.renderRegionCache.clear();
            this.taskDispatcher.createThreads();

            this.graphNeedsUpdate = true;

            this.renderDistance = this.minecraft.options.getEffectiveRenderDistance();
            if (this.sectionGrid != null) {
                this.sectionGrid.releaseAllBuffers();
            }

            this.shadowSections.clear();
            this.lastShadowSecX = Integer.MIN_VALUE;
            bumpGeometryVersion();

            this.taskDispatcher.clearBatchQueue();
            synchronized (this.globalBlockEntities) {
                this.globalBlockEntities.clear();
            }

            this.sectionGrid = new SectionGrid(this.level, this.renderDistance);
            this.sectionGraph = new SectionGraph(this.level, this.sectionGrid, this.taskDispatcher);

            this.onAllChangedCallbacks.forEach(Runnable::run);

            Entity entity = this.minecraft.getCameraEntity();
            if (entity != null) {
                this.sectionGrid.repositionCamera(entity.getX(), entity.getZ());
            }

        }
    }

    public void setLevel(@Nullable ClientLevel level) {
        this.lastCameraX = Float.MIN_VALUE;
        this.lastCameraY = Float.MIN_VALUE;
        this.lastCameraZ = Float.MIN_VALUE;

        this.lastCameraSectionX = Integer.MIN_VALUE;
        this.lastCameraSectionY = Integer.MIN_VALUE;
        this.lastCameraSectionZ = Integer.MIN_VALUE;

        this.level = level;
        ChunkStatusMap.createInstance(renderDistance);

        if (level != null)
            this.allChanged();
        else {
            if (this.sectionGrid != null) {
                this.sectionGrid.releaseAllBuffers();
                this.sectionGrid = null;
            }

            this.shadowSections.clear();
            this.lastShadowSecX = Integer.MIN_VALUE;

            bumpGeometryVersion();

            this.taskDispatcher.stopThreads();
            this.graphNeedsUpdate = true;
        }
    }

    public void addOnAllChangedCallback(Runnable runnable) {
        this.onAllChangedCallbacks.add(runnable);
    }

    public void clearOnAllChangedCallbacks() {
        this.onAllChangedCallbacks.clear();
    }

    public void renderSectionLayer(RenderType renderType, double camX, double camY, double camZ, Matrix4f modelView, Matrix4f projection) {
        TerrainRenderType terrainRenderType = TerrainRenderType.get(renderType);
        TerrainRenderState.prepareWorldTerrainState();
        renderType.setupRenderState();

        this.sortTranslucentSections(camX, camY, camZ);

        this.minecraft.getProfiler().push("filterempty");
        this.minecraft.getProfiler().popPush(() -> "render_" + renderType);

        final boolean isTranslucent = terrainRenderType == TerrainRenderType.TRANSLUCENT;
        final boolean indirectDraw = Initializer.CONFIG.indirectDraw && DeviceManager.supportsFastIndirectDraw();
        final long fadeNow = System.nanoTime();
        final boolean fadeSplit = !isTranslucent && RenderSection.anyFading(fadeNow)
                && !net.vulkanmod.render.sodium.SodiumShaderBridge.isActive();

        VRenderSystem.applyMVP(modelView, projection);
        VRenderSystem.setPrimitiveTopology(VertexFormat.Mode.TRIANGLES);

        Renderer renderer = Renderer.getInstance();
        RenderPipeline pipeline = pipelineManager.getPipeline(PipelineManager.ROLE_TERRAIN_MAIN, terrainRenderType);
        renderer.bindGraphicsPipeline(pipeline);

        VTextureSelector.bindShaderTextures(pipeline);
        VulkanImage normalAtlas = PbrAtlas.getBlockNormalAtlas();

        if (normalAtlas != null)
            VTextureSelector.bindTexture(4, normalAtlas);

        IndexBuffer indexBuffer = Renderer.getDrawer().getQuadsIndexBuffer().getIndexBuffer();
        Renderer.getDrawer().bindIndexBuffer(Renderer.getCommandBuffer(), indexBuffer);

        int currentFrame = Renderer.getCurrentFrame();
        Set<TerrainRenderType> allowedRenderTypes = Initializer.CONFIG.uniqueOpaqueLayer ? TerrainRenderType.COMPACT_RENDER_TYPES : TerrainRenderType.SEMI_COMPACT_RENDER_TYPES;

        if (allowedRenderTypes.contains(terrainRenderType)) {
            terrainRenderType.setCutoutUniform();

            boolean ubosBound = false;
            for (Iterator<ChunkArea> iterator = this.sectionGraph.getChunkAreaQueue().iterator(isTranslucent); iterator.hasNext(); ) {
                ChunkArea chunkArea = iterator.next();
                var queue = chunkArea.sectionQueue;
                DrawBuffers drawBuffers = chunkArea.drawBuffers;

                if (drawBuffers.getAreaBuffer(terrainRenderType) != null && queue.size() > 0) {

                    drawBuffers.bindBuffers(Renderer.getCommandBuffer(), pipeline, terrainRenderType, camX, camY, camZ);
                    if (!ubosBound) {
                        renderer.uploadAndBindUBOs(pipeline);
                        ubosBound = true;
                    }

                    if (indirectDraw)
                        chunkArea.fadePending = drawBuffers.buildDrawBatchesIndirect(indirectBuffers[currentFrame], queue, terrainRenderType, fadeSplit ? fadeNow : 0L, false);
                    else
                        chunkArea.fadePending = drawBuffers.buildDrawBatchesDirect(queue, terrainRenderType, fadeSplit ? fadeNow : 0L, false);
                }
            }

            if (fadeSplit) {
                RenderPipeline fadePipeline = pipelineManager.getPipeline(pipelineManager.ROLE_TERRAIN_FADE, terrainRenderType);
                renderer.bindGraphicsPipeline(fadePipeline);
                VTextureSelector.bindShaderTextures(fadePipeline);

                if (normalAtlas != null)
                    VTextureSelector.bindTexture(4, normalAtlas);

                boolean fadeUbosBound = false;

                for (Iterator<ChunkArea> iterator = this.sectionGraph.getChunkAreaQueue().iterator(isTranslucent); iterator.hasNext(); ) {
                    ChunkArea chunkArea = iterator.next();
                    var queue = chunkArea.sectionQueue;
                    DrawBuffers drawBuffers = chunkArea.drawBuffers;

                    if (drawBuffers.getAreaBuffer(terrainRenderType) != null && queue.size() > 0 && chunkArea.fadePending) {

                        drawBuffers.bindBuffers(Renderer.getCommandBuffer(), fadePipeline, terrainRenderType, camX, camY, camZ);
                        if (!fadeUbosBound) {
                            renderer.uploadAndBindUBOs(fadePipeline);
                            fadeUbosBound = true;
                        }

                        if (indirectDraw)
                            drawBuffers.buildDrawBatchesIndirect(indirectBuffers[currentFrame], queue, terrainRenderType, fadeNow, true);
                        else
                            drawBuffers.buildDrawBatchesDirect(queue, terrainRenderType, fadeNow, true);
                    }
                }
            }
        }

        if (terrainRenderType == TerrainRenderType.CUTOUT || terrainRenderType == TerrainRenderType.TRIPWIRE) {
            indirectBuffers[currentFrame].submitUploads();

        }

        if (!indirectDraw) {
            VRenderSystem.setChunkOffset(0, 0, 0);
            renderer.pushConstants(pipeline);
        }

        this.minecraft.getProfiler().pop();
        renderType.clearRenderState();

        VRenderSystem.applyMVP(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
    }

    public void renderMaterialTerrain(double camX, double camY, double camZ) {
        if (this.sectionGrid == null || this.sectionGrid.sections == null || this.sectionGraph == null)
            return;

        RenderPipeline pipeline = pipelineManager.getPipeline(PipelineManager.ROLE_MATERIAL, null);

        if (pipeline == null)
            return;

        Renderer renderer = Renderer.getInstance();
        VRenderSystem.setPrimitiveTopology(VertexFormat.Mode.TRIANGLES);
        IndexBuffer indexBuffer = Renderer.getDrawer().getQuadsIndexBuffer().getIndexBuffer();
        Renderer.getDrawer().bindIndexBuffer(Renderer.getCommandBuffer(), indexBuffer);

        try (RenderStateSnapshot snapshot = RenderStateSnapshot.capture()) {
            PipelineState.BlendInfo bi = PipelineState.blendInfo;

            VRenderSystem.depthTest = true;
            VRenderSystem.depthMask = true;
            VRenderSystem.depthFun = 515;
            VRenderSystem.cull = false;
            VRenderSystem.colorMask = PipelineState.ColorMask.getColorMask(true, true, true, true);
            bi.enabled = false;

            renderer.bindGraphicsPipeline(pipeline);
            VTextureSelector.bindShaderTextures(pipeline);

            final TerrainRenderType[] types = {
                    TerrainRenderType.CUTOUT_MIPPED, TerrainRenderType.CUTOUT, TerrainRenderType.TRANSLUCENT };

            for (TerrainRenderType terrainRenderType : types) {
                terrainRenderType.setCutoutUniform();
                final boolean isTranslucent = terrainRenderType == TerrainRenderType.TRANSLUCENT;

                for (Iterator<ChunkArea> iterator = this.sectionGraph.getChunkAreaQueue().iterator(isTranslucent); iterator.hasNext(); ) {
                    ChunkArea chunkArea = iterator.next();
                    DrawBuffers drawBuffers = chunkArea.drawBuffers;
                    if (drawBuffers.getAreaBuffer(terrainRenderType) == null) {
                        continue;
                    }

                    boolean bound = false;
                    for (RenderSection section : chunkArea.sectionQueue) {
                        if (!section.hasReflective()) {
                            continue;
                        }
                        if (!bound) {
                            drawBuffers.bindBuffers(Renderer.getCommandBuffer(), pipeline, terrainRenderType, camX, camY, camZ);
                            renderer.uploadAndBindUBOs(pipeline);
                            bound = true;
                        }
                        drawBuffers.drawSingleSection(section, terrainRenderType);
                    }
                }
            }

            VRenderSystem.setChunkOffset(0, 0, 0);
            renderer.pushConstants(pipeline);
        }
    }

    private static final double SHADOW_PRESPLIT_SLACK = 39.0;

    private final ArrayList<RenderSection> shadowSections = new ArrayList<>(1024);
    private final ArrayList<RenderSection>[] cascadeShadowSections = createCascadeShadowLists();
    private final float[] cascadeShadowRadius = new float[ShadowMap.CASCADES];
    private double shadowListCamX, shadowListCamZ;
    private int lastShadowSecX = Integer.MIN_VALUE, lastShadowSecY, lastShadowSecZ;
    private int lastShadowGeometryVersion = -1;

    @SuppressWarnings("unchecked")
    private static ArrayList<RenderSection>[] createCascadeShadowLists() {
        ArrayList<RenderSection>[] lists = new ArrayList[ShadowMap.CASCADES];

        for (int i = 0; i < lists.length; i++) {
            lists[i] = new java.util.ArrayList<>(1024);
        }

        return lists;
    }

    private void splitShadowCascade(int cascade, float radius) {
        ArrayList<RenderSection> list = this.cascadeShadowSections[cascade];
        list.clear();

        final double presplitRange = radius + SHADOW_PRESPLIT_SLACK;
        final double presplitRangeSq = presplitRange * presplitRange;

        for (RenderSection s : this.shadowSections) {
            double dx = (s.xOffset() + 8) - this.shadowListCamX;
            double dz = (s.zOffset() + 8) - this.shadowListCamZ;

            if (dx * dx + dz * dz > presplitRangeSq) continue;

            list.add(s);
        }

        this.cascadeShadowRadius[cascade] = radius;
    }

    private List<RenderSection> shadowSectionsForCascade(int cascade, float radius) {
        if (cascade < 0 || cascade >= this.cascadeShadowSections.length)
            return this.shadowSections;

        if (this.cascadeShadowRadius[cascade] != radius)
            splitShadowCascade(cascade, radius);

        return this.cascadeShadowSections[cascade];
    }

    private static int geometryVersion;

    public static void bumpGeometryVersion() {
        geometryVersion++;
    }

    public static int getGeometryVersion() {
        return geometryVersion;
    }

    private final StaticQueue<RenderSection> shadowScratchQueue =
            new StaticQueue<>(4096);

    public void renderShadowTerrain(double camX, double camY, double camZ, int cascade, float cascadeRadius) {
        if (this.sectionGrid == null || this.sectionGrid.sections == null)
            return;

        final double cullRangeSq = (cascadeRadius + 16.0) * (cascadeRadius + 16.0);

        int csx = (int) Math.floor(camX) >> 4;
        int csy = (int) Math.floor(camY) >> 4;
        int csz = (int) Math.floor(camZ) >> 4;

        if (csx != lastShadowSecX || csy != lastShadowSecY || csz != lastShadowSecZ
                || this.lastShadowGeometryVersion != geometryVersion) {
            this.lastShadowSecX = csx;
            this.lastShadowSecY = csy;
            this.lastShadowSecZ = csz;
            this.lastShadowGeometryVersion = geometryVersion;
            rebuildShadowSectionList(camX, camY, camZ);
        }

        if (this.shadowSections.isEmpty())
            return;

        Renderer renderer = Renderer.getInstance();
        IndexBuffer indexBuffer = Renderer.getDrawer().getQuadsIndexBuffer().getIndexBuffer();
        Renderer.getDrawer().bindIndexBuffer(Renderer.getCommandBuffer(), indexBuffer);

        final TerrainRenderType[] types = {
                TerrainRenderType.SOLID, TerrainRenderType.CUTOUT_MIPPED, TerrainRenderType.CUTOUT };

        final boolean indirectDraw = Initializer.CONFIG.indirectDraw && DeviceManager.supportsFastIndirectDraw();
        IndirectBuffer shadowIndirect = indirectDraw ? this.shadowIndirectBuffers[cascade][Renderer.getCurrentFrame()] : null;
        if (indirectDraw)
            shadowIndirect.reset();

        final java.util.List<RenderSection> cascadeSections = shadowSectionsForCascade(cascade, cascadeRadius);

        try (RenderStateSnapshot snapshot = RenderStateSnapshot.capture()) {
            for (TerrainRenderType terrainRenderType : types) {
                RenderType renderType = TerrainRenderType.getRenderType(terrainRenderType);
                renderType.setupRenderState();

                VRenderSystem.depthTest = true;
                VRenderSystem.depthMask = true;
                VRenderSystem.depthFun = 515;
                VRenderSystem.cull = (terrainRenderType == TerrainRenderType.SOLID);

                RenderPipeline pipeline = pipelineManager.getPipeline(PipelineManager.ROLE_SHADOW_TERRAIN, terrainRenderType);
                renderer.bindGraphicsPipeline(pipeline);
                VTextureSelector.bindShaderTextures(pipeline);
                terrainRenderType.setCutoutUniform();

                if (indirectDraw) {
                    ChunkArea curArea = null;

                    this.shadowScratchQueue.clear();

                    for (RenderSection s : cascadeSections) {
                        double dx = (s.xOffset() + 8) - camX;
                        double dz = (s.zOffset() + 8) - camZ;

                        if (dx * dx + dz * dz > cullRangeSq) continue;

                        ChunkArea area = s.getChunkArea();

                        if (area != curArea) {
                            flushShadowArea(curArea, terrainRenderType, pipeline, renderer, shadowIndirect, camX, camY, camZ);
                            this.shadowScratchQueue.clear();
                            curArea = area;
                        }

                        this.shadowScratchQueue.add(s);
                    }

                    flushShadowArea(curArea, terrainRenderType, pipeline, renderer, shadowIndirect, camX, camY, camZ);
                } else {
                    ChunkArea lastArea = null;
                    for (RenderSection s : cascadeSections) {
                        double dx = (s.xOffset() + 8) - camX;
                        double dz = (s.zOffset() + 8) - camZ;

                        if (dx * dx + dz * dz > cullRangeSq) continue;

                        ChunkArea area = s.getChunkArea();
                        DrawBuffers drawBuffers = area.drawBuffers;
                        if (drawBuffers.getAreaBuffer(terrainRenderType) == null) continue;

                        if (area != lastArea) {
                            drawBuffers.bindBuffers(Renderer.getCommandBuffer(), pipeline, terrainRenderType, camX, camY, camZ);
                            renderer.uploadAndBindUBOs(pipeline);
                            lastArea = area;
                        }

                        drawBuffers.drawSingleSection(s, terrainRenderType);
                    }
                }
                renderType.clearRenderState();
            }

            if (indirectDraw)
                shadowIndirect.submitUploads();

            VRenderSystem.setChunkOffset(0, 0, 0);
            renderer.pushConstants(pipelineManager.getPipeline(PipelineManager.ROLE_SHADOW_TERRAIN, TerrainRenderType.SOLID));
        }
    }

    public void renderShadowTint(int cascade, double camX, double camY, double camZ) {
        if (this.sectionGrid == null || this.sectionGrid.sections == null || this.shadowSections.isEmpty()) {
            return;
        }
        final float radius = net.vulkanmod.vulkan.pass.ShadowMap.cascadeRadius(cascade);
        final double cullRangeSq = (radius + 16.0) * (radius + 16.0);

        Renderer renderer = Renderer.getInstance();
        IndexBuffer indexBuffer = Renderer.getDrawer().getQuadsIndexBuffer().getIndexBuffer();
        Renderer.getDrawer().bindIndexBuffer(Renderer.getCommandBuffer(), indexBuffer);

        final TerrainRenderType type = TerrainRenderType.TRANSLUCENT;
        RenderType renderType = TerrainRenderType.getRenderType(type);
        renderType.setupRenderState();

        try (RenderStateSnapshot ignored = RenderStateSnapshot.capture()) {
            PipelineState.BlendInfo bi = PipelineState.blendInfo;

            VRenderSystem.depthTest = true;
            VRenderSystem.depthMask = false;
            VRenderSystem.depthFun = 515;
            VRenderSystem.cull = true;
            VRenderSystem.colorMask = PipelineState.ColorMask.getColorMask(true, true, true, true);

            bi.setMultiplyBlend();

            RenderPipeline pipeline = pipelineManager.getPipeline(PipelineManager.ROLE_SHADOW_TINT, type);
            renderer.bindGraphicsPipeline(pipeline);
            VTextureSelector.bindShaderTextures(pipeline);
            type.setCutoutUniform();

            final boolean indirectDraw = Initializer.CONFIG.indirectDraw && DeviceManager.supportsFastIndirectDraw();
            IndirectBuffer shadowIndirect = indirectDraw ? this.shadowTintIndirectBuffers[cascade][Renderer.getCurrentFrame()] : null;
            final List<RenderSection> cascadeSections = shadowSectionsForCascade(cascade, radius);

            if (indirectDraw) {
                shadowIndirect.reset();
                ChunkArea curArea = null;

                this.shadowScratchQueue.clear();

                for (RenderSection s : cascadeSections) {
                    double dx = (s.xOffset() + 8) - camX;
                    double dz = (s.zOffset() + 8) - camZ;

                    if (dx * dx + dz * dz > cullRangeSq) continue;
                    ChunkArea area = s.getChunkArea();

                    if (area != curArea) {
                        flushShadowArea(curArea, type, pipeline, renderer, shadowIndirect, camX, camY, camZ);
                        this.shadowScratchQueue.clear();
                        curArea = area;
                    }

                    this.shadowScratchQueue.add(s);
                }

                flushShadowArea(curArea, type, pipeline, renderer, shadowIndirect, camX, camY, camZ);
                shadowIndirect.submitUploads();
            } else {
                ChunkArea lastArea = null;

                for (RenderSection s : cascadeSections) {
                    double dx = (s.xOffset() + 8) - camX;
                    double dz = (s.zOffset() + 8) - camZ;

                    if (dx * dx + dz * dz > cullRangeSq) continue;

                    ChunkArea area = s.getChunkArea();
                    DrawBuffers drawBuffers = area.drawBuffers;

                    if (drawBuffers.getAreaBuffer(type) == null) continue;
                    if (area != lastArea) {
                        drawBuffers.bindBuffers(Renderer.getCommandBuffer(), pipeline, type, camX, camY, camZ);
                        renderer.uploadAndBindUBOs(pipeline);
                        lastArea = area;
                    }

                    drawBuffers.drawSingleSection(s, type);
                }
            }

            renderType.clearRenderState();

            VRenderSystem.setChunkOffset(0, 0, 0);
            renderer.pushConstants(pipeline);
        }
    }

    private void flushShadowArea(ChunkArea area, TerrainRenderType terrainRenderType, RenderPipeline pipeline,
                                 Renderer renderer, IndirectBuffer shadowIndirect, double camX, double camY, double camZ) {
        if (area == null || this.shadowScratchQueue.size() == 0)
            return;
        DrawBuffers drawBuffers = area.drawBuffers;
        if (drawBuffers.getAreaBuffer(terrainRenderType) == null)
            return;

        drawBuffers.bindBuffers(Renderer.getCommandBuffer(), pipeline, terrainRenderType, camX, camY, camZ);
        renderer.uploadAndBindUBOs(pipeline);
        drawBuffers.buildDrawBatchesIndirect(shadowIndirect, this.shadowScratchQueue, terrainRenderType);
    }

    private void rebuildShadowSectionList(double camX, double camY, double camZ) {
        final float range = ShadowMap.shadowRange();
        final float rangeSq = range * range;

        this.shadowSections.clear();

        for (RenderSection s : this.sectionGrid.sections) {
            if (s == null || !s.isCompiled() || s.isCompletelyEmpty()) continue;

            double dx = (s.xOffset() + 8) - camX;
            double dz = (s.zOffset() + 8) - camZ;

            if (dx * dx + dz * dz > rangeSq) continue;

            double dy = (s.yOffset() + 8) - camY;

            if (dy < -80.0 || dy > 80.0) continue;

            this.shadowSections.add(s);
        }
        this.shadowSections.sort(Comparator.comparingInt(a -> a.getChunkArea().index));

        this.shadowListCamX = camX;
        this.shadowListCamZ = camZ;
        for (int i = 0; i < this.cascadeShadowSections.length; i++) {
            splitShadowCascade(i, net.vulkanmod.vulkan.pass.ShadowMap.cascadeRadius(i));
        }
    }

    private void sortTranslucentSections(double camX, double camY, double camZ) {
        this.minecraft.getProfiler().push("translucent_sort");

        double d0 = camX - this.xTransparentOld;
        double d1 = camY - this.yTransparentOld;
        double d2 = camZ - this.zTransparentOld;

        if (d0 * d0 + d1 * d1 + d2 * d2 > 2.0D) {
            this.xTransparentOld = camX;
            this.yTransparentOld = camY;
            this.zTransparentOld = camZ;

            int j = 0;

            Iterator<RenderSection> iterator = this.sectionGraph.getSectionQueue().iterator(false);

            while (iterator.hasNext() && j < 15) {
                RenderSection section = iterator.next();

                section.resortTransparency(this.taskDispatcher);

                ++j;
            }
        }

        this.minecraft.getProfiler().pop();
    }

    public void renderBlockEntities(PoseStack poseStack, double camX, double camY, double camZ,
                                    Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress, float gameTime) {
        MultiBufferSource bufferSource = this.renderBuffers.bufferSource();
        VFrustum frustum = this.sectionGraph.getFrustum();
        int blockEntityDistance = Initializer.CONFIG.blockEntityDistance;
        final double blockEntityRange = blockEntityDistance >= Config.BLOCK_ENTITY_DISTANCE_UNLIMITED
                ? Double.MAX_VALUE : (double) blockEntityDistance * blockEntityDistance;

        for (RenderSection renderSection : this.sectionGraph.getBlockEntitiesSections()) {
            List<BlockEntity> list = renderSection.getCompiledSection().getBlockEntities();

            if (!list.isEmpty()) {
                for (BlockEntity blockEntity : list) {
                    BlockPos blockPos = blockEntity.getBlockPos();
                    double dx = (double) blockPos.getX() + 0.5 - camX;
                    double dy = (double) blockPos.getY() + 0.5 - camY;
                    double dz = (double) blockPos.getZ() + 0.5 - camZ;
                    double squared = dx * dx + dy * dy + dz * dz;
                    if (squared > blockEntityRange) {
                        continue;
                    }
                    if (Initializer.CONFIG.blockEntityCulling) {
                        if (squared > 9216.0) {
                            continue;
                        }

                        if (frustum != null) {
                            float x1 = (float) (blockPos.getX() - 1);
                            float y1 = (float) (blockPos.getY() - 1);
                            float z1 = (float) (blockPos.getZ() - 1);
                            float x2 = (float) (blockPos.getX() + 2);
                            float y2 = (float) (blockPos.getY() + 2);
                            float z2 = (float) (blockPos.getZ() + 2);
                            if (!frustum.testFrustum(x1, y1, z1, x2, y2, z2))
                                continue;
                        }
                    }

                    MultiBufferSource bufferSource1 = bufferSource;

                    poseStack.pushPose();
                    poseStack.translate((double) blockPos.getX() - camX, (double) blockPos.getY() - camY, (double) blockPos.getZ() - camZ);

                    SortedSet<BlockDestructionProgress> sortedset = destructionProgress.get(blockPos.asLong());

                    if (sortedset != null && !sortedset.isEmpty()) {
                        int j1 = sortedset.last().getProgress();

                        if (j1 >= 0) {
                            PoseStack.Pose pose = poseStack.last();

                            VertexConsumer vertexconsumer = new SheetedDecalTextureGenerator(this.renderBuffers.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(j1)), pose, 1.0f);

                            bufferSource1 = (renderType) -> {
                                VertexConsumer vertexConsumer2 = bufferSource.getBuffer(renderType);
                                return renderType.affectsCrumbling() ? VertexMultiConsumer.create(vertexconsumer, vertexConsumer2) : vertexConsumer2;
                            };
                        }
                    }

                    this.minecraft.getBlockEntityRenderDispatcher().render(blockEntity, gameTime, poseStack, bufferSource1);
                    poseStack.popPose();
                }
            }
        }
    }

    private MultiBufferSource.BufferSource shadowBufferSource;

    private MultiBufferSource.BufferSource shadowBufferSource() {
        if (this.shadowBufferSource == null) {
            this.shadowBufferSource = MultiBufferSource.immediate(
                    new ByteBufferBuilder(1536));
        }
        return this.shadowBufferSource;
    }

    public void renderShadowCasters(PoseStack poseStack, double camX, double camY, double camZ, float partialTick) {
        renderShadowEntities(poseStack, camX, camY, camZ, partialTick);
        renderShadowBlockEntities(poseStack, camX, camY, camZ, partialTick);
    }

    private void renderShadowEntities(PoseStack poseStack, double camX, double camY, double camZ, float partialTick) {
        if (this.level == null) {
            return;
        }
        EntityRenderDispatcher dispatcher = this.minecraft.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource src = shadowBufferSource();
        ShadowCasterBufferSource filtered = new ShadowCasterBufferSource(src);

        final float range = ShadowMap.shadowRange();
        final float rangeSq = range * range;

        for (Entity entity : this.level.entitiesForRendering()) {
            if (entity.isInvisible() || entity.isSpectator()) {
                continue;
            }

            double ex = Mth.lerp(partialTick, entity.xOld, entity.getX());
            double ey = Mth.lerp(partialTick, entity.yOld, entity.getY());
            double ez = Mth.lerp(partialTick, entity.zOld, entity.getZ());

            double dx = ex - camX;
            double dz = ez - camZ;

            if (dx * dx + dz * dz > rangeSq)
                continue;

            double dy = ey - camY;
            if (dy < -80.0 || dy > 80.0)
                continue;

            float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
            int packedLight = dispatcher.getPackedLightCoords(entity, partialTick);

            try {
                dispatcher.render(entity, ex - camX, ey - camY, ez - camZ, yRot, partialTick, poseStack, filtered, packedLight);
            } catch (Throwable ignored) {}
        }
        src.endBatch();
    }

    private void renderShadowBlockEntities(PoseStack poseStack, double camX, double camY, double camZ, float partialTick) {
        MultiBufferSource.BufferSource src = shadowBufferSource();
        ShadowCasterBufferSource filtered = new ShadowCasterBufferSource(src);

        final float range = net.vulkanmod.vulkan.pass.ShadowMap.shadowRange();
        final float rangeSq = range * range;

        for (RenderSection renderSection : this.sectionGraph.getBlockEntitiesSections()) {
            List<BlockEntity> list = renderSection.getCompiledSection().getBlockEntities();

            if (list.isEmpty()) continue;

            for (BlockEntity blockEntity : list) {
                BlockPos blockPos = blockEntity.getBlockPos();

                double dx = (double) blockPos.getX() + 0.5 - camX;
                double dz = (double) blockPos.getZ() + 0.5 - camZ;

                if (dx * dx + dz * dz > rangeSq) continue;

                double dy = (double) blockPos.getY() + 0.5 - camY;

                if (dy < -80.0 || dy > 80.0) continue;

                poseStack.pushPose();
                poseStack.translate((double) blockPos.getX() - camX, (double) blockPos.getY() - camY, (double) blockPos.getZ() - camZ);

                try {
                    this.minecraft.getBlockEntityRenderDispatcher().render(blockEntity, partialTick, poseStack, filtered);
                } catch (Throwable ignored) {}

                poseStack.popPose();
            }
        }
        src.endBatch();
    }

    private record ShadowCasterBufferSource(MultiBufferSource delegate) implements MultiBufferSource {
            private static final VertexConsumer NOOP = new NoOpVertexConsumer();

        @Override
        @NotNull
        public VertexConsumer getBuffer(RenderType renderType) {
            String full = renderType.toString();

            int lb = full.indexOf('[');
            int colon = full.indexOf(':', lb + 1);

            String type = (lb >= 0 && colon > lb) ? full.substring(lb + 1, colon) : full;

            boolean geom = type.contains("solid") || type.contains("cutout") || type.contains("translucent");
            boolean skip = type.contains("item_entity") || type.contains("eyes") || type.contains("glint")
                    || type.contains("outline") || type.contains("beacon") || type.contains("lines")
                    || type.contains("energy_swirl") || type.contains("breeze_wind") || type.contains("text")
                    || type.contains("leash") || type.contains("water_mask") || type.contains("weather");

            return (geom && !skip) ? this.delegate.getBuffer(renderType) : NOOP;
        }
    }

    private static final class NoOpVertexConsumer implements VertexConsumer {
        @Override @NotNull public VertexConsumer addVertex  (float x, float y, float z)     { return this; }
        @Override @NotNull public VertexConsumer setColor   (int r, int g, int b, int a)    { return this; }
        @Override @NotNull public VertexConsumer setUv      (float u, float v)              { return this; }
        @Override @NotNull public VertexConsumer setUv1     (int u, int v)                  { return this; }
        @Override @NotNull public VertexConsumer setUv2     (int u, int v)                  { return this; }
        @Override @NotNull public VertexConsumer setNormal  (float x, float y, float z)     { return this; }
    }

    public void scheduleGraphUpdate() {
        this.graphNeedsUpdate = true;
    }

    public boolean graphNeedsUpdate() {
        return this.graphNeedsUpdate;
    }

    public int getVisibleSectionsCount() {
        return this.sectionGraph.getSectionQueue().size();
    }

    public void setSectionDirty(int x, int y, int z, boolean flag) {
        this.sectionGrid.setDirty(x, y, z, flag);

        this.renderRegionCache.remove(x, z);
    }

    public SectionGrid getSectionGrid() {
        return this.sectionGrid;
    }

    public ChunkAreaManager getChunkAreaManager() {
        return this.sectionGrid.chunkAreaManager;
    }

    public TaskDispatcher getTaskDispatcher() {
        return taskDispatcher;
    }

    public short getLastFrame() {
        return this.sectionGraph.getLastFrame();
    }

    public int getRenderDistance() {
        return this.renderDistance;
    }

    public String getChunkStatistics() {
        return this.sectionGraph.getStatistics();
    }

    public List<String> getChunkStatisticsAsList() {
        return this.sectionGraph.getStatisticsAsList();
    }

    public void cleanUp() {
        if (indirectBuffers != null)
            Arrays.stream(indirectBuffers).forEach(Buffer::freeBuffer);

        if (shadowIndirectBuffers != null)
            for (IndirectBuffer[] perCascade : shadowIndirectBuffers)
                Arrays.stream(perCascade).forEach(Buffer::freeBuffer);

        if (shadowTintIndirectBuffers != null)
            for (IndirectBuffer[] perCascade : shadowTintIndirectBuffers)
                Arrays.stream(perCascade).forEach(Buffer::freeBuffer);
    }

}
