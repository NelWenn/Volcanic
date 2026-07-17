#version 450

// Half-res terms pass: shadow/fog/god-ray amounts into RGBA16F.
//   r = shadow term (post-TAA)   g = fog amount   b = god-ray amount   a = interiorness
// One exact full-res depth texel per half-res pixel (no depth blending) so the composite can match by depth.

layout(binding = 0) uniform UBO {
    mat4 FogInvProjMat;   // inverse world projection (kept for compatibility)
    mat4 FogInvMVPMat;    // inverse world MVP: reconstructs camera-relative world position from depth
    mat4 FogShadowMVP;    // sun view-projection: projects a camera-relative world point into shadow space
    mat4 FogPrevMVP;      // previous frame's world MVP (for TAA reprojection)
    vec4 FogColor;        // game fog colour (follows time of day / biome)
    vec3 FogCameraPos;    // camera world position
    float FogDayTime;     // 0..1 time of day
    vec3 FogSunDir;       // world-space sun direction
    float FogDensity;     // 0 .. 0.30 (UI slider)
    float FogHeight;      // world Y ceiling (UI slider): fog dense below, fades above
    float FogSunVisible;  // 1 if the sun is on-screen (enables god-rays)
    vec2 FogSunScreenUV;  // sun position in screen UV
    vec3 FogPrevCameraPos;// previous frame's camera world position (for TAA reprojection)
    float FogTaaStrength; // 0 = TAA off, ~0.88 = history blend weight
    float FogShadowTexel; // 1 / shadow-map resolution (for resolution-independent PCF)
    float FogShadowIntensity; // 0 at night-horizon .. 1 sun .. ~0.35 moon (drives shadow darkness)
    vec3 FogShadowCameraPos;  // camera pos the shadow map was rendered from (corrects camera lag/flash)
    float FogGlowStrength;    // emissive bloom around torches / lava / bright warm sources (0..2)
    float PointLightCount;    // number of active per-pixel point lights (0..32)
    float PointLightStrength; // per-pixel point light intensity (0..2, 0 = disabled)
    vec4 PointLightPosR[32];  // xyz = light position relative to FogCameraPos, w = radius (blocks)
    vec4 PointLightColor[32]; // rgb = light colour, a = intensity (emission / 15)
};

layout(binding = 1) uniform sampler2D Sampler0;  // world color (unused here; bound for layout symmetry)
layout(binding = 2) uniform sampler2D Sampler1;  // captured world depth (no first-person hand)
layout(binding = 3) uniform sampler2D Sampler2;  // foreground depth (includes the hand)
layout(binding = 4) uniform sampler2D Sampler3;  // sun shadow map (depth from the sun's POV)
layout(binding = 5) uniform sampler2D Sampler4;  // terms history (previous frame's half-res output)

float hash(vec3 p);   // defined below (noise helpers)
float noise(vec3 x);  // defined below

// Vogel-disk offsets, rotated per pixel by phi (one sin/cos per fragment).
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

