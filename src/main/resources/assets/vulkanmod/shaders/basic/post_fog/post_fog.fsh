#version 450

// Volumetric height fog + screen-space god-rays

layout(binding = 0) uniform UBO {
    mat4 FogInvProjMat;   // unused, kept for compatibility
    mat4 FogInvMVPMat;    // reconstructs camera-relative world pos from depth
    mat4 FogShadowMVP;    // sun view-projection into shadow space
    mat4 FogPrevMVP;      // previous frame world MVP
    vec4 FogColor;
    vec3 FogCameraPos;
    float FogDayTime;     // 0..1
    vec3 FogSunDir;
    float FogDensity;     // 0 .. 0.30
    float FogHeight;      // world Y: dense below, fades above
    float FogSunVisible;  // 1 if sun on-screen
    vec2 FogSunScreenUV;
    vec3 FogPrevCameraPos;
    float FogTaaStrength; // 0 = off
    float FogShadowTexel; // 1 / shadow-map resolution
    float FogShadowIntensity; // 0 night-horizon .. 1 sun .. ~0.35 moon
    vec3 FogShadowCameraPos;  // camera pos the shadow map was rendered from
    float FogGlowStrength;    // 0..2
    float PointLightCount;    // 0..32
    float PointLightStrength; // 0..2, 0 = disabled
    vec4 PointLightPosR[32];  // xyz relative to FogCameraPos, w = radius
    vec4 PointLightColor[32]; // rgb colour, a = intensity
};

layout(binding = 1) uniform sampler2D Sampler0;  // world color
layout(binding = 2) uniform sampler2D Sampler1;  // world depth (no hand)
layout(binding = 3) uniform sampler2D Sampler2;  // foreground depth (with hand)
layout(binding = 4) uniform sampler2D Sampler3;  // sun shadow map
layout(binding = 5) uniform sampler2D Sampler4;  // TAA history

float hash(vec3 p);
float noise(vec3 x);

// Vogel-disk offsets, precomputed unrotated; rotated per pixel by one sin/cos.
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
const vec2 VOGEL12[12] = vec2[](
    vec2(0.2041241, 0.0000000),
    vec2(-0.2606992, 0.2388219),
    vec2(0.0399040, -0.4546878),
    vec2(0.3285948, 0.4285932),
    vec2(-0.6030115, -0.1066637),
    vec2(0.5712246, -0.3633673),
    vec2(-0.1910626, 0.7107473),
    vec2(-0.3643801, -0.7015890),
    vec2(0.7905572, 0.2887086),
    vec2(-0.8224418, 0.3394940),
    vec2(0.3964697, -0.8472377),
    vec2(0.2929848, 0.9340735)
);

