#version 450

layout(binding = 0) uniform UBO {
    mat4 FogInvMVPMat;
    mat4 FogShadowMVP;
    vec3 FogCameraPos;
    float FogShadowIntensity;
    vec3 FogSunDir;
    float FogShadowTexel;
    vec3 FogShadowCameraPos;
};

layout(binding = 1) uniform sampler2D Sampler0;
layout(binding = 2) uniform sampler2D Sampler1;
layout(binding = 3) uniform sampler2D Sampler2;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

const float GOLDEN = 2.39996323;

vec2 vogel(int i, int n, float r0) {
    float r = sqrt((float(i) + 0.5) / float(n));
    float theta = float(i) * GOLDEN + r0;
    return r * vec2(cos(theta), sin(theta));
}

vec3 reconstruct(vec2 uv, float depth) {
    vec4 ndc = vec4(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0, depth, 1.0);
    vec4 rel = FogInvMVPMat * ndc;
    return rel.xyz / rel.w;
}

float shadowLit(vec3 rel, vec3 N, out float interior) {
    interior = 0.0;
    vec3 shadowRel = rel + (FogCameraPos - FogShadowCameraPos);
    vec4 sc = FogShadowMVP * vec4(shadowRel, 1.0);
    if (sc.w <= 0.0) return 1.0;
    vec3 ndc = sc.xyz / sc.w;
    if (ndc.z < 0.0 || ndc.z > 1.0) return 1.0;
    vec2 uv = vec2(ndc.x * 0.5 + 0.5, 0.5 - ndc.y * 0.5);
    if (uv.x <= 0.0 || uv.x >= 1.0 || uv.y <= 0.0 || uv.y >= 1.0) return 1.0;

    float ndl = max(abs(dot(N, normalize(FogSunDir.y >= 0.0 ? FogSunDir : -FogSunDir))), 0.1);
    float bias = clamp(0.0006 / ndl, 0.0005, 0.003);
    float d = ndc.z - bias;

    float centerOccluder = texture(Sampler2, uv).r;
    float verticalGap = max(0.0, d - centerOccluder) * 240.0 * abs(FogSunDir.y);
    interior = smoothstep(22.0, 42.0, verticalGap);
    if (verticalGap >= 42.0) return 1.0;

    float searchR = 5.0 * FogShadowTexel;
    float blockerSum = 0.0, blockers = 0.0;
    for (int i = 0; i < 12; i++) {
        float occ = texture(Sampler2, uv + vogel(i, 12, 0.0) * searchR).r;
        if (occ < d) { blockerSum += occ; blockers += 1.0; }
    }
    if (blockers <= 0.0) return 1.0;

    float gap = max(0.0, d - blockerSum / blockers);
    float penumbra = clamp(gap * 260.0, 1.4, 24.0) * FogShadowTexel;

    float sum = 0.0;
    for (int i = 0; i < 32; i++) {
        vec2 o = vogel(i, 32, 0.0) * penumbra;
        sum += d > texture(Sampler2, uv + o).r ? 0.0 : 1.0;
    }
    return sum / 32.0;
}

void main() {
    vec2 dSize = vec2(textureSize(Sampler0, 0));
    ivec2 dTexel = ivec2(texCoord * dSize);
    float depth = min(texelFetch(Sampler0, dTexel, 0).r, texelFetch(Sampler1, dTexel, 0).r);
    vec2 fullUV = (vec2(dTexel) + 0.5) / dSize;

    if (depth >= 0.9999) {
        fragColor = vec4(0.0, 1.0, 0.0, 4096.0);
        return;
    }

    vec3 p = reconstruct(fullUV, depth);
    float dist = length(p);
    vec3 N = normalize(cross(dFdx(p), dFdy(p)));

    float interior = 0.0;
    float shadowTerm = 0.0;
    if (FogShadowIntensity > 0.0) {
        float lit = shadowLit(p, N, interior);
        shadowTerm = (1.0 - lit) * FogShadowIntensity;
    }

    fragColor = vec4(shadowTerm, 1.0, interior, dist);
}