// Returns (lit fraction, interiorness) via Vogel-disk PCF. Interiorness rises when the occluder is far
// above the surface (deep cave/interior) — there the projected sun shadow is released to the lightmap.
vec2 sunlight(vec3 relPos, float bias, float radiusScale) {
    // Shift into the shadow map's camera-relative space (rendered at frame start, not mid-frame).
    vec3 shadowRel = relPos + (FogCameraPos - FogShadowCameraPos);
    vec4 sc = FogShadowMVP * vec4(shadowRel, 1.0);
    if (sc.w <= 0.0) return vec2(1.0, 0.0);
    vec3 ndc = sc.xyz / sc.w;
    vec2 uv = vec2(ndc.x * 0.5 + 0.5, 0.5 - ndc.y * 0.5);   // negative-height viewport Y flip
    if (ndc.z < 0.0 || ndc.z > 1.0) return vec2(1.0, 0.0);

    float d = ndc.z - bias;

    // Occluder gap in blocks (sun ortho depth spans 240 blocks over z 0..1); big gap = interior.
    float centerOccluder = texture(Sampler3, uv).r;
    float gapBlocks = max(0.0, d - centerOccluder) * 240.0;
    float interior = smoothstep(8.0, 22.0, gapBlocks);
    if (gapBlocks >= 22.0) return vec2(1.0, 1.0);   // fully interior, skip PCF

    // Fade to lit at the shadow-box border (wide, no hard edge)
    float edge = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    if (edge <= 0.0) return vec2(1.0, interior);

    float phi = hash(vec3(gl_FragCoord.xy, 7.3)) * 6.2831853;
    float s = sin(phi), c = cos(phi);
    const int N = 16;
    float radius = 1.3 * FogShadowTexel * radiusScale;   // widened with distance

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

// Single-tap sun visibility for a point already in shadow clip space: 1 = lit, 0 = shadowed.
float shadowLitAtClip(vec4 sc) {
    if (sc.w <= 0.0) return 1.0;
    vec3 ndc = sc.xyz / sc.w;
    if (ndc.z < 0.0 || ndc.z > 1.0) return 1.0;
    vec2 uv = vec2(ndc.x * 0.5 + 0.5, 0.5 - ndc.y * 0.5);
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return 1.0;   // outside box: assume lit
    return (ndc.z - 0.0009) > texture(Sampler3, uv).r ? 0.0 : 1.0;
}

// Volumetric fog + god-rays in one ray march (shared ray). Density is a drifting Perlin field in a
// height band around the surface; Beer-Lambert gives fog coverage, shadow-tested sun gives the god-ray.
// Returns: .x = fog coverage (0..1), .y = in-scattered god-ray amount.
vec2 volumetricFogGodrays(vec3 rayDir, float maxL, float dither, bool doShadow, float dayF, float duskDawn) {
    const int STEPS = 16;
    float L = min(maxL, 130.0);
    float stepLen = L / float(STEPS);
    float t = stepLen * dither;
    vec3 drift = vec3(FogDayTime * 4.0, FogDayTime * 0.7, -FogDayTime * 3.0);

    // Time-of-day density: clear at noon, thick at dawn/dusk, moderate at night.
    float timeDensity = 0.40 + 1.00 * duskDawn + 0.45 * (1.0 - dayF);
    float densScale = FogDensity * 0.13 * timeDensity;

    // Shadow clip position is affine in t: scBase + t * scStep, so no per-step matrix multiply.
    vec4 scBase = FogShadowMVP * vec4(FogCameraPos - FogShadowCameraPos, 1.0);
    vec4 scStep = FogShadowMVP * vec4(rayDir, 0.0);

    float od = 0.0;
    float godLit = 0.0, godW = 0.0;
    for (int i = 0; i < STEPS; i++) {
        vec3 p = FogCameraPos + rayDir * t;
        // Height band: dense at the surface, wide upper fade into the sky, caves fog-free.
        float band = smoothstep(FogHeight + 100.0, FogHeight - 15.0, p.y)
                   * smoothstep(FogHeight - 60.0, FogHeight - 28.0, p.y);
        if (band <= 0.0) { t += stepLen; continue; }   // outside band, skip noise + shadow tap
        float n = noise(p * 0.022 + drift);
        od += band * (0.42 + 1.15 * n * n);
        // God-ray = average in-band sun visibility, decoupled from fog density.
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
    // One exact full-res depth texel per half-res pixel (no depth blending).
    ivec2 fullResTexel = ivec2(gl_FragCoord.xy) * 2;
    vec2 fullResSize = vec2(textureSize(Sampler1, 0));
    float depth = min(texelFetch(Sampler1, fullResTexel, 0).r, texelFetch(Sampler2, fullResTexel, 0).r);
    vec2 fullUV = (vec2(fullResTexel) + 0.5) / fullResSize;

    // Reconstruct camera-relative world position (Y flipped: negative-height viewport).
    vec4 ndc = vec4(fullUV.x * 2.0 - 1.0, 1.0 - fullUV.y * 2.0, depth, 1.0);
    vec4 rel = FogInvMVPMat * ndc;
    rel.xyz /= rel.w;

    bool isSky = depth >= 0.9999;
    float dist = length(rel.xyz);
    vec3 rayDir = rel.xyz / max(dist, 1e-4);
    float L = isSky ? 900.0 : min(dist, 1024.0);

    // Screen-space normal for the slope-scaled shadow bias. lightDir = sun by day, moon by night.
    vec3 surfN = normalize(cross(dFdx(rel.xyz), dFdy(rel.xyz)));
    vec3 lightDir = FogSunDir.y >= 0.0 ? FogSunDir : -FogSunDir;   // up-light: sun by day, moon by night
    float sunH = FogSunDir.y;
    float dayFactor = clamp(sunH * 1.2 + 0.2, 0.0, 1.0);
    float duskDawn = 1.0 - abs(sunH);
    float ndl = max(abs(dot(surfN, lightDir)), 0.08);   // abs: screen-space normal sign is ambiguous
    float shadowBias = clamp(0.0004 / ndl, 0.0004, 0.0025);

    // Distance factor: 0 near, 1 far. Far surfaces shimmer most, so soften/stabilise harder with distance.
    float df = smoothstep(25.0, 90.0, dist);

    // Shadow computed as a term (not applied) so TAA can smooth just the shadow, not the scene.
    float shadowTerm = 0.0;
    float sunInterior = 0.0;   // 1 = deep cave/interior: no projected sun shadow, no sun ground-light
    if (!isSky && FogShadowIntensity > 0.0) {
        vec2 sunVis = sunlight(rel.xyz, shadowBias * mix(1.0, 2.2, df), mix(1.0, 2.8, df));
        shadowTerm = (1.0 - sunVis.x) * FogShadowIntensity;
        sunInterior = sunVis.y;
    }

    // TAA on the shadow term only: reproject and blend the history red channel. Difference-rejection
    // kills ghosting where motion reveals/hides shadow, loosened with distance to blend far shimmer.
    if (FogTaaStrength > 0.0) {
        vec3 prevRel = rel.xyz + (FogCameraPos - FogPrevCameraPos);
        vec4 prevClip = FogPrevMVP * vec4(prevRel, 1.0);
        if (prevClip.w > 0.0001) {
            vec2 pndc = prevClip.xy / prevClip.w;
            vec2 prevUV = vec2(pndc.x * 0.5 + 0.5, 0.5 - pndc.y * 0.5);
            if (prevUV.x > 0.0 && prevUV.x < 1.0 && prevUV.y > 0.0 && prevUV.y < 1.0) {
                float histShadow = texture(Sampler4, prevUV).r;
                float diff = abs(shadowTerm - histShadow);
                float rLo = mix(0.05, 0.30, df);   // loosen rejection with distance
                float rHi = mix(0.35, 0.95, df);
                float w = min(0.97, FogTaaStrength * (1.0 + 0.10 * df)) * (1.0 - smoothstep(rLo, rHi, diff));
                shadowTerm = mix(shadowTerm, histShadow, w);
            }
        }
    }

    // Volumetric fog + god-rays (skip the march when neither fog nor shadow is on).
    bool doShadow = FogShadowIntensity > 0.0;
    vec2 vf = vec2(0.0);
    if (doShadow || FogDensity > 0.0001) {
        float dither = hash(vec3(gl_FragCoord.xy, 1.0));
        vf = volumetricFogGodrays(rayDir, L, dither, doShadow, dayFactor, duskDawn);
    }
    float godAmt = vf.y;

    // Fade the fog layer out as the camera climbs above it.
    float layerPresence = smoothstep(FogHeight + 130.0, FogHeight + 40.0, FogCameraPos.y);
    float fog = clamp(vf.x, 0.0, 1.0) * layerPresence;

    // Amounts only; red doubles as next frame's shadow TAA history.
    fragColor = vec4(shadowTerm, fog, godAmt, sunInterior);
}