// Vogel-disk PCF sun visibility. Returns (lit fraction, interiorness);
// interiorness releases the shadow deep under terrain (caves) so the lightmap owns those.
vec2 sunlight(vec3 relPos, float bias, float radiusScale) {
    // shadow map is relative to FogShadowCameraPos, relPos to FogCameraPos: shift to match
    vec3 shadowRel = relPos + (FogCameraPos - FogShadowCameraPos);
    vec4 sc = FogShadowMVP * vec4(shadowRel, 1.0);
    if (sc.w <= 0.0) return vec2(1.0, 0.0);
    vec3 ndc = sc.xyz / sc.w;
    vec2 uv = vec2(ndc.x * 0.5 + 0.5, 0.5 - ndc.y * 0.5);   // Y flip
    if (ndc.z < 0.0 || ndc.z > 1.0) return vec2(1.0, 0.0);

    float d = ndc.z - bias;

    // occluder gap in blocks (sun ortho z spans 240 blocks); small = cast shadow, huge = interior
    float centerOccluder = texture(Sampler3, uv).r;
    float gapBlocks = max(0.0, d - centerOccluder) * 240.0;
    float interior = smoothstep(8.0, 22.0, gapBlocks);
    if (gapBlocks >= 22.0) return vec2(1.0, 1.0);

    // fade to lit at the shadow-box border; skip PCF past it
    float edge = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
    if (edge <= 0.0) return vec2(1.0, interior);

    float phi = hash(vec3(gl_FragCoord.xy, 7.3)) * 6.2831853;
    float s = sin(phi), c = cos(phi);
    const int N = 16;
    // penumbra widened with distance so far surfaces don't shimmer
    float radius = 1.3 * FogShadowTexel * radiusScale;

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

// single-tap sun visibility for a point already in shadow clip space (1 = lit)
float shadowLitAtClip(vec4 sc) {
    if (sc.w <= 0.0) return 1.0;
    vec3 ndc = sc.xyz / sc.w;
    if (ndc.z < 0.0 || ndc.z > 1.0) return 1.0;
    vec2 uv = vec2(ndc.x * 0.5 + 0.5, 0.5 - ndc.y * 0.5);
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return 1.0;   // outside box: lit
    return (ndc.z - 0.0009) > texture(Sampler3, uv).r ? 0.0 : 1.0;
}

// Volumetric fog + god-rays in one shared ray march.
// Returns .x = fog coverage (0..1), .y = in-scattered god-ray amount.
vec2 volumetricFogGodrays(vec3 rayDir, float maxL, float dither, bool doShadow, float dayF, float duskDawn) {
    const int STEPS = 16;
    float L = min(maxL, 130.0);
    float stepLen = L / float(STEPS);
    float t = stepLen * dither;
    vec3 drift = vec3(FogDayTime * 4.0, FogDayTime * 0.7, -FogDayTime * 3.0);

    // clear at noon, thick at dawn/dusk, moderate at night
    float timeDensity = 0.40 + 1.00 * duskDawn + 0.45 * (1.0 - dayF);
    float densScale = FogDensity * 0.13 * timeDensity;   // constant along ray, hoisted out of loop

    // shadow clip pos is affine in t: scBase + t * scStep, so no per-step matrix multiply
    vec4 scBase = FogShadowMVP * vec4(FogCameraPos - FogShadowCameraPos, 1.0);
    vec4 scStep = FogShadowMVP * vec4(rayDir, 0.0);

    float od = 0.0;
    float godLit = 0.0, godW = 0.0;
    for (int i = 0; i < STEPS; i++) {
        vec3 p = FogCameraPos + rayDir * t;
        // dense around surface; wide upper fade into sky, lower fade keeps caves clear
        float band = smoothstep(FogHeight + 100.0, FogHeight - 15.0, p.y)
                   * smoothstep(FogHeight - 60.0, FogHeight - 28.0, p.y);
        if (band <= 0.0) { t += stepLen; continue; }
        float n = noise(p * 0.022 + drift);
        od += band * (0.42 + 1.15 * n * n);
        // god-ray decoupled from density so shafts survive light fog
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

const float K = 0.12;  // height falloff rate

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

float fbm(vec3 p) {
    float f = 0.0, a = 0.5;
    for (int i = 0; i < 3; i++) { f += a * noise(p); p = p * 2.4 + 7.0; a *= 0.5; }
    return f;
}

// closed-form optical depth of an exp(-K*(y-H)) fog layer along a ray
float heightFogOpticalDepth(float y0, float dirY, float L) {
    float a = exp(-K * clamp(y0 - FogHeight, -60.0, 60.0));
    float ky = K * dirY;
    if (abs(ky) < 1e-4) return a * L;
    return a * (1.0 - exp(-clamp(ky * L, -60.0, 60.0))) / ky;
}

void main() {
    vec3 color = texture(Sampler0, texCoord).rgb;
    float depth = min(texture(Sampler1, texCoord).r, texture(Sampler2, texCoord).r);

    // reconstruct camera-relative world pos (Y flipped)
    vec4 ndc = vec4(texCoord.x * 2.0 - 1.0, 1.0 - texCoord.y * 2.0, depth, 1.0);
    vec4 rel = FogInvMVPMat * ndc;
    rel.xyz /= rel.w;

    bool isSky = depth >= 0.9999;
    float dist = length(rel.xyz);
    vec3 rayDir = rel.xyz / max(dist, 1e-4);
    float L = isSky ? 900.0 : min(dist, 1024.0);

    // screen-space normal for slope-scaled shadow bias; lightDir = sun by day, moon by night
    vec3 surfN = normalize(cross(dFdx(rel.xyz), dFdy(rel.xyz)));
    vec3 lightDir = FogSunDir.y >= 0.0 ? FogSunDir : -FogSunDir;
    vec3 lightDirN = normalize(lightDir + vec3(1e-4));
    vec3 lightCol = FogSunDir.y >= 0.0 ? vec3(1.0, 0.92, 0.75) : vec3(0.58, 0.68, 1.0);   // warm day, cool night
    float sunH = FogSunDir.y;
    float dayFactor = clamp(sunH * 1.2 + 0.2, 0.0, 1.0);
    float duskDawn = 1.0 - abs(sunH);
    // abs(): screen-space normal sign is ambiguous
    float ndl = max(abs(dot(surfN, lightDir)), 0.08);
    float shadowBias = clamp(0.0004 / ndl, 0.0004, 0.0025);

    // 0 near camera .. 1 far; far surfaces get softened / stabilised harder
    float df = smoothstep(25.0, 90.0, dist);

    // computed as a term first so TAA can smooth just the shadow
    float shadowTerm = 0.0;
    float sunInterior = 0.0;   // 1 = cave/interior
    if (!isSky && FogShadowIntensity > 0.0) {
        vec2 sunVis = sunlight(rel.xyz, shadowBias * mix(1.0, 2.2, df), mix(1.0, 2.8, df));
        shadowTerm = (1.0 - sunVis.x) * FogShadowIntensity;
        sunInterior = sunVis.y;
    }

    // TAA on the shadow term only (stored in history alpha); rejection loosens with distance
    if (FogTaaStrength > 0.0) {
        vec3 prevRel = rel.xyz + (FogCameraPos - FogPrevCameraPos);
        vec4 prevClip = FogPrevMVP * vec4(prevRel, 1.0);
        if (prevClip.w > 0.0001) {
            vec2 pndc = prevClip.xy / prevClip.w;
            vec2 prevUV = vec2(pndc.x * 0.5 + 0.5, 0.5 - pndc.y * 0.5);
            if (prevUV.x > 0.0 && prevUV.x < 1.0 && prevUV.y > 0.0 && prevUV.y < 1.0) {
                float histShadow = texture(Sampler4, prevUV).a;
                float diff = abs(shadowTerm - histShadow);
                float rLo = mix(0.05, 0.30, df);       // loosen rejection with distance
                float rHi = mix(0.35, 0.95, df);
                float w = min(0.97, FogTaaStrength * (1.0 + 0.10 * df)) * (1.0 - smoothstep(rLo, rHi, diff));
                shadowTerm = mix(shadowTerm, histShadow, w);
            }
        }
    }

    // shadowed areas keep a cool ambient fill, not black
    color *= mix(vec3(1.0), vec3(0.60, 0.65, 0.80), shadowTerm);

    // camera-facing normal (screen-space sign is ambiguous)
    vec3 nrm = dot(surfN, rayDir) > 0.0 ? -surfN : surfN;

    // warm directional light on lit surfaces facing the sun
    if (!isSky && FogShadowIntensity > 0.0) {
        float ndlLight = max(dot(nrm, lightDirN), 0.0);
        // pronounced at golden hour, neutral at noon
        float hiTime = mix(0.16, 0.42, duskDawn);
        color += color * lightCol * (1.0 - shadowTerm) * (1.0 - sunInterior) * ndlLight * FogShadowIntensity * hiTime;
    }

    // per-pixel point lights, max-blended not summed (strongest source wins)
    if (!isSky && PointLightStrength > 0.001 && PointLightCount > 0.5) {
        vec3 addLight = vec3(0.0);
        int cnt = int(PointLightCount + 0.5);
        for (int i = 0; i < 32; i++) {
            if (i >= cnt) break;
            vec3 toL = PointLightPosR[i].xyz - rel.xyz;
            float radius = PointLightPosR[i].w;
            float d2 = dot(toL, toL);
            if (d2 > radius * radius) continue;
            float invD = inversesqrt(max(d2, 1e-6));
            float d = d2 * invD;
            float x = 1.0 - d / radius;
            float att = x * x / (1.0 + d2 * 0.08);             // radial falloff
            float ndl = clamp(dot(nrm, toL * invD) * 0.6 + 0.4, 0.0, 1.0);   // wrapped diffuse
            addLight = max(addLight, PointLightColor[i].rgb * (att * ndl * PointLightColor[i].a));
        }
        addLight = min(addLight, vec3(1.1));

        // pull hue toward neutral so it doesn't oversaturate
        float aLum = dot(addLight, vec3(0.35, 0.45, 0.20));
        addLight = mix(vec3(aLum), addLight, 0.62);

        float dayGate = mix(1.0, 0.4, dayFactor);
        addLight *= PointLightStrength * dayGate;

        // hybrid: additive half relights near-black pixels, multiplicative half keeps lit areas natural
        // (albedo gain capped so dark pixels don't wash to grey)
        float sceneLum = dot(color, vec3(0.299, 0.587, 0.114));
        vec3 albedoApprox = color / max(sceneLum, 0.09);
        color = color * (1.0 + addLight * 0.55) + albedoApprox * addLight * 0.33;
    }

    // volumetric fog + god-rays (one shared march)
    float cosT = dot(rayDir, lightDirN);
    float cosP = max(cosT, 0.0);       // Mie forward-scatter lobe, shared by both phase functions
    float cos2 = cosP * cosP;
    float cos6 = cos2 * cos2 * cos2;

    // skip the march when neither shadows nor fog are on
    bool doShadow = FogShadowIntensity > 0.0;
    vec2 vf = vec2(0.0);
    if (doShadow || FogDensity > 0.0001) {
        float dither = hash(vec3(gl_FragCoord.xy, 1.0));
        vf = volumetricFogGodrays(rayDir, L, dither, doShadow, dayFactor, duskDawn);
    }
    float godAmt = vf.y;

    // fade the whole layer out as the camera climbs above it
    float layerPresence = smoothstep(FogHeight + 130.0, FogHeight + 40.0, FogCameraPos.y);
    float fog = clamp(vf.x, 0.0, 1.0) * layerPresence;

    // game fog colour nudged cooler, brightened toward the sun
    float phaseFog = 0.40 + 0.60 * cos6;
    vec3 fogCol = mix(FogColor.rgb, vec3(0.74, 0.78, 0.82), 0.12) * (0.92 + 0.08 * phaseFog * dayFactor);
    color = mix(color, fogCol, fog);

    // god-rays; FogShadowIntensity scales sun vs moon, no dayFactor gate so moonlit shafts stay faint
    if (FogShadowIntensity > 0.0) {
        float phaseGR = 0.25 + 0.75 * cos6;
        // scheduled like the fog: subtle at noon, strong at golden hour
        float grTime = 0.45 + 0.55 * duskDawn;
        float strength = godAmt * phaseGR * FogShadowIntensity * (0.45 + 0.55 * fog) * layerPresence * grTime * 1.25;
        color += lightCol * min(strength, 0.45);
    }

    // emissive glow: 12 warmth-weighted disk taps bleed a halo from bright warm pixels
    if (FogGlowStrength > 0.001) {
        float phiG = hash(vec3(gl_FragCoord.xy, 9.7)) * 6.2831853;
        float sG = sin(phiG), cG = cos(phiG);
        vec3 glow = vec3(0.0);
        const int GN = 12;
        for (int i = 0; i < GN; i++) {
            vec2 v = VOGEL12[i];
            // rotate first, then aspect scale (they don't commute)
            vec2 o = vec2(v.x * cG - v.y * sG, v.x * sG + v.y * cG) * vec2(0.5625, 1.0) * 0.035;   // round on 16:9
            vec3 c = texture(Sampler0, clamp(texCoord + o, 0.0, 1.0)).rgb;
            float lum = dot(c, vec3(0.299, 0.587, 0.114));
            float warm = 0.45 + 1.10 * clamp((c.r - c.b) * 1.5, 0.0, 1.0);
            glow += c * max(lum - 0.52, 0.0) * warm;
        }
        glow /= float(GN);

        // self-emissive boost so bright warm source pixels read as light
        float selfLum = dot(color, vec3(0.299, 0.587, 0.114));
        float selfWarm = clamp((color.r - color.b) * 1.5, 0.0, 1.0);
        vec3 self = color * max(selfLum - 0.62, 0.0) * selfWarm;

        float glowGate = mix(1.0, 0.22, dayFactor);   // mostly a night/dusk effect
        color += (glow * 2.4 + self * 0.5) * FogGlowStrength * glowGate * (1.0 - fog * 0.6);
    }

    // sun glare: bright core + halo around the sun, gated by open sky (aspect-corrected round)
    if (FogSunVisible > 0.01 && dayFactor > 0.05) {
        vec2 d = (texCoord - FogSunScreenUV) * vec2(1.78, 1.0);
        float sunD = length(d);
        float occl = smoothstep(0.9990, 0.9999, texture(Sampler1, clamp(FogSunScreenUV, 0.0, 1.0)).r);
        float core = smoothstep(0.04, 0.0, sunD);          // bright disc
        float glare = exp(-sunD * 11.0);                   // halo
        vec3 sunWarm = vec3(1.0, 0.95, 0.82);
        color += sunWarm * FogSunVisible * dayFactor * occl * (core * 1.5 + glare * 0.25);
    }

    // filmic highlight roll-off so stacked highlights don't clip to flat white
    vec3 hx = max(color - vec3(0.78), vec3(0.0));
    color = min(color, vec3(0.78)) + hx / (1.0 + 4.6 * hx);

    // alpha carries the shadow term into the history for next frame's TAA
    fragColor = vec4(color, shadowTerm);
}
