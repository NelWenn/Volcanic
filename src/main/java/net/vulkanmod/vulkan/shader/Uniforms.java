package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.vulkanmod.compat.external.ExternalRenderPathSupport;
import net.vulkanmod.compat.external.ExternalTerrainRenderBridge;
import net.vulkanmod.render.PointLights;
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
        vec1f_uniformMap.put("GlintAlpha", RenderSystem::getShaderGlintAlpha);
        vec1f_uniformMap.put("AlphaCutout", () -> VRenderSystem.alphaCutout);

        vec1f_uniformMap.put("CgExposure", () -> net.vulkanmod.Initializer.CONFIG.cgExposure);
        vec1f_uniformMap.put("CgContrast", () -> net.vulkanmod.Initializer.CONFIG.cgContrast);
        vec1f_uniformMap.put("CgSaturation", () -> net.vulkanmod.Initializer.CONFIG.cgSaturation);
        vec1f_uniformMap.put("CgTemperature", () -> net.vulkanmod.Initializer.CONFIG.cgTemperature);
        vec1f_uniformMap.put("FogDensity", () -> net.vulkanmod.Initializer.CONFIG.fogDensity);
        vec1f_uniformMap.put("FogHeight", () -> net.vulkanmod.Initializer.CONFIG.fogHeight);
        vec1f_uniformMap.put("FogDayTime", VRenderSystem::getCapturedDayTime);
        mat4f_uniformMap.put("FogInvProjMat", VRenderSystem::getCapturedInverseProjectionMatrix);
        mat4f_uniformMap.put("FogInvMVPMat", VRenderSystem::getCapturedInverseMVP);
        mat4f_uniformMap.put("FogShadowMVP", VRenderSystem::getCapturedShadowMVP);
        mat4f_uniformMap.put("FogShadowInvMVP", VRenderSystem::getCapturedShadowInvMVP);
        mat4f_uniformMap.put("FogRsmMVP", VRenderSystem::getCapturedRsmMVP);
        mat4f_uniformMap.put("FogRsmInvMVP", VRenderSystem::getCapturedRsmInvMVP);
        vec3f_uniformMap.put("FogRsmCameraPos", VRenderSystem::getCapturedRsmCameraPos);
        vec3f_uniformMap.put("FogShadowCameraPos", VRenderSystem::getCapturedShadowCameraPos);
        vec1f_uniformMap.put("FogGlowStrength", () -> net.vulkanmod.Initializer.CONFIG.glowStrength);
        mat4f_uniformMap.put("FogPrevMVP", VRenderSystem::getPrevMVPForward);
        vec3f_uniformMap.put("FogPrevCameraPos", VRenderSystem::getPrevCameraPos);
        vec1f_uniformMap.put("FogTaaStrength", () -> net.vulkanmod.Initializer.CONFIG.taaEnabled ? 0.88f : 0.0f);
        vec1f_uniformMap.put("FogTaaFrame", VRenderSystem::getTaaFrame);
        vec1f_uniformMap.put("FogShadowTexel", () -> 1.0f / net.vulkanmod.vulkan.pass.ShadowMap.currentResolution());
        vec1f_uniformMap.put("FogShadowIntensity", VRenderSystem::getCapturedShadowIntensity);
        vec3f_uniformMap.put("FogCameraPos", VRenderSystem::getCapturedCameraPos);
        vec3f_uniformMap.put("FogSunDir", VRenderSystem::getCapturedSunDir);
        vec2f_uniformMap.put("FogSunScreenUV", VRenderSystem::getCapturedSunScreenUV);
        vec1f_uniformMap.put("FogSunVisible", VRenderSystem::getCapturedSunVisible);

        vec1f_uniformMap.put("AutoExposureEnabled", () -> net.vulkanmod.Initializer.CONFIG.autoExposure ? 1.0f : 0.0f);
        vec1f_uniformMap.put("ExposureStrength", () -> net.vulkanmod.Initializer.CONFIG.exposureStrength);
        vec1f_uniformMap.put("FrameDelta", VRenderSystem::getFrameDelta);
        vec1f_uniformMap.put("FogColoredShadows", () -> net.vulkanmod.Initializer.CONFIG.coloredShadows ? 1.0f : 0.0f);

        vec1f_uniformMap.put("PointLightCount", PointLights::getCount);
        vec1f_uniformMap.put("PointLightStrength", () -> net.vulkanmod.Initializer.CONFIG.pointLightStrength
                * (net.vulkanmod.Initializer.CONFIG.pointLightsEnabled ? 1.0f : 0.0f));
        floatArr_uniformMap.put("PointLightPosR", PointLights::getPosRadiusBuffer);
        floatArr_uniformMap.put("PointLightColor", PointLights::getColorBuffer);

        vec1f_uniformMap.put("HeldLightLevel", PointLights::getHeldLightLevel);

        vec1f_uniformMap.put("WindTime", VRenderSystem::getWindTime);
        vec1f_uniformMap.put("WindStrength", () -> net.vulkanmod.Initializer.CONFIG.windEnabled
                ? net.vulkanmod.Initializer.CONFIG.windStrength : 0.0f);
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
