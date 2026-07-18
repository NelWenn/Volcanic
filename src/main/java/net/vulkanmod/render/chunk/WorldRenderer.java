package net.vulkanmod.render.chunk;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.PipelineManager;
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
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

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
    private IndirectBuffer[] shadowIndirectBuffers;

    public RenderRegionBuilder renderRegionCache;

    private final List<Runnable> onAllChangedCallbacks = new ObjectArrayList<>();

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
    }

    private void allocateIndirectBuffers() {
        if (this.indirectBuffers != null)
            Arrays.stream(this.indirectBuffers).forEach(Buffer::freeBuffer);

        this.indirectBuffers = new IndirectBuffer[Renderer.getFramesNum()];

        for (int i = 0; i < this.indirectBuffers.length; ++i) {
            this.indirectBuffers[i] = new IndirectBuffer(1000000, MemoryTypes.HOST_MEM);

        }

        // Shadow pass runs before the main render; can't share its indirectBuffers (main render reset()s them)
        if (this.shadowIndirectBuffers != null)
            Arrays.stream(this.shadowIndirectBuffers).forEach(Buffer::freeBuffer);

        this.shadowIndirectBuffers = new IndirectBuffer[Renderer.getFramesNum()];

        for (int i = 0; i < this.shadowIndirectBuffers.length; ++i) {
            this.shadowIndirectBuffers[i] = new IndirectBuffer(1000000, MemoryTypes.HOST_MEM);

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

        if (this.sectionGrid == null) {
            return;
        }

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

            // grid rebuilt: drop cached shadow casters and bump version so they rebuild
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
        if (level != null) {
            this.allChanged();
        } else {
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

        VRenderSystem.applyMVP(modelView, projection);
        VRenderSystem.setPrimitiveTopologyGL(GL11.GL_TRIANGLES);

        Renderer renderer = Renderer.getInstance();
        GraphicsPipeline pipeline = PipelineManager.getTerrainShader(terrainRenderType);
        renderer.bindGraphicsPipeline(pipeline);

        VTextureSelector.bindShaderTextures(pipeline);

        IndexBuffer indexBuffer = Renderer.getDrawer().getQuadsIndexBuffer().getIndexBuffer();
        Renderer.getDrawer().bindIndexBuffer(Renderer.getCommandBuffer(), indexBuffer);

        int currentFrame = Renderer.getCurrentFrame();
        Set<TerrainRenderType> allowedRenderTypes = Initializer.CONFIG.uniqueOpaqueLayer ? TerrainRenderType.COMPACT_RENDER_TYPES : TerrainRenderType.SEMI_COMPACT_RENDER_TYPES;
        if (allowedRenderTypes.contains(terrainRenderType)) {
            terrainRenderType.setCutoutUniform();

            for (Iterator<ChunkArea> iterator = this.sectionGraph.getChunkAreaQueue().iterator(isTranslucent); iterator.hasNext(); ) {
                ChunkArea chunkArea = iterator.next();
                var queue = chunkArea.sectionQueue;
                DrawBuffers drawBuffers = chunkArea.drawBuffers;

                if (drawBuffers.getAreaBuffer(terrainRenderType) != null && queue.size() > 0) {

                    drawBuffers.bindBuffers(Renderer.getCommandBuffer(), pipeline, terrainRenderType, camX, camY, camZ);
                    renderer.uploadAndBindUBOs(pipeline);

                    if (indirectDraw)
                        drawBuffers.buildDrawBatchesIndirect(indirectBuffers[currentFrame], queue, terrainRenderType);
                    else
                        drawBuffers.buildDrawBatchesDirect(queue, terrainRenderType);
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

    private final java.util.ArrayList<RenderSection> shadowSections = new java.util.ArrayList<>(1024);
    private int lastShadowSecX = Integer.MIN_VALUE, lastShadowSecY, lastShadowSecZ;
    private int lastShadowGeometryVersion = -1;

    // bumped when compiled geometry reaches the GPU or the grid is rebuilt
    private static int geometryVersion;

    public static void bumpGeometryVersion() {
        geometryVersion++;
    }

    public static int getGeometryVersion() {
        return geometryVersion;
    }
    private final net.vulkanmod.render.chunk.util.StaticQueue<RenderSection> shadowScratchQueue =
            new net.vulkanmod.render.chunk.util.StaticQueue<>(4096);

    public void renderShadowTerrain(double camX, double camY, double camZ) {
        if (this.sectionGrid == null || this.sectionGrid.sections == null) {
            return;
        }

        // rebuild the caster list only on camera-section change or geometry version bump
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
        if (this.shadowSections.isEmpty()) {
            return;
        }

        Renderer renderer = Renderer.getInstance();
        IndexBuffer indexBuffer = Renderer.getDrawer().getQuadsIndexBuffer().getIndexBuffer();
        Renderer.getDrawer().bindIndexBuffer(Renderer.getCommandBuffer(), indexBuffer);

        // snapshot the full pipeline-selecting state; restored at the end so the GUI picks the right pipeline
        final boolean sDepthTest = VRenderSystem.depthTest, sDepthMask = VRenderSystem.depthMask, sCull = VRenderSystem.cull;
        final int sDepthFun = VRenderSystem.depthFun, sCullFace = VRenderSystem.cullFace, sFrontFace = VRenderSystem.frontFace;
        final int sTopology = VRenderSystem.topology, sPolygonMode = VRenderSystem.polygonMode, sColorMask = VRenderSystem.colorMask;
        final boolean sStencilTest = VRenderSystem.stencilTest, sLogicOp = VRenderSystem.logicOp;
        final int sStencilFunc = VRenderSystem.stencilFunc, sStencilRef = VRenderSystem.stencilRef, sStencilFuncMask = VRenderSystem.stencilFuncMask;
        final int sStencilFail = VRenderSystem.stencilFailOp, sStencilDepthFail = VRenderSystem.stencilDepthFailOp;
        final int sStencilPass = VRenderSystem.stencilPassOp, sStencilWriteMask = VRenderSystem.stencilWriteMask, sLogicOpFun = VRenderSystem.logicOpFun;
        final net.vulkanmod.vulkan.shader.PipelineState.BlendInfo bi = net.vulkanmod.vulkan.shader.PipelineState.blendInfo;
        final boolean sBlendEnabled = bi.enabled;
        final int sSrcRgb = bi.srcRgbFactor, sDstRgb = bi.dstRgbFactor, sSrcA = bi.srcAlphaFactor, sDstA = bi.dstAlphaFactor;
        final int sBlendOp = bi.blendOp, sBlendOpRgb = bi.blendOpRgb, sBlendOpAlpha = bi.blendOpAlpha;

        final TerrainRenderType[] types = {
                TerrainRenderType.SOLID, TerrainRenderType.CUTOUT_MIPPED, TerrainRenderType.CUTOUT };

        final boolean indirectDraw = Initializer.CONFIG.indirectDraw && DeviceManager.supportsFastIndirectDraw();
        IndirectBuffer shadowIndirect = indirectDraw ? this.shadowIndirectBuffers[Renderer.getCurrentFrame()] : null;
        if (indirectDraw)
            shadowIndirect.reset();

        for (TerrainRenderType terrainRenderType : types) {
            RenderType renderType = TerrainRenderType.getRenderType(terrainRenderType);
            renderType.setupRenderState();

            // depth test/write on; cull SOLID only (foliage quads are double-sided)
            net.vulkanmod.vulkan.VRenderSystem.depthTest = true;
            net.vulkanmod.vulkan.VRenderSystem.depthMask = true;
            net.vulkanmod.vulkan.VRenderSystem.depthFun = 515;   // GL_LEQUAL
            net.vulkanmod.vulkan.VRenderSystem.cull = (terrainRenderType == TerrainRenderType.SOLID);

            GraphicsPipeline pipeline = PipelineManager.getShadowTerrainShader(terrainRenderType);
            renderer.bindGraphicsPipeline(pipeline);
            VTextureSelector.bindShaderTextures(pipeline);
            terrainRenderType.setCutoutUniform();

            if (indirectDraw) {
                // one indirect draw per area; shadowSections is sorted by area index so areas are contiguous
                ChunkArea curArea = null;
                this.shadowScratchQueue.clear();
                for (RenderSection s : this.shadowSections) {
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
                for (RenderSection s : this.shadowSections) {
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
        renderer.pushConstants(PipelineManager.getShadowTerrainShader(TerrainRenderType.SOLID));

        VRenderSystem.depthTest = sDepthTest; VRenderSystem.depthMask = sDepthMask; VRenderSystem.depthFun = sDepthFun;
        VRenderSystem.cull = sCull; VRenderSystem.cullFace = sCullFace; VRenderSystem.frontFace = sFrontFace;
        VRenderSystem.topology = sTopology; VRenderSystem.polygonMode = sPolygonMode; VRenderSystem.colorMask = sColorMask;
        VRenderSystem.stencilTest = sStencilTest; VRenderSystem.stencilFunc = sStencilFunc; VRenderSystem.stencilRef = sStencilRef;
        VRenderSystem.stencilFuncMask = sStencilFuncMask; VRenderSystem.stencilFailOp = sStencilFail;
        VRenderSystem.stencilDepthFailOp = sStencilDepthFail; VRenderSystem.stencilPassOp = sStencilPass;
        VRenderSystem.stencilWriteMask = sStencilWriteMask; VRenderSystem.logicOp = sLogicOp; VRenderSystem.logicOpFun = sLogicOpFun;
        bi.enabled = sBlendEnabled; bi.srcRgbFactor = sSrcRgb; bi.dstRgbFactor = sDstRgb;
        bi.srcAlphaFactor = sSrcA; bi.dstAlphaFactor = sDstA;
        bi.blendOp = sBlendOp; bi.blendOpRgb = sBlendOpRgb; bi.blendOpAlpha = sBlendOpAlpha;
    }

    private void flushShadowArea(ChunkArea area, TerrainRenderType terrainRenderType, GraphicsPipeline pipeline,
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
        final float range = net.vulkanmod.vulkan.pass.ShadowMap.shadowRange();
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
        this.shadowSections.sort((a, b) -> Integer.compare(a.getChunkArea().index, b.getChunkArea().index));
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

        for (RenderSection renderSection : this.sectionGraph.getBlockEntitiesSections()) {
            List<BlockEntity> list = renderSection.getCompiledSection().getBlockEntities();
            if (!list.isEmpty()) {
                for (BlockEntity blockEntity : list) {
                    BlockPos blockPos = blockEntity.getBlockPos();
                    if (Initializer.CONFIG.blockEntityCulling) {
                        double dx = (double) blockPos.getX() + 0.5 - camX;
                        double dy = (double) blockPos.getY() + 0.5 - camY;
                        double dz = (double) blockPos.getZ() + 0.5 - camZ;
                        if (dx * dx + dy * dy + dz * dz > 9216.0) {
                            continue;
                        }
                        if (frustum != null) {
                            float x1 = (float) (blockPos.getX() - 1);
                            float y1 = (float) (blockPos.getY() - 1);
                            float z1 = (float) (blockPos.getZ() - 1);
                            float x2 = (float) (blockPos.getX() + 2);
                            float y2 = (float) (blockPos.getY() + 2);
                            float z2 = (float) (blockPos.getZ() + 2);
                            if (!frustum.testFrustum(x1, y1, z1, x2, y2, z2)) {
                                continue;
                            }
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

    public void cleanUp() {
        if (indirectBuffers != null)
            Arrays.stream(indirectBuffers).forEach(Buffer::freeBuffer);
        if (shadowIndirectBuffers != null)
            Arrays.stream(shadowIndirectBuffers).forEach(Buffer::freeBuffer);
    }

}
