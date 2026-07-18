#version 450

layout(binding = 0) uniform UBO {
    mat4 FogInvProjMat;
    mat4 FogInvMVPMat;
    mat4 FogShadowMVP;
    mat4 FogPrevMVP;
    vec4 FogColor;
    vec3 FogCameraPos;
    float FogDayTime;
    vec3 FogSunDir;
    float FogDensity;
    float FogHeight;
    float FogSunVisible;
    vec2 FogSunScreenUV;
    vec3 FogPrevCameraPos;
    float FogTaaStrength;
    float FogShadowTexel;
    float FogShadowIntensity;
    vec3 FogShadowCameraPos;
    float FogGlowStrength;
    float PointLightCount;
    float PointLightStrength;
    vec4 PointLightPosR[32];
    vec4 PointLightColor[32];
    float AutoExposureEnabled;
    float ExposureStrength;
    float FrameDelta;
};

layout(binding = 1) uniform sampler2D Sampler0;
layout(binding = 2) uniform sampler2D Sampler1;
layout(binding = 3) uniform sampler2D Sampler2;
layout(binding = 4) uniform sampler2D Sampler3;
layout(binding = 5) uniform sampler2D Sampler4;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

const int GRID = 5;

void main() {
    float prevExp = texture(Sampler4, vec2(0.5)).r;
    if (prevExp <= 0.0) prevExp = 1.0;

    if (AutoExposureEnabled < 0.5) {
        fragColor = vec4(1.0, 0.0, 0.0, 0.0);
        return;
    }

    float logSum = 0.0;
    float wSum = 0.0;
    for (int y = 0; y < GRID; y++) {
        for (int x = 0; x < GRID; x++) {
            vec2 uv = (vec2(x, y) + 0.5) / float(GRID);
            vec2 d = uv - 0.5;
            float w = exp(-dot(d, d) * 6.0);
            vec3 c = texture(Sampler0, uv).rgb;
            float lum = dot(c, vec3(0.2126, 0.7152, 0.0722));
            logSum += log(max(lum, 1e-4)) * w;
            wSum += w;
        }
    }
    float avgLum = exp(logSum / max(wSum, 1e-4));

    const float keyValue = 0.35;
    float target = clamp(keyValue / max(avgLum, 1e-4), 0.85, 1.7);

    float dayFactor = clamp(FogSunDir.y * 1.2 + 0.2, 0.0, 1.0);
    target = mix(1.0, target, clamp(ExposureStrength, 0.0, 1.5) * dayFactor);

    float tau = target < prevExp ? 0.3 : 0.7;
    float rate = 1.0 - exp(-FrameDelta / max(tau, 1e-3));
    float adapted = mix(prevExp, target, clamp(rate, 0.0, 1.0));

    fragColor = vec4(adapted, 0.0, 0.0, 0.0);
}
