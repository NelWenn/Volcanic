#version 450

#define CSM_DEBUG 0
#define TEMPORAL_DEBUG 0

layout(binding = 0) uniform UBO {
    mat4 FogInvMVPMat;
    vec3 FogSunDir;
    float FogShadowIntensity;
};

layout(binding = 1) uniform sampler2D Sampler0;
layout(binding = 2) uniform sampler2D Sampler1;
layout(binding = 3) uniform sampler2D Sampler2;
layout(binding = 4) uniform sampler2D Sampler3;
layout(binding = 5) uniform sampler2D Sampler4;
layout(binding = 6) uniform sampler2D Sampler5;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

const float AO_STRENGTH = 0.35;
const float HIGHLIGHT = 0.28;
const vec3 MURK_COLOR = vec3(0.05, 0.16, 0.20);
const float MURK_DENSITY = 0.11;
const float MURK_STRENGTH = 0.85;

vec3 worldAt(vec2 uv, float d) {
    vec4 n = vec4(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0, d, 1.0);
    vec4 w = FogInvMVPMat * n;
    return w.xyz / w.w;
}

const vec2 VOGEL12[12] = vec2[](
    vec2(0.20412, 0.00000), vec2(-0.26070, 0.23882), vec2(0.03990, -0.45469), vec2(0.32859, 0.42859),
    vec2(-0.60301, -0.10666), vec2(0.57123, -0.36337), vec2(-0.19106, 0.71075), vec2(-0.36438, -0.70159),
    vec2(0.79056, 0.28871), vec2(-0.82244, 0.33949), vec2(0.39647, -0.84724), vec2(0.29298, 0.93407)
);

vec4 upsampleLight(float dist) {
    vec2 ht = 0.8 / vec2(textureSize(Sampler3, 0));
    vec4 sum = texture(Sampler3, texCoord);
    float wSum = 1.0;
    for (int i = 0; i < 12; i++) {
        vec4 t = texture(Sampler3, texCoord + VOGEL12[i] * ht);
        float w = 1.0 / (1.0 + 30.0 * abs(dist - t.a) / max(dist, 1.0));
        sum += t * w;
        wSum += w;
    }
    return sum / wSum;
}

void main() {
#if TEMPORAL_DEBUG
    fragColor = vec4(vec3(texture(Sampler3, texCoord).r), 1.0);
    return;
#endif
    vec3 color = texture(Sampler0, texCoord).rgb;
    float depthOpaque = texture(Sampler1, texCoord).r;
    float depthTrans = texture(Sampler2, texCoord).r;
    float depth = min(depthOpaque, depthTrans);

    vec4 ndc = vec4(texCoord.x * 2.0 - 1.0, 1.0 - texCoord.y * 2.0, depth, 1.0);
    vec4 rel = FogInvMVPMat * ndc;
    rel.xyz /= rel.w;

    bool isSky = depth >= 0.9999;
    if (isSky) {
        fragColor = vec4(color, 1.0);
        return;
    }

#if CSM_DEBUG
    {
        vec4 L = texture(Sampler3, texCoord);
        float ci = L.g;
        vec3 cc = ci < 0.5 ? vec3(1.0, 0.35, 0.35) : (ci < 1.5 ? vec3(0.35, 1.0, 0.35) : vec3(0.35, 0.35, 1.0));
        fragColor = vec4(cc * (1.0 - L.r * 0.85), 1.0);
        return;
    }
#endif

    float dist = length(rel.xyz);
    vec3 rayDir = rel.xyz / max(dist, 1e-4);
    vec3 surfN = normalize(cross(dFdx(rel.xyz), dFdy(rel.xyz)));
    vec3 nrm = dot(surfN, rayDir) > 0.0 ? -surfN : surfN;

    vec3 lightDir = FogSunDir.y >= 0.0 ? FogSunDir : -FogSunDir;
    vec3 lightDirN = normalize(lightDir + vec3(1e-4));
    vec3 lightCol = FogSunDir.y >= 0.0 ? vec3(1.0, 0.92, 0.75) : vec3(0.58, 0.68, 1.0);

    vec4 lightBuf = upsampleLight(dist);
    float shadowTerm = lightBuf.r;
    float ao = lightBuf.g;
    float interior = lightBuf.b;
    float shadowFrac = clamp(shadowTerm / max(FogShadowIntensity, 1e-3), 0.0, 1.0);

    color *= mix(vec3(1.0), vec3(0.60, 0.65, 0.80), shadowTerm);

    if (FogShadowIntensity > 0.0) {
        float ndlLight = max(dot(nrm, lightDirN), 0.0);
        float lit = (1.0 - shadowFrac) * (1.0 - interior) * ndlLight;
        color += color * lightCol * lit * FogShadowIntensity * HIGHLIGHT;
    }

    color *= 1.0 - AO_STRENGTH * (1.0 - ao);

    float dOpaqueOnly = texture(Sampler5, texCoord).r;
    float belowCamera = 1.0 - step(-0.05, rel.y);
    float underWater = step(depthOpaque + 1e-6, dOpaqueOnly) * step(depthOpaque, depthTrans + 1e-6) * belowCamera;
    if (underWater > 0.001) {
        float thickness = length(worldAt(texCoord, dOpaqueOnly)) - dist;
        float murk = 1.0 - exp(-max(thickness, 0.0) * MURK_DENSITY);
        color = mix(color, MURK_COLOR, murk * MURK_STRENGTH);
    }

    vec4 refl = texture(Sampler4, texCoord);
    color = mix(color, refl.rgb, refl.a);

    vec3 hx = max(color - vec3(0.82), vec3(0.0));
    color = min(color, vec3(0.82)) + hx / (1.0 + 4.0 * hx);

    fragColor = vec4(color, 1.0);
}
