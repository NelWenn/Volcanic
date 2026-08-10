package net.vulkanmod.mixin.chunk;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.render.chunk.TerrainRenderState;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.framegraph.Phase;
import net.vulkanmod.vulkan.FrameTimer;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.pass.DefaultMainPass;
import net.vulkanmod.vulkan.pass.MainPass;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.SortedSet;
import java.util.function.IntConsumer;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow @Final
    private RenderBuffers renderBuffers;

    @Shadow @Final
    private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

    @Unique
    private WorldRenderer volcanic$worldRenderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, RenderBuffers renderBuffers, CallbackInfo ci) {
        this.volcanic$worldRenderer = WorldRenderer.init(this.renderBuffers);
    }

    @Inject(method = "setLevel", at = @At("RETURN"))
    private void setLevel(ClientLevel clientLevel, CallbackInfo ci) {
        this.volcanic$worldRenderer.setLevel(clientLevel);
    }

    @Inject(method = "allChanged", at = @At("RETURN"))
    private void onAllChanged(CallbackInfo ci) {
        this.volcanic$worldRenderer.allChanged();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 1, shift = At.Shift.BEFORE))
    private void renderBlockEntities(DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        Vec3 pos = camera.getPosition();
        PoseStack poseStack = new PoseStack();

        this.volcanic$worldRenderer.renderBlockEntities(
                poseStack,
                pos.x(), pos.y(), pos.z(),
                this.destructionProgress,
                deltaTracker.getGameTimeDeltaPartialTick(false)
        );

        volcanic$renderEntityShadows(camera, deltaTracker);
    }

    @Unique
    private void volcanic$renderEntityShadows(Camera camera, DeltaTracker deltaTracker) {
        net.vulkanmod.config.Config cfg = net.vulkanmod.Initializer.CONFIG;
        if (!cfg.shadersEnabled || !cfg.isCamille() || !cfg.shadowsEnabled)
            return;

        Vec3 pos = camera.getPosition();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        PoseStack shadowPose = new PoseStack();

        Runnable casters = cfg.entityShadows
                ? () -> this.volcanic$worldRenderer.renderShadowCasters(shadowPose, pos.x(), pos.y(), pos.z(), partialTick) : null;

        IntConsumer tint = cfg.coloredShadows
                ? (int cascade) -> this.volcanic$worldRenderer.renderShadowTint(cascade,
                        VRenderSystem.shadowCamX,
                        VRenderSystem.shadowCamY,
                        VRenderSystem.shadowCamZ) : null;

        if (casters == null && tint == null)
            return;

        Renderer.getInstance().getMainPass().renderEntityShadows(casters, tint);
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void prepareLevelRenderState(DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        if (DefaultMainPass.postShaderActive()) {
            VRenderSystem.snapshotPrevFrameMatrices();
        }
        volcanic$prepareWorldPassRenderState();
    }

    @Inject(method = "setupRender", at = @At("HEAD"))
    private void setupRender(Camera camera, Frustum frustum, boolean isCapturedFrustum, boolean spectator, CallbackInfo ci) {
        FrameTimer timer = FrameTimer.instance();
        long t = timer != null ? System.nanoTime() : 0;

        this.volcanic$worldRenderer.setupRenderer(camera, frustum, isCapturedFrustum, spectator);

        if (timer != null) timer.addSetupNanos(System.nanoTime() - t);
    }

    @Inject(method = "renderSectionLayer", at = @At("HEAD"))
    private void renderSectionLayer(RenderType renderType, double camX, double camY, double camZ, Matrix4f modelView, Matrix4f projectionMatrix, CallbackInfo ci) {
        FrameTimer timer = FrameTimer.instance();
        long t = timer != null ? System.nanoTime() : 0;
        VRenderSystem.captureExternalLodViewMatrix(modelView);
        boolean postShader = DefaultMainPass.postShaderActive();

        if (renderType == RenderType.translucent()) {
            Renderer.getInstance().getMainPass().captureOpaqueDepth();
            if (postShader) {
                Renderer.getInstance().getMainPass()
                        .prepareMaterialBuffer(camX, camY, camZ, modelView, projectionMatrix);

                try (MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
                    volcanic$getMainPass().getFrameGraph().get().execute(
                            Phase.MID_RENDER,
                            Renderer.getCommandBuffer(), stack, name -> null, () -> {});
                }
            }
        }

        if (postShader)
            VRenderSystem.captureWorldViewMatrix(modelView, camX, camY, camZ);

        this.volcanic$worldRenderer.renderSectionLayer(renderType, camX, camY, camZ, modelView, projectionMatrix);
        if (timer != null) timer.addTerrainNanos(System.nanoTime() - t);
    }

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void prepareWeatherRenderState(LightTexture lightTexture, float partialTick, double camX, double camY, double camZ, CallbackInfo ci) {
        volcanic$prepareWorldPassRenderState();
        if (!net.vulkanmod.Initializer.CONFIG.weatherRendering) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSky", at = @At("HEAD"))
    private void prepareSkyRenderState(Matrix4f modelView, Matrix4f projection, float partialTick, Camera camera, boolean isFoggy, Runnable skyFogSetup, CallbackInfo ci) {
        volcanic$prepareWorldPassRenderState();
    }

    @Inject(method = "renderClouds", at = @At("HEAD"))
    private void prepareCloudRenderState(PoseStack poseStack, Matrix4f modelView, Matrix4f projection, float partialTick, double camX, double camY, double camZ, CallbackInfo ci) {
        volcanic$prepareWorldPassRenderState();
    }

    @Inject(method = "renderWorldBorder", at = @At("HEAD"))
    private void prepareWorldBorderRenderState(Camera camera, CallbackInfo ci) {
        volcanic$prepareWorldPassRenderState();
    }

    @Inject(method = "renderDebug", at = @At("HEAD"))
    private void prepareDebugRenderState(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, CallbackInfo ci) {
        volcanic$prepareWorldPassRenderState();
    }

    @Unique
    private void volcanic$prepareWorldPassRenderState() {
        TerrainRenderState.prepareWorldTerrainState();
    }

    @Unique
    private MainPass volcanic$getMainPass() {
        return Renderer.getInstance().getMainPass();
    }

    @Inject(method = "applyFrustum", at = @At("HEAD"), cancellable = true)
    private void skipVanillaSectionCollection(Frustum frustum, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "isSectionCompiled", at = @At("HEAD"), cancellable = true)
    public void isSectionCompiled(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this.volcanic$worldRenderer.isSectionCompiled(blockPos));
    }

    @Inject(method = "onChunkLoaded", at = @At("HEAD"), cancellable = true)
    public void onChunkLoaded(ChunkPos chunkPos, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
    private void setSectionDirty(int x, int y, int z, boolean flag, CallbackInfo ci) {
        this.volcanic$worldRenderer.setSectionDirty(x, y, z, flag);
    }

    @Inject(method = "getSectionStatistics", at = @At("HEAD"), cancellable = true)
    public void getSectionStatistics(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(this.volcanic$worldRenderer.getChunkStatistics());
    }

    @Inject(method = "hasRenderedAllSections", at = @At("HEAD"), cancellable = true)
    public void hasRenderedAllSections(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(!this.volcanic$worldRenderer.graphNeedsUpdate() && this.volcanic$worldRenderer.getTaskDispatcher().isIdle());
    }

    @Inject(method = "countRenderedSections", at = @At("HEAD"), cancellable = true)
    public void countRenderedSections(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.volcanic$worldRenderer.getVisibleSectionsCount());
    }

    @Redirect(method = "renderWorldBorder", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getDepthFar()F"))
    private float getRenderDistanceZFar(GameRenderer instance) {
        return instance.getRenderDistance() * 4F;
    }

}
