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
    float FogColoredShadows;
};

layout(binding = 1) uniform sampler2D Sampler0;
layout(binding = 2) uniform sampler2D Sampler1;
layout(binding = 3) uniform sampler2D Sampler2;
layout(binding = 4) uniform sampler2D Sampler3;
layout(binding = 5) uniform sampler2D Sampler4;
layout(binding = 6) uniform sampler2D Sampler5;
layout(binding = 7) uniform sampler2D Sampler6;

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

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

vec4 upsampleTerms(float depth) {
    vec2 halfSize = vec2(textureSize(Sampler3, 0));
    vec2 hPos = texCoord * halfSize - 0.5;
    ivec2 h0 = ivec2(floor(hPos));
    vec2 f = hPos - vec2(h0);
    ivec2 hMax = ivec2(halfSize) - 1;

    vec4 sum = vec4(0.0);
    float wSum = 0.0;
    float bestW = -1.0;
    vec4 best = vec4(0.0);
    for (int i = 0; i < 4; i++) {
        ivec2 o = ivec2(i & 1, i >> 1);
        ivec2 h = clamp(h0 + o, ivec2(0), hMax);
        float wBilin = (o.x == 1 ? f.x : 1.0 - f.x) * (o.y == 1 ? f.y : 1.0 - f.y);
        float refDepth = texelFetch(Sampler1, h * 2, 0).r;
        float w = wBilin / (1e-5 + abs(depth - refDepth));
        vec4 t = texelFetch(Sampler3, h, 0);
        sum += t * w;
        wSum += w;
        if (w > bestW) { bestW = w; best = t; }
    }
    return wSum < 1e-3 ? best : sum / wSum;
}

