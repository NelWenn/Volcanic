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

const float AoRadius = 2.6;

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
    float penumbra = clamp(gap * 260.0, 1.2, 22.0) * FogShadowTexel;

    float sum = 0.0;
    for (int i = 0; i < 32; i++) {
        vec2 o = vogel(i, 32, 0.0) * penumbra;
        sum += d > texture(Sampler2, uv + o).r ? 0.0 : 1.0;
    }
    return sum / 32.0;
}

float ambientOcclusion(vec3 p, vec3 N, float dist) {
    vec2 fullResSize = vec2(textureSize(Sampler0, 0));
    float aspect = fullResSize.x / fullResSize.y;
    float rUV = clamp(0.6 / (dist + 1.0), 0.003, 0.014);
    vec2 radiusUV = vec2(rUV / aspect, rUV);

    float occ = 0.0;
    float samples = 0.0;
    const int N_AO = 12;
    for (int i = 0; i < N_AO; i++) {
        vec2 uv = texCoord + vogel(i, N_AO, 0.0) * radiusUV;
        float sd = min(texture(Sampler0, uv).r, texture(Sampler1, uv).r);
        vec3 sp = reconstruct(uv, sd);
        vec3 v = sp - p;
        float len = length(v);
        if (len < 0.02) continue;
        float range = 1.0 - smoothstep(AoRadius * 0.7, AoRadius * 1.5, len);
        occ += max(0.0, dot(v / len, N) - 0.1) * range;
        samples += 1.0;
    }
    occ = samples > 0.0 ? occ / samples : 0.0;
    return clamp(1.0 - occ * 2.0, 0.0, 1.0);
}

void main() {
    ivec2 fullResTexel = ivec2(gl_FragCoord.xy) * 2;
    vec2 fullResSize = vec2(textureSize(Sampler0, 0));
    float depth = min(texelFetch(Sampler0, fullResTexel, 0).r, texelFetch(Sampler1, fullResTexel, 0).r);
    vec2 fullUV = (vec2(fullResTexel) + 0.5) / fullResSize;

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

    float ao = ambientOcclusion(p, N, dist);

    fragColor = vec4(shadowTerm, ao, interior, dist);
}
