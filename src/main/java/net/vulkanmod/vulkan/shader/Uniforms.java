package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.vulkanmod.compat.external.ExternalRenderPathSupport;
import net.vulkanmod.compat.external.ExternalTerrainRenderBridge;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;

import java.util.function.Supplier;

public class Uniforms {

    public static Object2ReferenceOpenHashMap<String, Supplier<Integer>> vec1i_uniformMap = new Object2ReferenceOpenHashMap<>();

    public static Object2ReferenceOpenHashMap<String, Supplier<Float>> vec1f_uniformMap = new Object2ReferenceOpenHashMap<>();
    public static Object2ReferenceOpenHashMap<String, Supplier<MappedBuffer>> vec2f_uniformMap = new Object2ReferenceOpenHashMap<>();
    public static Object2ReferenceOpenHashMap<String, Supplier<MappedBuffer>> vec3f_uniformMap = new Object2ReferenceOpenHashMap<>();
    public static Object2ReferenceOpenHashMap<String, Supplier<MappedBuffer>> vec4f_uniformMap = new Object2ReferenceOpenHashMap<>();

    public static Object2ReferenceOpenHashMap<String, Supplier<MappedBuffer>> mat4f_uniformMap = new Object2ReferenceOpenHashMap<>();

    public static Object2ReferenceOpenHashMap<String, Supplier<MappedBuffer>> floatArr_uniformMap = new Object2ReferenceOpenHashMap<>();

    public static void setupDefaultUniforms() {

        mat4f_uniformMap.put("ModelViewMat", VRenderSystem::getModelViewMatrix);
        mat4f_uniformMap.put("ProjMat", VRenderSystem::getProjectionMatrix);
        mat4f_uniformMap.put("MVP", VRenderSystem::getMVP);
        mat4f_uniformMap.put("TextureMat", VRenderSystem::getTextureMatrix);
        if (ExternalRenderPathSupport.isExternalLodBridgeEnabled()) {
            mat4f_uniformMap.put("ExternalLodCombinedMatrix", ExternalTerrainRenderBridge::getCombinedMatrix);
        }

        vec1i_uniformMap.put("EndPortalLayers", () -> 15);
        vec1i_uniformMap.put("FogShape", () -> RenderSystem.getShaderFogShape().getIndex());

        vec1f_uniformMap.put("FogStart", RenderSystem::getShaderFogStart);
        vec1f_uniformMap.put("FogEnd", RenderSystem::getShaderFogEnd);
        vec1f_uniformMap.put("LineWidth", RenderSystem::getShaderLineWidth);
        vec1f_uniformMap.put("GameTime", RenderSystem::getShaderGameTime);
        vec1f_uniformMap.put("SunAngle", () -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) return 0.0f;
            return VRenderSystem.smoothTimeOfDay(mc) * (float) (Math.PI * 2.0);
        });
        vec1f_uniformMap.put("GlintAlpha", RenderSystem::getShaderGlintAlpha);
        vec1f_uniformMap.put("AlphaCutout", () -> VRenderSystem.alphaCutout);
        vec1f_uniformMap.put("PbrDebug", () -> net.vulkanmod.Initializer.CONFIG.pbrDebugNormals ? 1.0f : 0.0f);
        vec1f_uniformMap.put("CamilleActive", () -> {
            var cfg = net.vulkanmod.Initializer.CONFIG;
            return cfg.shadersEnabled && cfg.isCamille() ? 1.0f : 0.0f;
        });

        mat4f_uniformMap.put("FogInvMVPMat", VRenderSystem::getCapturedInverseMVP);
        mat4f_uniformMap.put("FogMVPMat", VRenderSystem::getCapturedMVP);
        mat4f_uniformMap.put("FogPrevMVP", VRenderSystem::getCapturedPrevMVP);
        mat4f_uniformMap.put("FogShadowMVP0", VRenderSystem::getCapturedShadowMVP0);
        mat4f_uniformMap.put("FogShadowMVP1", VRenderSystem::getCapturedShadowMVP1);
        mat4f_uniformMap.put("FogShadowMVP2", VRenderSystem::getCapturedShadowMVP2);
        vec3f_uniformMap.put("FogShadowSplits", VRenderSystem::getCapturedShadowSplits);
        vec1f_uniformMap.put("FogShadowResolution", () -> (float) net.vulkanmod.vulkan.pass.ShadowMap.cascadeResolution());
        vec3f_uniformMap.put("FogShadowCameraPos", VRenderSystem::getCapturedShadowCameraPos);
        vec1f_uniformMap.put("FogShadowIntensity", VRenderSystem::getCapturedShadowIntensity);
        vec3f_uniformMap.put("FogCameraPos", VRenderSystem::getCapturedCameraPos);
        vec3f_uniformMap.put("FogPrevCameraPos", VRenderSystem::getCapturedPrevCameraPos);
        vec3f_uniformMap.put("FogSunDir", VRenderSystem::getCapturedSunDir);

        vec1f_uniformMap.put("AaMode", () -> (float) net.vulkanmod.Initializer.CONFIG.aaMode);
        vec1f_uniformMap.put("FogColoredShadows", () -> net.vulkanmod.Initializer.CONFIG.coloredShadows ? 1.0f : 0.0f);

        vec1f_uniformMap.put("WindTime", VRenderSystem::getWindTime);
        vec1f_uniformMap.put("WindStrength", () -> {
            var cfg = net.vulkanmod.Initializer.CONFIG;
            return cfg.windEnabled && cfg.shadersEnabled && cfg.isCamille() ? cfg.windStrength : 0.0f;
        });
        vec3f_uniformMap.put("CameraWorldPos", VRenderSystem::getCapturedCameraPos);

        vec2f_uniformMap.put("ScreenSize", VRenderSystem::getScreenSize);

        vec3f_uniformMap.put("Light0_Direction", () -> VRenderSystem.lightDirection0);
        vec3f_uniformMap.put("Light1_Direction", () -> VRenderSystem.lightDirection1);
        vec3f_uniformMap.put("ChunkOffset", () -> VRenderSystem.ChunkOffset);

        vec4f_uniformMap.put("ColorModulator", VRenderSystem::getShaderColor);
        vec4f_uniformMap.put("FogColor", VRenderSystem::getShaderFogColor);
        if (ExternalRenderPathSupport.isExternalLodBridgeEnabled()) {
            vec4f_uniformMap.put("ExternalLodModelOffsetAndYOffset", ExternalTerrainRenderBridge::getModelOffsetAndYOffset);
            vec4f_uniformMap.put("ExternalLodRenderParams", ExternalTerrainRenderBridge::getRenderParams);
        }

    }
}