void main() {
    vec3 color = texture(Sampler0, texCoord).rgb;
    float depth = min(texture(Sampler1, texCoord).r, texture(Sampler2, texCoord).r);

    // Y flipped: negative-height viewport
    vec4 ndc = vec4(texCoord.x * 2.0 - 1.0, 1.0 - texCoord.y * 2.0, depth, 1.0);
    vec4 rel = FogInvMVPMat * ndc;
    rel.xyz /= rel.w;

    bool isSky = depth >= 0.9999;
    float dist = length(rel.xyz);
    vec3 rayDir = rel.xyz / max(dist, 1e-4);

    vec3 surfN = normalize(cross(dFdx(rel.xyz), dFdy(rel.xyz)));
    vec3 lightDir = FogSunDir.y >= 0.0 ? FogSunDir : -FogSunDir;
    vec3 lightDirN = normalize(lightDir + vec3(1e-4));
    vec3 lightCol = FogSunDir.y >= 0.0 ? vec3(1.0, 0.92, 0.75) : vec3(0.58, 0.68, 1.0);
    float sunH = FogSunDir.y;
    float dayFactor = clamp(sunH * 1.2 + 0.2, 0.0, 1.0);
    float duskDawn = 1.0 - abs(sunH);

    vec4 terms = upsampleTerms(depth);
    float shadowTerm = terms.r;
    float fog = terms.g;
    float godAmt = terms.b;
    float sunInterior = terms.a;

    // stained-glass tint of the sun light at this pixel's shadow-map texel (white = no glass)
    // colour of the glass occluder that cast this pixel's shadow (white = opaque occluder / no glass)
    vec3 glassTint = vec3(1.0);
    if (FogColoredShadows > 0.5 && FogShadowIntensity > 0.0) {
        vec3 shadowRel = rel.xyz + (FogCameraPos - FogShadowCameraPos);
        vec4 sc = FogShadowMVP * vec4(shadowRel, 1.0);
        if (sc.w > 0.0) {
            vec2 suv = vec2(sc.x * 0.5 + 0.5, 0.5 - sc.y * 0.5);
            if (all(greaterThanEqual(suv, vec2(0.0))) && all(lessThanEqual(suv, vec2(1.0))))
                glassTint = texture(Sampler6, suv).rgb;
        }
    }
    // toggle on -> keep the colour; off -> classic uncoloured (grey) dim
    float glassLum = dot(glassTint, vec3(0.299, 0.587, 0.114));
    vec3 appliedTint = (FogColoredShadows > 0.5) ? glassTint : vec3(glassLum);
    float glassAmt = clamp(length(vec3(1.0) - glassTint) * 1.2, 0.0, 1.0);

    color *= mix(vec3(1.0), vec3(0.60, 0.65, 0.80), shadowTerm);

    vec3 nrm = dot(surfN, rayDir) > 0.0 ? -surfN : surfN;

    if (!isSky && FogShadowIntensity > 0.0) {
        float ndlLight = max(dot(nrm, lightDirN), 0.0);
        float hiTime = mix(0.16, 0.42, duskDawn);
        float litFactor = (1.0 - shadowTerm) * (1.0 - sunInterior) * ndlLight;
        color = mix(color, color * appliedTint, litFactor * glassAmt * 0.5);
        color += color * lightCol * appliedTint * litFactor * FogShadowIntensity * hiTime;
    }

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
            float att = x * x / (1.0 + d2 * 0.08);
            float ndl = clamp(dot(nrm, toL * invD) * 0.6 + 0.4, 0.0, 1.0);
            addLight = max(addLight, PointLightColor[i].rgb * (att * ndl * PointLightColor[i].a));
        }
        addLight = min(addLight, vec3(1.1));

        float aLum = dot(addLight, vec3(0.35, 0.45, 0.20));
        addLight = mix(vec3(aLum), addLight, 0.62);

        float dayGate = mix(1.0, 0.4, dayFactor);
        addLight *= PointLightStrength * dayGate;

        float sceneLum = dot(color, vec3(0.299, 0.587, 0.114));
        vec3 albedoApprox = color / max(sceneLum, 0.09);
        color = color * (1.0 + addLight * 0.55) + albedoApprox * addLight * 0.33;
    }

    float cosT = dot(rayDir, lightDirN);
    float cosP = max(cosT, 0.0);
    float cos2 = cosP * cosP;
    float cos6 = cos2 * cos2 * cos2;

    float layerPresence = smoothstep(FogHeight + 130.0, FogHeight + 40.0, FogCameraPos.y);

    float phaseFog = 0.40 + 0.60 * cos6;
    vec3 fogCol = mix(FogColor.rgb, vec3(0.74, 0.78, 0.82), 0.12) * (0.92 + 0.08 * phaseFog * dayFactor);
    color = mix(color, fogCol, fog);

    if (FogShadowIntensity > 0.0) {
        float phaseGR = 0.25 + 0.75 * cos6;
        float grTime = 0.45 + 0.55 * duskDawn;
        float strength = godAmt * phaseGR * FogShadowIntensity * (0.45 + 0.55 * fog) * layerPresence * grTime * 1.25;
        color += lightCol * appliedTint * min(strength, 0.45);
    }

    if (FogGlowStrength > 0.001) {
        float phiG = hash(vec3(gl_FragCoord.xy, 9.7)) * 6.2831853;
        float sG = sin(phiG), cG = cos(phiG);
        vec3 glow = vec3(0.0);
        const int GN = 12;
        for (int i = 0; i < GN; i++) {
            vec2 v = VOGEL12[i];
            vec2 o = vec2(v.x * cG - v.y * sG, v.x * sG + v.y * cG) * vec2(0.5625, 1.0) * 0.035;
            vec3 c = texture(Sampler0, clamp(texCoord + o, 0.0, 1.0)).rgb;
            float lum = dot(c, vec3(0.299, 0.587, 0.114));
            float warm = 0.45 + 1.10 * clamp((c.r - c.b) * 1.5, 0.0, 1.0);
            glow += c * max(lum - 0.52, 0.0) * warm;
        }
        glow /= float(GN);

        float selfLum = dot(color, vec3(0.299, 0.587, 0.114));
        float selfWarm = clamp((color.r - color.b) * 1.5, 0.0, 1.0);
        vec3 self = color * max(selfLum - 0.62, 0.0) * selfWarm;

        float glowGate = mix(1.0, 0.22, dayFactor);
        color += (glow * 2.4 + self * 0.5) * FogGlowStrength * glowGate * (1.0 - fog * 0.6);
    }

    float sunOccl = smoothstep(0.9990, 0.9999, texture(Sampler1, clamp(FogSunScreenUV, 0.0, 1.0)).r);
    if (FogSunVisible > 0.01 && dayFactor > 0.05) {
        vec2 d = (texCoord - FogSunScreenUV) * vec2(1.78, 1.0);
        float sunD = length(d);
        float core = smoothstep(0.04, 0.0, sunD);
        float glare = exp(-sunD * 11.0);
        vec3 sunWarm = vec3(1.0, 0.95, 0.82);
        color += sunWarm * FogSunVisible * sunOccl * dayFactor * (core * 1.5 + glare * 0.25);
    }

    float exposure = AutoExposureEnabled > 0.5 ? texture(Sampler5, vec2(0.5)).r : 1.0;
    if (exposure <= 0.0) exposure = 1.0;
    color *= exposure;

    vec3 hx = max(color - vec3(0.78), vec3(0.0));
    color = min(color, vec3(0.78)) + hx / (1.0 + 4.6 * hx);

    fragColor = vec4(color, 1.0);
}
