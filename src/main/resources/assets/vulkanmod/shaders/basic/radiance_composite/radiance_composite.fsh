#version 450

#define CSM_DEBUG 0
#define TEMPORAL_DEBUG 0

layout(binding = 0) uniform UBO {
    mat4 FogInvMVPMat;
    mat4 FogMVPMat;
    vec3 FogSunDir;
    float FogShadowIntensity;
};

layout(binding = 1) uniform sampler2D Sampler0;
layout(binding = 2) uniform sampler2D Sampler1;
layout(binding = 3) uniform sampler2D Sampler2;
layout(binding = 4) uniform sampler2D Sampler3;
layout(binding = 5) uniform sampler2D Sampler4;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

const float GOLDEN = 2.39996323;
const float AO_STRENGTH = 0.35;
const float HIGHLIGHT = 0.28;
const int SSR_STEPS = 28;
const int SSR_REFINE = 6;
const float SSR_BASE = 0.6;

vec2 vogel(int i, int n) {
    float r = sqrt((float(i) + 0.5) / float(n));
    float theta = float(i) * GOLDEN;
    return r * vec2(cos(theta), sin(theta));
}

vec4 upsampleLight(float dist) {
    vec2 ht = 0.8 / vec2(textureSize(Sampler3, 0));
    vec4 sum = texture(Sampler3, texCoord);
    float wSum = 1.0;
    for (int i = 0; i < 12; i++) {
        vec4 t = texture(Sampler3, texCoord + vogel(i, 12) * ht);
        float w = 1.0 / (1.0 + 30.0 * abs(dist - t.a) / max(dist, 1.0));
        sum += t * w;
        wSum += w;
    }
    return sum / wSum;
}

vec2 projUV(vec3 p) {
    vec4 c = FogMVPMat * vec4(p, 1.0);
    return vec2(c.x / c.w * 0.5 + 0.5, 0.5 - c.y / c.w * 0.5);
}

vec3 worldAt(vec2 uv, float d) {
    vec4 n = vec4(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0, d, 1.0);
    vec4 w = FogInvMVPMat * n;
    return w.xyz / w.w;
}

vec3 skyReflect(vec3 d) {
    float t = clamp(d.y, 0.0, 1.0);
    vec3 sky = mix(vec3(0.72, 0.80, 0.92), vec3(0.40, 0.58, 0.88), t);
    return mix(sky, vec3(0.05, 0.07, 0.15), clamp(-FogSunDir.y * 2.0, 0.0, 0.9));
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
    vec2 res = vec2(textureSize(Sampler4, 0));
    int steps = int(clamp(length((uv1 - uv0) * res) / 6.0, 16.0, 64.0));
    float dt = 1.0 / float(steps);
    float t = dt;
    for (int i = 0; i < 64; i++) {
        if (i >= steps) break;
        vec4 c = mix(cs, ce, t);
        vec2 uv = (c.xy / c.w) * vec2(0.5, -0.5) + 0.5;
        if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) break;
        float rayDepth = c.z / c.w;
        float sceneDepth = texture(Sampler4, uv).r;
        if (sceneDepth < 0.9999 && rayDepth > sceneDepth) {
            float t0 = t - dt, t1 = t;
            for (int j = 0; j < SSR_REFINE; j++) {
                float tm = 0.5 * (t0 + t1);
                vec4 cm = mix(cs, ce, tm);
                vec2 muv = (cm.xy / cm.w) * vec2(0.5, -0.5) + 0.5;
                if (cm.z / cm.w > texture(Sampler4, muv).r) t1 = tm; else t0 = tm;
            }
            vec4 ch = mix(cs, ce, t1);
            vec2 huv = (ch.xy / ch.w) * vec2(0.5, -0.5) + 0.5;
            vec3 rp = worldAt(huv, ch.z / ch.w);
            vec3 sp = worldAt(huv, texture(Sampler4, huv).r);
            if (length(rp) - length(sp) > 3.0) break;
            vec2 e = smoothstep(vec2(0.0), vec2(0.08), huv) * smoothstep(vec2(0.0), vec2(0.08), 1.0 - huv);
            hit = e.x * e.y;
            return texture(Sampler0, huv).rgb;
        }
        t += dt;
    }
    hit = 0.7;
    return skyReflect(dir);
}

