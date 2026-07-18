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

float hash(vec3 p);
float noise(vec3 x);

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

vec2 sunlight(vec3 relPos, float bias, float radiusScale) {
    vec3 shadowRel = relPos + (FogCameraPos - FogShadowCameraPos);
    vec4 sc = FogShadowMVP * vec4(shadowRel, 1.0);
    if (sc.w <= 0.0) return vec2(1.0, 0.0);
    vec3 ndc = sc.xyz / sc.w;
    vec2 uv = vec2(ndc.x * 0.5 + 0.5, 0.5 - ndc.y * 0.5);
    if (ndc.z < 0.0 || ndc.z > 1.0) return vec2(1.0, 0.0);

    float d = ndc.z - bias;

    float centerOccluder = texture(Sampler3, uv).r;
    float verticalGap = max(0.0, d - centerOccluder) * 240.0 * abs(FogSunDir.y);
    float interior = smoothstep(22.0, 42.0, verticalGap);
    if (verticalGap >= 42.0) return vec2(1.0, 1.0);

    float edge = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    if (edge <= 0.0) return vec2(1.0, interior);

    float phi = hash(vec3(gl_FragCoord.xy, 7.3)) * 6.2831853;
    float s = sin(phi), c = cos(phi);
    const int N = 16;
    float radius = 2.0 * FogShadowTexel * radiusScale;

    float sum = 0.0;
    for (int i = 0; i < N; i++) {
        vec2 v = VOGEL16[i];
        vec2 o = vec2(v.x * c - v.y * s, v.x * s + v.y * c) * radius;
        sum += d > texture(Sampler3, uv + o).r ? 0.0 : 1.0;
    }
    float lit = sum / float(N);

    float shaded = mix(1.0, lit, smoothstep(0.0, 0.12, edge));
    return vec2(mix(shaded, 1.0, interior), interior);
}

float shadowLitAtClip(vec4 sc) {
    if (sc.w <= 0.0) return 1.0;
    vec3 ndc = sc.xyz / sc.w;
    if (ndc.z < 0.0 || ndc.z > 1.0) return 1.0;
    vec2 uv = vec2(ndc.x * 0.5 + 0.5, 0.5 - ndc.y * 0.5);
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return 1.0;
    return (ndc.z - 0.0009) > texture(Sampler3, uv).r ? 0.0 : 1.0;
}

vec2 volumetricFogGodrays(vec3 rayDir, float maxL, float dither, bool doShadow, float dayF, float duskDawn) {
    const int STEPS = 16;
    float L = min(maxL, 130.0);
    float stepLen = L / float(STEPS);
    float t = stepLen * dither;
    vec3 drift = vec3(FogDayTime * 4.0, FogDayTime * 0.7, -FogDayTime * 3.0);

    float timeDensity = 0.40 + 1.00 * duskDawn + 0.45 * (1.0 - dayF);
    float densScale = FogDensity * 0.13 * timeDensity;

    vec4 scBase = FogShadowMVP * vec4(FogCameraPos - FogShadowCameraPos, 1.0);
    vec4 scStep = FogShadowMVP * vec4(rayDir, 0.0);

    float od = 0.0;
    float godLit = 0.0, godW = 0.0;
    for (int i = 0; i < STEPS; i++) {
        vec3 p = FogCameraPos + rayDir * t;
        float band = smoothstep(FogHeight + 100.0, FogHeight - 15.0, p.y)
                   * smoothstep(FogHeight - 60.0, FogHeight - 28.0, p.y);
        if (band <= 0.0) { t += stepLen; continue; }
        float n = noise(p * 0.022 + drift);
        od += band * (0.42 + 1.15 * n * n);
        float lit = doShadow ? shadowLitAtClip(scBase + t * scStep) : 1.0;
        godLit += lit * band;
        godW   += band;
        t += stepLen;
    }
    float trans = exp(-od * densScale * stepLen);
    float god = godW > 1e-4 ? godLit / godW : 0.0;
    return vec2(1.0 - trans, god);
}

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float noise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i + vec3(0,0,0)), hash(i + vec3(1,0,0)), f.x),
                   mix(hash(i + vec3(0,1,0)), hash(i + vec3(1,1,0)), f.x), f.y),
               mix(mix(hash(i + vec3(0,0,1)), hash(i + vec3(1,0,1)), f.x),
                   mix(hash(i + vec3(0,1,1)), hash(i + vec3(1,1,1)), f.x), f.y), f.z);
}

void main() {
    ivec2 fullResTexel = ivec2(gl_FragCoord.xy) * 2;
    vec2 fullResSize = vec2(textureSize(Sampler1, 0));
    float depth = min(texelFetch(Sampler1, fullResTexel, 0).r, texelFetch(Sampler2, fullResTexel, 0).r);
    vec2 fullUV = (vec2(fullResTexel) + 0.5) / fullResSize;

    vec4 ndc = vec4(fullUV.x * 2.0 - 1.0, 1.0 - fullUV.y * 2.0, depth, 1.0);
    vec4 rel = FogInvMVPMat * ndc;
    rel.xyz /= rel.w;

    bool isSky = depth >= 0.9999;
    float dist = length(rel.xyz);
    vec3 rayDir = rel.xyz / max(dist, 1e-4);
    float L = isSky ? 900.0 : min(dist, 1024.0);

    vec3 surfN = normalize(cross(dFdx(rel.xyz), dFdy(rel.xyz)));
    vec3 lightDir = FogSunDir.y >= 0.0 ? FogSunDir : -FogSunDir;
    float sunH = FogSunDir.y;
    float dayFactor = clamp(sunH * 1.2 + 0.2, 0.0, 1.0);
    float duskDawn = 1.0 - abs(sunH);
    float ndl = max(abs(dot(surfN, lightDir)), 0.08);
    float shadowBias = clamp(0.0004 / ndl, 0.0004, 0.0025);

    float df = smoothstep(25.0, 90.0, dist);

    float shadowTerm = 0.0;
    float sunInterior = 0.0;
    if (!isSky && FogShadowIntensity > 0.0) {
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
                float histShadow = texture(Sampler4, prevUV).r;
                float diff = abs(shadowTerm - histShadow);
                float rLo = mix(0.12, 0.30, df);
                float rHi = mix(0.80, 0.95, df);
                float w = min(0.97, FogTaaStrength * (1.0 + 0.10 * df)) * (1.0 - smoothstep(rLo, rHi, diff));
                shadowTerm = mix(shadowTerm, histShadow, w);
            }
        }
    }

    bool doShadow = FogShadowIntensity > 0.0;
    vec2 vf = vec2(0.0);
    if (doShadow || FogDensity > 0.0001) {
        float dither = hash(vec3(gl_FragCoord.xy, 1.0));
        vf = volumetricFogGodrays(rayDir, L, dither, doShadow, dayFactor, duskDawn);
    }
    float godAmt = vf.y;

    float layerPresence = smoothstep(FogHeight + 130.0, FogHeight + 40.0, FogCameraPos.y);
    float fog = clamp(vf.x, 0.0, 1.0) * layerPresence;

    fragColor = vec4(shadowTerm, fog, godAmt, sunInterior);
}
