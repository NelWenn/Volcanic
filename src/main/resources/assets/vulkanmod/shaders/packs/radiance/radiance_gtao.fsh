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
    float FogTaaFrame;
};

layout(binding = 1) uniform sampler2D Sampler0;
layout(binding = 2) uniform sampler2D Sampler1;
layout(binding = 3) uniform sampler2D Sampler2;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

const float PI = 3.14159265;
const float AO_RADIUS = 3.2;
const float AO_MAX_DIST = 64.0;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

vec3 relAt(vec2 uv) {
    float d = texture(Sampler0, uv).r;
    vec4 ndc = vec4(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0, d, 1.0);
    vec4 rel = FogInvMVPMat * ndc;
    return rel.xyz / rel.w;
}

void main() {
    ivec2 fullResTexel = ivec2(gl_FragCoord.xy) * 2;
    vec2 fullResSize = vec2(textureSize(Sampler0, 0));
    float depth = min(texelFetch(Sampler0, fullResTexel, 0).r, texelFetch(Sampler1, fullResTexel, 0).r);
    vec2 fullUV = (vec2(fullResTexel) + 0.5) / fullResSize;

    if (depth >= 0.9999) {
        fragColor = vec4(1.0, 4096.0, 0.0, 0.0);
        return;
    }

    vec4 ndc4 = vec4(fullUV.x * 2.0 - 1.0, 1.0 - fullUV.y * 2.0, depth, 1.0);
    vec4 relW = FogInvMVPMat * ndc4;
    vec3 p = relW.xyz / relW.w;
    float dist = length(p);
    if (dist > AO_MAX_DIST) {
        fragColor = vec4(1.0, dist, 0.0, 0.0);
        return;
    }
    vec3 V = -p / max(dist, 1e-4);
    vec3 Tu = dFdx(p);
    vec3 Tv = dFdy(p);
    vec3 N = normalize(cross(Tu, Tv));
    if (dot(N, V) < 0.0) N = -N;

    float tanHalf = FogInvProjMat[1][1];
    float radY = AO_RADIUS / max(2.0 * dist * tanHalf, 1e-4);
    vec2 radiusUV = min(vec2(radY * fullResSize.y / fullResSize.x, radY), vec2(0.12));

    float tj = FogTaaStrength > 0.0 ? FogTaaFrame * 0.6180339887 : 0.0;
    float rot = fract(hash(vec3(gl_FragCoord.xy, 5.1)) + tj) * PI;

    float visSum = 0.0;
    float wSum = 0.0;
    const int SLICES = 2;
    const int STEPS = 3;
    for (int sl = 0; sl < SLICES; sl++) {
        float phi = rot + float(sl) * (PI / float(SLICES));
        vec2 dir2 = vec2(cos(phi), sin(phi));
        vec3 D = dir2.x * Tu + dir2.y * Tv;
        D -= V * dot(D, V);
        float dLen = length(D);
        if (dLen < 1e-6) continue;
        D /= dLen;

        float nx = dot(N, D);
        float ny = dot(N, V);
        float nLen = sqrt(nx * nx + ny * ny);
        float gamma = atan(nx, ny);

        float h1c = -1.0;
        float h2c = -1.0;
        for (int s = 1; s <= STEPS; s++) {
            float jitter = hash(vec3(gl_FragCoord.xy, float(s) * 2.3 + phi));
            float frac = pow((float(s) - 0.5 + 0.5 * jitter) / float(STEPS), 1.5);
            vec2 off = dir2 * radiusUV * frac;
            for (int side = 0; side < 2; side++) {
                vec2 uvS = fullUV + (side == 0 ? off : -off);
                if (any(lessThan(uvS, vec2(0.0))) || any(greaterThan(uvS, vec2(1.0)))) continue;
                vec3 q = relAt(uvS);
                vec3 w = q - p;
                float t = length(w);
                if (t < 1e-4) continue;
                float cand = dot(w / t, V);
                cand = mix(cand, -1.0, clamp((t - AO_RADIUS) / AO_RADIUS, 0.0, 1.0));
                if (side == 0) h2c = max(h2c, cand); else h1c = max(h1c, cand);
            }
        }

        float t1 = -acos(clamp(h1c, -1.0, 1.0));
        float t2 = acos(clamp(h2c, -1.0, 1.0));
        float a1 = gamma + max(t1 - gamma, -PI * 0.5);
        float a2 = gamma + min(t2 - gamma, PI * 0.5);
        float vis = 0.25 * (-cos(2.0 * a1 - gamma) + cos(gamma) + 2.0 * a1 * sin(gamma))
                  + 0.25 * (-cos(2.0 * a2 - gamma) + cos(gamma) + 2.0 * a2 * sin(gamma));
        visSum += vis * nLen;
        wSum += nLen;
    }

    float ao = clamp(wSum > 1e-4 ? visSum / wSum : 1.0, 0.0, 1.0);

    if (FogTaaStrength > 0.0) {
        vec3 prevRel = p + (FogCameraPos - FogPrevCameraPos);
        vec4 prevClip = FogPrevMVP * vec4(prevRel, 1.0);
        if (prevClip.w > 0.0001) {
            vec2 pndc = prevClip.xy / prevClip.w;
            vec2 prevUV = vec2(pndc.x * 0.5 + 0.5, 0.5 - pndc.y * 0.5);
            if (prevUV.x > 0.0 && prevUV.x < 1.0 && prevUV.y > 0.0 && prevUV.y < 1.0) {
                vec2 hist = texture(Sampler2, prevUV).rg;
                if (abs(hist.g - dist) < 0.1 * dist + 0.5) {
                    ao = mix(ao, hist.r, 0.85);
                }
            }
        }
    }

    fragColor = vec4(ao, dist, 0.0, 0.0);
}
