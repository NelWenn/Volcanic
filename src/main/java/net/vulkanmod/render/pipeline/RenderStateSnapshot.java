package net.vulkanmod.render.pipeline;

import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.PipelineState;

/**
 * Captures the full mutable global render state (depth/cull/stencil/blend/topology/color mask)
 * You can use it in a try block, to get a snapshot of it correctly
 * It was made primarly for readability
 * <pre>{@code
 * try (RenderStateSnapshot snapshot = RenderStateSnapshot.capture()) {
 *     // mutate VRenderSystem / PipelineState.blendInfo freely
 * } // original state is restored here even if an exception is thrown
 * }</pre>
 */
public final class RenderStateSnapshot implements AutoCloseable {
    private final boolean depthTest, depthMask, cull, stencilTest, logicOp;
    private final int depthFun, cullFace, frontFace, topology, polygonMode, colorMask;
    private final int stencilFunc, stencilRef, stencilFuncMask, stencilFailOp, stencilDepthFailOp, stencilPassOp, stencilWriteMask, logicOpFun;

    private final boolean blendEnabled;
    private final int srcRgb, dstRgb, srcAlpha, dstAlpha, blendOp, blendOpRgb, blendOpAlpha;

    private RenderStateSnapshot() {
        this.depthTest = VRenderSystem.depthTest;
        this.depthMask = VRenderSystem.depthMask;
        this.cull = VRenderSystem.cull;
        this.depthFun = VRenderSystem.depthFun;
        this.cullFace = VRenderSystem.cullFace;
        this.frontFace = VRenderSystem.frontFace;
        this.topology = VRenderSystem.topology;
        this.polygonMode = VRenderSystem.polygonMode;
        this.colorMask = VRenderSystem.colorMask;
        this.stencilTest = VRenderSystem.stencilTest;
        this.stencilFunc = VRenderSystem.stencilFunc;
        this.stencilRef = VRenderSystem.stencilRef;
        this.stencilFuncMask = VRenderSystem.stencilFuncMask;
        this.stencilFailOp = VRenderSystem.stencilFailOp;
        this.stencilDepthFailOp = VRenderSystem.stencilDepthFailOp;
        this.stencilPassOp = VRenderSystem.stencilPassOp;
        this.stencilWriteMask = VRenderSystem.stencilWriteMask;
        this.logicOp = VRenderSystem.logicOp;
        this.logicOpFun = VRenderSystem.logicOpFun;

        PipelineState.BlendInfo bi = PipelineState.blendInfo;
        this.blendEnabled = bi.enabled;
        this.srcRgb = bi.srcRgbFactor;
        this.dstRgb = bi.dstRgbFactor;
        this.srcAlpha = bi.srcAlphaFactor;
        this.dstAlpha = bi.dstAlphaFactor;
        this.blendOp = bi.blendOp;
        this.blendOpRgb = bi.blendOpRgb;
        this.blendOpAlpha = bi.blendOpAlpha;
    }

    public static RenderStateSnapshot capture() {
        return new RenderStateSnapshot();
    }

    @Override
    public void close() {
        VRenderSystem.depthTest = this.depthTest;
        VRenderSystem.depthMask = this.depthMask;
        VRenderSystem.cull = this.cull;
        VRenderSystem.depthFun = this.depthFun;
        VRenderSystem.cullFace = this.cullFace;
        VRenderSystem.frontFace = this.frontFace;
        VRenderSystem.topology = this.topology;
        VRenderSystem.polygonMode = this.polygonMode;
        VRenderSystem.colorMask = this.colorMask;
        VRenderSystem.stencilTest = this.stencilTest;
        VRenderSystem.stencilFunc = this.stencilFunc;
        VRenderSystem.stencilRef = this.stencilRef;
        VRenderSystem.stencilFuncMask = this.stencilFuncMask;
        VRenderSystem.stencilFailOp = this.stencilFailOp;
        VRenderSystem.stencilDepthFailOp = this.stencilDepthFailOp;
        VRenderSystem.stencilPassOp = this.stencilPassOp;
        VRenderSystem.stencilWriteMask = this.stencilWriteMask;
        VRenderSystem.logicOp = this.logicOp;
        VRenderSystem.logicOpFun = this.logicOpFun;

        PipelineState.BlendInfo bi = PipelineState.blendInfo;
        bi.enabled = this.blendEnabled;
        bi.srcRgbFactor = this.srcRgb;
        bi.dstRgbFactor = this.dstRgb;
        bi.srcAlphaFactor = this.srcAlpha;
        bi.dstAlphaFactor = this.dstAlpha;
        bi.blendOp = this.blendOp;
        bi.blendOpRgb = this.blendOpRgb;
        bi.blendOpAlpha = this.blendOpAlpha;
    }
}
