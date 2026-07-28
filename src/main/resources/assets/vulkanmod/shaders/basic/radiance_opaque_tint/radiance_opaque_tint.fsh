#version 450

layout(binding = 0) uniform UBO {
    mat4 FogInvMVPMat;
    mat4 FogShadowMVP0;
    mat4 FogShadowMVP1;
    mat4 FogShadowMVP2;
    vec3 FogCameraPos;
    float FogColoredShadows;
    vec3 FogShadowCameraPos;
    vec3 FogShadowSplits;
};

layout(binding = 1) uniform sampler2D Sampler0;
layout(binding = 2) uniform sampler2D Sampler1;
layout(binding = 3) uniform sampler2D Sampler2;
layout(binding = 4) uniform sampler2D Sampler3;
layout(binding = 5) uniform sampler2D Sampler4;
layout(binding = 6) uniform sampler2D Sampler5;
layout(binding = 7) uniform sampler2D Sampler6;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

const float STRENGTH = 0.6;

vec3 reconstruct(vec2 uv, float depth) {
    vec4 ndc = vec4(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0, depth, 1.0);
    vec4 rel = FogInvMVPMat * ndc;
    return rel.xyz / rel.w;
}

vec3 sampleTint(vec3 p, mat4 mvp, sampler2D tex, sampler2D depthTex) {
    vec3 shadowRel = p + (FogCameraPos - FogShadowCameraPos);
    vec4 sc = mvp * vec4(shadowRel, 1.0);
    if (sc.w <= 0.0) return vec3(1.0);
    vec3 ndc = sc.xyz / sc.w;
    if (ndc.z < 0.0 || ndc.z > 1.0) return vec3(1.0);
    vec2 suv = vec2(ndc.x * 0.5 + 0.5, 0.5 - ndc.y * 0.5);
    if (any(lessThan(suv, vec2(0.02))) || any(greaterThan(suv, vec2(0.98)))) return vec3(1.0);
    float rd = ndc.z + 0.0008;
    vec2 ts = 2.4 / vec2(textureSize(tex, 0));
    vec3 acc = vec3(0.0);
    float wsum = 0.0;
    for (int x = -2; x <= 2; x++) {
        for (int y = -2; y <= 2; y++) {
            vec2 uv = suv + vec2(float(x), float(y)) * ts;
            float w = exp(-0.20 * float(x * x + y * y));
            vec3 t = rd > texture(depthTex, uv).r ? texture(tex, uv).rgb : vec3(1.0);
            acc += t * w;
            wsum += w;
        }
    }
    return acc / wsum;
}

void main() {
    vec3 tint = vec3(1.0);
    float depth = texture(Sampler0, texCoord).r;
    if (FogColoredShadows > 0.5 && depth < 0.9999) {
        vec3 p = reconstruct(texCoord, depth);
        float dist = length(p);
        vec3 glass = vec3(1.0);
        if (dist < FogShadowSplits.x) {
            glass = sampleTint(p, FogShadowMVP0, Sampler1, Sampler4);
        } else if (dist < FogShadowSplits.y) {
            glass = sampleTint(p, FogShadowMVP1, Sampler2, Sampler5);
        } else if (dist < FogShadowSplits.z) {
            glass = sampleTint(p, FogShadowMVP2, Sampler3, Sampler6);
        }
        tint = mix(vec3(1.0), glass, STRENGTH);
    }
    fragColor = vec4(tint, 1.0);
}
