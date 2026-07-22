#version 450

#define CSM_DEBUG 0
#define TEMPORAL_DEBUG 0

layout(binding = 0) uniform UBO {
    mat4 FogInvMVPMat;
    mat4 FogShadowMVP0;
    mat4 FogShadowMVP1;
    mat4 FogShadowMVP2;
    vec3 FogCameraPos;
    float FogShadowIntensity;
    vec3 FogSunDir;
    float FogShadowResolution;
    vec3 FogShadowCameraPos;
    vec3 FogShadowSplits;
    mat4 FogPrevMVP;
    vec3 FogPrevCameraPos;
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

const float BLEND = 0.82;

const vec2 VOGEL12[12] = vec2[](
    vec2(0.20412, 0.00000), vec2(-0.26070, 0.23882), vec2(0.03990, -0.45469), vec2(0.32859, 0.42859),
    vec2(-0.60301, -0.10666), vec2(0.57123, -0.36337), vec2(-0.19106, 0.71075), vec2(-0.36438, -0.70159),
    vec2(0.79056, 0.28871), vec2(-0.82244, 0.33949), vec2(0.39647, -0.84724), vec2(0.29298, 0.93407)
);

const vec2 VOGEL32[32] = vec2[](
    vec2(0.12500, 0.00000), vec2(-0.15965, 0.14625), vec2(0.02444, -0.27844), vec2(0.20122, 0.26246),
    vec2(-0.36927, -0.06532), vec2(0.34980, -0.22252), vec2(-0.11700, 0.43524), vec2(-0.22314, -0.42963),
    vec2(0.48412, 0.17680), vec2(-0.50364, 0.20790), vec2(0.24279, -0.51882), vec2(0.17941, 0.57200),
    vec2(-0.54076, -0.31338), vec2(0.63437, -0.13946), vec2(-0.38715, 0.55068), vec2(-0.08944, -0.69020),
    vec2(0.54907, 0.46276), vec2(-0.73888, 0.03055), vec2(0.53896, -0.53633), vec2(-0.03606, 0.77979),
    vec2(-0.51282, -0.61453), vec2(0.81236, 0.10930), vec2(-0.68831, 0.47891), vec2(0.18809, -0.83606),
    vec2(0.43503, 0.75919), vec2(-0.85045, -0.27132), vec2(0.82610, -0.38168), vec2(-0.35789, 0.85516),
    vec2(-0.31941, -0.88803), vec2(0.84991, 0.44669), vec2(-0.94403, 0.24884), vec2(0.53660, -0.83453)
);

vec3 reconstruct(vec2 uv, float depth) {
    vec4 ndc = vec4(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0, depth, 1.0);
    vec4 rel = FogInvMVPMat * ndc;
    return rel.xyz / rel.w;
}

float cascadeLit(vec3 rel, vec3 N, sampler2D tex, mat4 mvp, float texel, float worldTexel, out float interior) {
    interior = 0.0;
    vec3 lightDir = normalize(FogSunDir.y >= 0.0 ? FogSunDir : -FogSunDir);
    vec3 No = dot(N, lightDir) < 0.0 ? -N : N;
    float ndl = max(dot(No, lightDir), 0.05);
    vec3 shadowRel = rel + (FogCameraPos - FogShadowCameraPos);
    shadowRel += No * worldTexel * (1.5 + 3.0 * (1.0 - ndl));
    vec4 sc = mvp * vec4(shadowRel, 1.0);
    if (sc.w <= 0.0) return 1.0;
    vec3 ndc = sc.xyz / sc.w;
    if (ndc.z < 0.0 || ndc.z > 1.0) return 1.0;
    vec2 uv = vec2(ndc.x * 0.5 + 0.5, 0.5 - ndc.y * 0.5);
    if (uv.x <= 0.0 || uv.x >= 1.0 || uv.y <= 0.0 || uv.y >= 1.0) return 1.0;

    float bias = clamp(0.0003 / ndl, 0.00012, 0.0010);
    float d = ndc.z - bias;

    float centerOccluder = texture(tex, uv).r;
    float verticalGap = max(0.0, d - centerOccluder) * 240.0 * abs(FogSunDir.y);
    interior = smoothstep(22.0, 42.0, verticalGap);
    if (verticalGap >= 42.0) return 1.0;

    float searchR = 5.0 * texel;
    float blockerSum = 0.0, blockers = 0.0;
    for (int i = 0; i < 12; i++) {
        float occ = texture(tex, uv + VOGEL12[i] * searchR).r;
        if (occ < d) { blockerSum += occ; blockers += 1.0; }
    }
    if (blockers <= 0.0) return 1.0;

    float gap = max(0.0, d - blockerSum / blockers);
    float penumbra = clamp(gap * 260.0, 1.4, 24.0) * texel;

    float sum = 0.0;
    for (int i = 0; i < 32; i++) {
        vec2 o = VOGEL32[i] * penumbra;
        sum += d > texture(tex, uv + o).r ? 0.0 : 1.0;
    }
    return sum / 32.0;
}

float sampleCascades(vec3 rel, vec3 N, float dist, out float interior, out float cascadeId) {
    float res = FogShadowResolution;
    float texel = 1.0 / res;
    float wt0 = 2.0 * FogShadowSplits.x / res;
    float wt1 = 2.0 * FogShadowSplits.y / res;
    float wt2 = 2.0 * FogShadowSplits.z / res;
    float s0 = FogShadowSplits.x, s1 = FogShadowSplits.y;

    float lit;
    cascadeId = dist < s0 ? 0.0 : (dist < s1 ? 1.0 : 2.0);
    if (dist < s0) {
        lit = cascadeLit(rel, N, Sampler2, FogShadowMVP0, texel, wt0, interior);
        float bs = s0 * BLEND;
        if (dist > bs) {
            float i1;
            float l1 = cascadeLit(rel, N, Sampler3, FogShadowMVP1, texel, wt1, i1);
            float f = (dist - bs) / (s0 - bs);
            lit = mix(lit, l1, f);
            interior = mix(interior, i1, f);
        }
    } else if (dist < s1) {
        lit = cascadeLit(rel, N, Sampler3, FogShadowMVP1, texel, wt1, interior);
        float bs = s1 * BLEND;
        if (dist > bs) {
            float i2;
            float l2 = cascadeLit(rel, N, Sampler4, FogShadowMVP2, texel, wt2, i2);
            float f = (dist - bs) / (s1 - bs);
            lit = mix(lit, l2, f);
            interior = mix(interior, i2, f);
        }
    } else {
        lit = cascadeLit(rel, N, Sampler4, FogShadowMVP2, texel, wt2, interior);
    }
    return lit;
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
    vec4 gn = texture(Sampler6, fullUV);
    vec3 N = gn.w > 0.5 ? normalize(gn.xyz) : normalize(cross(dFdx(p), dFdy(p)));

    float interior = 0.0;
    float cascadeId = 0.0;
    float shadowTerm = 0.0;
    if (FogShadowIntensity > 0.0) {
        float lit = sampleCascades(p, N, dist, interior, cascadeId);
        shadowTerm = (1.0 - lit) * FogShadowIntensity;
    }

    vec3 prevRel = p + (FogCameraPos - FogPrevCameraPos);
    vec4 pc = FogPrevMVP * vec4(prevRel, 1.0);
#if TEMPORAL_DEBUG
    vec2 dUV = pc.w > 0.0 ? vec2(pc.x / pc.w * 0.5 + 0.5, 0.5 - pc.y / pc.w * 0.5) : fullUV;
    fragColor = vec4(clamp(length(dUV - fullUV) * 25.0, 0.0, 1.0), 1.0, interior, dist);
    return;
#endif
    if (pc.w > 0.0) {
        vec2 pndc = pc.xy / pc.w;
        vec2 pUV = vec2(pndc.x * 0.5 + 0.5, 0.5 - pndc.y * 0.5);
        if (all(greaterThan(pUV, vec2(0.001))) && all(lessThan(pUV, vec2(0.999)))) {
            vec4 hist = texture(Sampler5, pUV);
            float expectedPrevDist = length(prevRel);
            float distDiff = abs(hist.a - expectedPrevDist) / max(expectedPrevDist, 1.0);
            float valid = 1.0 - smoothstep(0.04, 0.10, distDiff);
            float velFade = 1.0 - smoothstep(0.0015, 0.02, length(pUV - fullUV));
            shadowTerm = mix(shadowTerm, hist.r, 0.82 * valid * velFade);
        }
    }

#if CSM_DEBUG
    fragColor = vec4(shadowTerm, cascadeId, interior, dist);
#else
    fragColor = vec4(shadowTerm, 1.0, interior, dist);
#endif
}