void main() {
#if TEMPORAL_DEBUG
    fragColor = vec4(vec3(texture(Sampler3, texCoord).r), 1.0);
    return;
#endif
    vec3 color = texture(Sampler0, texCoord).rgb;
    float depthOpaque = texture(Sampler1, texCoord).r;
    float depthTrans = texture(Sampler2, texCoord).r;
    float depth = min(depthOpaque, depthTrans);

    vec4 ndc = vec4(texCoord.x * 2.0 - 1.0, 1.0 - texCoord.y * 2.0, depth, 1.0);
    vec4 rel = FogInvMVPMat * ndc;
    rel.xyz /= rel.w;

    bool isSky = depth >= 0.9999;
    if (isSky) {
        fragColor = vec4(color, 1.0);
        return;
    }

#if CSM_DEBUG
    {
        vec4 L = texture(Sampler3, texCoord);
        float ci = L.g;
        vec3 cc = ci < 0.5 ? vec3(1.0, 0.35, 0.35) : (ci < 1.5 ? vec3(0.35, 1.0, 0.35) : vec3(0.35, 0.35, 1.0));
        fragColor = vec4(cc * (1.0 - L.r * 0.85), 1.0);
        return;
    }
#endif

    float dist = length(rel.xyz);
    vec3 rayDir = rel.xyz / max(dist, 1e-4);
    vec3 surfN = normalize(cross(dFdx(rel.xyz), dFdy(rel.xyz)));
    vec3 nrm = dot(surfN, rayDir) > 0.0 ? -surfN : surfN;

    vec3 lightDir = FogSunDir.y >= 0.0 ? FogSunDir : -FogSunDir;
    vec3 lightDirN = normalize(lightDir + vec3(1e-4));
    vec3 lightCol = FogSunDir.y >= 0.0 ? vec3(1.0, 0.92, 0.75) : vec3(0.58, 0.68, 1.0);

    vec4 lightBuf = upsampleLight(dist);
    float shadowTerm = lightBuf.r;
    float ao = lightBuf.g;
    float interior = lightBuf.b;
    float shadowFrac = clamp(shadowTerm / max(FogShadowIntensity, 1e-3), 0.0, 1.0);

    color *= mix(vec3(1.0), vec3(0.60, 0.65, 0.80), shadowTerm);

    if (FogShadowIntensity > 0.0) {
        float ndlLight = max(dot(nrm, lightDirN), 0.0);
        float lit = (1.0 - shadowFrac) * (1.0 - interior) * ndlLight;
        color += color * lightCol * lit * FogShadowIntensity * HIGHLIGHT;
    }

    color *= 1.0 - AO_STRENGTH * (1.0 - ao);

    float dOpaqueOnly = texture(Sampler4, texCoord).r;
    float translucentInWorld = step(depthOpaque + 1e-6, dOpaqueOnly);
    float notForeground = step(depthOpaque, depthTrans + 1e-6);
    float flatness = smoothstep(0.85, 0.97, nrm.y);
    float reflectivity = translucentInWorld * notForeground * flatness;
    if (reflectivity > 0.001) {
        vec3 wN = vec3(0.0, 1.0, 0.0);
        float ndv = max(-rayDir.y, 0.0);
        float fres = mix(SSR_BASE, 1.0, pow(1.0 - ndv, 4.0));
        float hit;
        vec3 reflColor = traceReflection(rel.xyz, reflect(rayDir, wN), hit);
        float w = reflectivity * fres * hit;
        color = mix(color, reflColor, w);
    }

    vec3 hx = max(color - vec3(0.82), vec3(0.0));
    color = min(color, vec3(0.82)) + hx / (1.0 + 4.0 * hx);

    fragColor = vec4(color, 1.0);
}
