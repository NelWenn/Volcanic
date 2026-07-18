#version 450

#define RADIANCE_PCSS 1

layout(binding = 0) uniform UBO {
    mat4 FogInvMVPMat;
    mat4 FogShadowMVP;
    mat4 FogPrevMVP;
    vec3 FogCameraPos;
    float FogShadowIntensity;
    vec3 FogSunDir;
    float FogTaaStrength;
    vec3 FogShadowCameraPos;
    float FogShadowTexel;
    vec3 FogPrevCameraPos;
    float FogTaaFrame;
};

layout(binding = 1) uniform sampler2D Sampler0;
layout(binding = 2) uniform sampler2D Sampler1;
layout(binding = 3) uniform sampler2D Sampler2;
layout(binding = 4) uniform sampler2D Sampler3;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

const vec2 VOGEL16[16] = vec2[](
    vec2(0.1767767, 0.0000000),
    vec2(-0.2257721, 0.2068259),
    vec2(0.0345579, -0.3937712),
    vec2(0.2845715, 0.3711726),
    vec2(-0.5222233, -0.0923734),
    vec2(0.4946950, -0.3146853),
    vec2(-0.1654651, 0.6155252),
    vec2(-0.3155624, -0.6075939),
    vec2(0.6846426, 0.2500290),
    vec2(-0.7122555, 0.2940104),
    vec2(0.3433528, -0.7337294),
    vec2(0.2537323, 0.8089313),
    vec2(-0.7647471, -0.4431838),
    vec2(0.8971334, -0.1972351),
    vec2(-0.5475044, 0.7787740),
    vec2(-0.1264901, -0.9760893)
);

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

vec2 sunlight(vec3 relPos, float bias, float radiusScale) {
    vec3 shadowRel = relPos + (FogCameraPos - FogShadowCameraPos);
    vec4 sc = FogShadowMVP * vec4(shadowRel, 1.0);
    if (sc.w <= 0.0) return vec2(1.0, 0.0);
    vec3 ndc = sc.xyz / sc.w;
    vec2 uv = vec2(ndc.x * 0.5 + 0.5, 0.5 - ndc.y * 0.5);
    if (ndc.z < 0.0 || ndc.z > 1.0) return vec2(1.0, 0.0);

    float d = ndc.z - bias;

    float centerOccluder = texture(Sampler2, uv).r;
    float verticalGap = max(0.0, d - centerOccluder) * 240.0 * abs(FogSunDir.y);
    float interior = smoothstep(22.0, 42.0, verticalGap);
    if (verticalGap >= 42.0) return vec2(1.0, 1.0);

    float edge = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    if (edge <= 0.0) return vec2(1.0, interior);

    float tj = FogTaaStrength > 0.0 ? FogTaaFrame * 0.6180339887 : 0.0;
    float phi = fract(hash(vec3(gl_FragCoord.xy, 7.3)) + tj) * 6.2831853;
    float s = sin(phi), c = cos(phi);
    const int N = 16;
    float minRadius = 2.0 * FogShadowTexel * radiusScale;

#if RADIANCE_PCSS
    float radius = minRadius;
    float dP = d;
    if (radiusScale < 2.2) {
        float search = 6.0 * FogShadowTexel * radiusScale;
        float blockerSum = 0.0;
        float blockers = 0.0;
        for (int i = 0; i < 8; i++) {
            vec2 v = VOGEL16[i * 2];
            vec2 o = vec2(v.x * c - v.y * s, v.x * s + v.y * c) * search;
            float occ = texture(Sampler2, uv + o).r;
            if (occ < d) { blockerSum += occ; blockers += 1.0; }
        }
        if (blockers <= 0.0) {
            return vec2(1.0, interior);
        }
        float gap = max(0.0, d - blockerSum / blockers);
        float penumbra = min(gap * 288.0, 26.0) * FogShadowTexel;
        radius = max(minRadius, penumbra);
        dP = ndc.z - bias * (1.0 + 0.08 * radius / FogShadowTexel);
    }
#else
    float radius = minRadius;
    float dP = d;
#endif

    float sum = 0.0;
    for (int i = 0; i < N; i++) {
        vec2 v = VOGEL16[i];
        vec2 o = vec2(v.x * c - v.y * s, v.x * s + v.y * c) * radius;
        sum += dP > texture(Sampler2, uv + o).r ? 0.0 : 1.0;
    }
    float lit = sum / float(N);

    float shaded = mix(1.0, lit, smoothstep(0.0, 0.12, edge));
    return vec2(mix(shaded, 1.0, interior), interior);
}

void main() {
    ivec2 fullResTexel = ivec2(gl_FragCoord.xy) * 2;
    vec2 fullResSize = vec2(textureSize(Sampler0, 0));
    float depth = min(texelFetch(Sampler0, fullResTexel, 0).r, texelFetch(Sampler1, fullResTexel, 0).r);
    vec2 fullUV = (vec2(fullResTexel) + 0.5) / fullResSize;

    if (depth >= 0.9999) {
        fragColor = vec4(0.0, 0.0, 4096.0, 1.0);
        return;
    }

    vec4 ndc = vec4(fullUV.x * 2.0 - 1.0, 1.0 - fullUV.y * 2.0, depth, 1.0);
    vec4 rel = FogInvMVPMat * ndc;
    rel.xyz /= rel.w;

    float dist = length(rel.xyz);
    vec3 surfN = normalize(cross(dFdx(rel.xyz), dFdy(rel.xyz)));
    vec3 lightDir = FogSunDir.y >= 0.0 ? FogSunDir : -FogSunDir;
    float ndl = max(abs(dot(surfN, lightDir)), 0.08);
    float shadowBias = clamp(0.0004 / ndl, 0.0004, 0.0025);

    float df = smoothstep(25.0, 90.0, dist);

    float shadowTerm = 0.0;
    float sunInterior = 0.0;
    if (FogShadowIntensity > 0.0) {
        vec2 sunVis = sunlight(rel.xyz, shadowBias * mix(1.0, 2.2, df), mix(1.0, 2.8, df));
        shadowTerm = (1.0 - sunVis.x) * FogShadowIntensity;
        sunInterior = sunVis.y;
    }

    if (FogTaaStrength > 0.0) {
        vec3 prevRel = rel.xyz + (FogCameraPos - FogPrevCameraPos);
        vec4 prevClip = FogPrevMVP * vec4(prevRel, 1.0);
        if (prevClip.w > 0.0001) {
            vec2 pndc = prevClip.xy / prevClip.w;
            vec2 prevUV = vec2(pndc.x * 0.5 + 0.5, 0.5 - pndc.y * 0.5);
            if (prevUV.x > 0.0 && prevUV.x < 1.0 && prevUV.y > 0.0 && prevUV.y < 1.0) {
                vec4 hist = texture(Sampler3, prevUV);
                if (abs(hist.b - dist) < 0.04 * dist + 0.25) {
                    float diff = abs(shadowTerm - hist.r);
                    float rLo = mix(0.10, 0.24, df);
                    float rHi = mix(0.55, 0.75, df);
                    float w = min(0.82, FogTaaStrength * (1.0 + 0.10 * df)) * (1.0 - smoothstep(rLo, rHi, diff));
                    shadowTerm = mix(shadowTerm, hist.r, w);
                }
            }
        }
    }

    fragColor = vec4(shadowTerm, sunInterior, dist, 1.0);
}
