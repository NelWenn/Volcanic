package net.vulkanmod.vulkan.shader.pipeline.definitions;

import net.vulkanmod.vulkan.shader.pipeline.Uniform;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Shared Fog UBO set */
final class FogUniformSets {
    private FogUniformSets() {
    }

    static class Core {
        Matrix4f FogInvProjMat;
        Matrix4f FogInvMVPMat;
        Matrix4f FogShadowMVP;
        Matrix4f FogPrevMVP;
        Vector4f FogColor;
        Vector3f FogCameraPos;
        float FogDayTime;
        Vector3f FogSunDir;
        float FogDensity;
        float FogHeight;
        float FogSunVisible;
        Vector2f FogSunScreenUV;
        Vector3f FogPrevCameraPos;
        float FogTaaStrength;
        float FogShadowTexel;
        float FogShadowIntensity;
        Vector3f FogShadowCameraPos;
        float FogGlowStrength;
        float PointLightCount;
        float PointLightStrength;
        @Uniform(count = 128) float[] PointLightPosR;
        @Uniform(count = 128) float[] PointLightColor;
    }

    static class Full extends Core {
        float AutoExposureEnabled;
        float ExposureStrength;
        float FrameDelta;
    }
}
