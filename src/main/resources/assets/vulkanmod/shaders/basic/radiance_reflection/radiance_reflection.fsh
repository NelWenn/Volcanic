#version 450

layout(binding = 0) uniform UBO {
    mat4 FogInvMVPMat;
    mat4 FogMVPMat;
    vec3 FogSunDir;
    vec3 FogCameraPos;
    float WindTime;
};

layout(binding = 1) uniform sampler2D Sampler0;
layout(binding = 2) uniform sampler2D Sampler1;
layout(binding = 3) uniform sampler2D Sampler2;
layout(binding = 4) uniform sampler2D Sampler3;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

const int SSR_REFINE = 6;
const float SSR_BASE = 0.18;
const float SSR_MAX = 0.6;
const float WAVE_STRENGTH = 0.045;
const float WAVE_SPEED = 3.3;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash12(i);
    float b = hash12(i + vec2(1.0, 0.0));
    float c = hash12(i + vec2(0.0, 1.0));
    float d = hash12(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float waterHeight(vec2 p, float t) {
    mat2 rot = mat2(0.80, -0.60, 0.60, 0.80);
    vec2 q = p * 1.1;
    float h = vnoise(q + vec2(t * 0.12, -t * 0.08)) * 0.65;
    q = rot * q * 2.1;
    h += vnoise(q + vec2(-t * 0.10, t * 0.14)) * 0.35;
    return h;
}

vec3 waterNormal(vec2 p, float time) {
    float t = time * WAVE_SPEED;
    float e = 0.16;
    float h0 = waterHeight(p, t);
    float hx = waterHeight(p + vec2(e, 0.0), t);
    float hz = waterHeight(p + vec2(0.0, e), t);
    vec2 grad = (vec2(hx, hz) - h0) / e * WAVE_STRENGTH;
    return normalize(vec3(-grad.x, 1.0, -grad.y));
}

vec3 worldAt(vec2 uv, float d) {
    vec4 n = vec4(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0, d, 1.0);
    vec4 w = FogInvMVPMat * n;
    return w.xyz / w.w;
}

vec3 skyReflect(vec3 d) {
    vec3 sunDir = normalize(FogSunDir + vec3(1e-4));
    if (sunDir.y < 0.0) sunDir = -sunDir;
    float up = clamp(d.y, 0.0, 1.0);
    vec3 horizon = vec3(0.74, 0.83, 0.95);
    vec3 zenith = vec3(0.42, 0.62, 0.92);
    vec3 sky = mix(horizon, zenith, pow(up, 0.6));
    float s = max(dot(d, sunDir), 0.0);
    sky += vec3(1.0, 0.88, 0.66) * pow(s, 10.0) * 0.45;
    float day = clamp(FogSunDir.y * 4.0 + 0.3, 0.0, 1.0);
    vec3 night = mix(vec3(0.03, 0.04, 0.09), vec3(0.01, 0.02, 0.05), up);
    return mix(night, sky, day);
}

vec3 traceReflection(vec3 startPos, vec3 dir, out float hit) {
    hit = 0.0;
    vec4 cs = FogMVPMat * vec4(startPos, 1.0);
    vec4 ce = FogMVPMat * vec4(startPos + dir * 120.0, 1.0);
    if (ce.w < 0.05) {
        float tc = (0.05 - cs.w) / (ce.w - cs.w);
        ce = mix(cs, ce, tc);
    }
    vec2 uv0 = (cs.xy / cs.w) * vec2(0.5, -0.5) + 0.5;
    vec2 uv1 = (ce.xy / ce.w) * vec2(0.5, -0.5) + 0.5;
    vec2 res = vec2(textureSize(Sampler3, 0));
    int steps = int(clamp(length((uv1 - uv0) * res) / 8.0, 16.0, 48.0));
    float dt = 1.0 / float(steps);
    float t = dt;
    for (int i = 0; i < 48; i++) {
        if (i >= steps) break;
        vec4 c = mix(cs, ce, t);
        vec2 uv = (c.xy / c.w) * vec2(0.5, -0.5) + 0.5;
        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) break;
        float rayDepth = c.z / c.w;
        float sceneDepth = texture(Sampler3, uv).r;
        if (sceneDepth < 0.9999 && rayDepth > sceneDepth) {
            float t0 = t - dt, t1 = t;
            for (int j = 0; j < SSR_REFINE; j++) {
                float tm = 0.5 * (t0 + t1);
                vec4 cm = mix(cs, ce, tm);
                vec2 muv = (cm.xy / cm.w) * vec2(0.5, -0.5) + 0.5;
                if (cm.z / cm.w > texture(Sampler3, muv).r) t1 = tm; else t0 = tm;
            }
            vec4 ch = mix(cs, ce, t1);
            vec2 huv = (ch.xy / ch.w) * vec2(0.5, -0.5) + 0.5;
            vec3 rp = worldAt(huv, ch.z / ch.w);
            vec3 sp = worldAt(huv, texture(Sampler3, huv).r);
            if (length(rp) - length(sp) > 3.0) break;
            vec2 e = smoothstep(vec2(0.0), vec2(0.08), huv) * smoothstep(vec2(0.0), vec2(0.08), 1.0 - huv);
            hit = 1.0;
            return mix(skyReflect(dir), texture(Sampler0, huv).rgb, e.x * e.y);
        }
        t += dt;
    }
    hit = 1.0;
    return skyReflect(dir);
}

void main() {
    float depthOpaque = texture(Sampler1, texCoord).r;
    float depthTrans = texture(Sampler2, texCoord).r;
    float depth = min(depthOpaque, depthTrans);
    if (depth >= 0.9999) {
        fragColor = vec4(0.0);
        return;
    }

    vec4 ndc = vec4(texCoord.x * 2.0 - 1.0, 1.0 - texCoord.y * 2.0, depth, 1.0);
    vec4 rel = FogInvMVPMat * ndc;
    rel.xyz /= rel.w;

    vec3 rayDir = rel.xyz / max(length(rel.xyz), 1e-4);
    vec3 surfN = normalize(cross(dFdx(rel.xyz), dFdy(rel.xyz)));
    vec3 nrm = dot(surfN, rayDir) > 0.0 ? -surfN : surfN;

    float dOpaqueOnly = texture(Sampler3, texCoord).r;
    float translucentInWorld = step(depthOpaque + 1e-6, dOpaqueOnly);
    float notForeground = step(depthOpaque, depthTrans + 1e-6);
    float flatness = smoothstep(0.4, 0.75, nrm.y);
    float belowCamera = 1.0 - step(-0.05, rel.y);
    float reflectivity = translucentInWorld * notForeground * flatness * belowCamera;
    if (reflectivity < 0.001) {
        fragColor = vec4(0.0);
        return;
    }

    vec3 wN = waterNormal(rel.xz + FogCameraPos.xz, WindTime);
    float ndv = max(dot(-rayDir, wN), 0.0);
    float fres = mix(SSR_BASE, SSR_MAX, pow(1.0 - ndv, 4.0));
    float hit;
    vec3 reflColor = traceReflection(rel.xyz, reflect(rayDir, wN), hit);
    fragColor = vec4(reflColor, reflectivity * fres * hit);
}
