#version 450

#define RADIANCE_DEBUG 0

layout(binding = 0) uniform UBO {
    mat4 FogInvMVPMat;
    vec3 FogSunDir;
    float FogShadowIntensity;
};

layout(binding = 1) uniform sampler2D Sampler0;
layout(binding = 2) uniform sampler2D Sampler1;
layout(binding = 3) uniform sampler2D Sampler2;
layout(binding = 4) uniform sampler2D Sampler3;
layout(binding = 5) uniform sampler2D Sampler4;
layout(binding = 6) uniform sampler2D Sampler5;
layout(binding = 7) uniform sampler2D Sampler6;
layout(binding = 8) uniform sampler2D Sampler7;

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

vec4 upsampleShadows(float dist) {
    vec2 ht = 2.0 / vec2(textureSize(Sampler3, 0));
    vec4 sum = texture(Sampler3, texCoord);
    float wSum = 1.0;
    for (int i = 0; i < 16; i++) {
        vec4 t = texture(Sampler3, texCoord + VOGEL16[i] * ht);
        float w = 1.0 / (1.0 + 6.0 * abs(dist - t.b) / max(dist, 1.0));
        sum += t * w;
        wSum += w;
    }
    return sum / wSum;
}

float upsampleAO(float dist) {
    vec2 ht = 2.6 / vec2(textureSize(Sampler4, 0));
    vec2 c = texture(Sampler4, texCoord).rg;
    float sum = c.r;
    float wSum = 1.0;
    for (int i = 0; i < 16; i++) {
        vec2 t = texture(Sampler4, texCoord + VOGEL16[i] * ht).rg;
        float w = 1.0 / (1.0 + 6.0 * abs(dist - t.g) / max(dist, 1.0));
        sum += t.r * w;
        wSum += w;
    }
    return sum / wSum;
}

vec3 upsampleGI(float dist) {
    vec2 ht = 2.6 / vec2(textureSize(Sampler5, 0));
    vec4 c = texture(Sampler5, texCoord);
    vec3 sum = c.rgb;
    float wSum = 1.0;
    for (int i = 0; i < 16; i++) {
        vec4 t = texture(Sampler5, texCoord + VOGEL16[i] * ht);
        float w = 1.0 / (1.0 + 6.0 * abs(dist - t.a) / max(dist, 1.0));
        sum += t.rgb * w;
        wSum += w;
    }
    return sum / wSum;
}

void main() {
#if RADIANCE_DEBUG
    if (texCoord.x < 0.5) {
        float dd = min(texture(Sampler1, texCoord).r, texture(Sampler2, texCoord).r);
        vec4 nd = vec4(texCoord.x * 2.0 - 1.0, 1.0 - texCoord.y * 2.0, dd, 1.0);
        vec4 rl = FogInvMVPMat * nd;
        fragColor = vec4(upsampleGI(length(rl.xyz / rl.w)) * 8.0, 1.0);
    } else if (texCoord.y < 0.85) {
        fragColor = vec4(texture(Sampler6, vec2((texCoord.x - 0.5) * 2.0, texCoord.y / 0.85)).rgb, 1.0);
    } else {
        float rd = texture(Sampler7, vec2((texCoord.x - 0.5) * 2.0, (texCoord.y - 0.85) / 0.15)).r;
        fragColor = vec4(vec3(fract(rd * 32.0)), 1.0);
    }
    return;
#endif
    vec3 color = texture(Sampler0, texCoord).rgb;
    float depth = min(texture(Sampler1, texCoord).r, texture(Sampler2, texCoord).r);

    vec4 ndc = vec4(texCoord.x * 2.0 - 1.0, 1.0 - texCoord.y * 2.0, depth, 1.0);
    vec4 rel = FogInvMVPMat * ndc;
    rel.xyz /= rel.w;

    bool isSky = depth >= 0.9999;
    float dist = length(rel.xyz);
    vec3 rayDir = rel.xyz / max(dist, 1e-4);

    vec3 surfN = normalize(cross(dFdx(rel.xyz), dFdy(rel.xyz)));
    vec3 nrm = dot(surfN, rayDir) > 0.0 ? -surfN : surfN;
    vec3 lightDir = FogSunDir.y >= 0.0 ? FogSunDir : -FogSunDir;
    vec3 lightDirN = normalize(lightDir + vec3(1e-4));
    vec3 lightCol = FogSunDir.y >= 0.0 ? vec3(1.0, 0.92, 0.75) : vec3(0.58, 0.68, 1.0);
    float dayFactor = clamp(FogSunDir.y * 1.2 + 0.2, 0.0, 1.0);

    float shadowTerm = 0.0;
    float shadowFrac = 0.0;
    float sunInterior = 0.0;
    if (!isSky && FogShadowIntensity > 0.0) {
        vec4 sh = upsampleShadows(dist);
        shadowTerm = sh.r;
        shadowFrac = clamp(shadowTerm / max(FogShadowIntensity, 1e-3), 0.0, 1.0);
        sunInterior = sh.g;

        color *= mix(vec3(1.0), vec3(0.60, 0.65, 0.80), shadowTerm);

        float ndlLight = max(dot(nrm, lightDirN), 0.0);
        float litFactor = (1.0 - shadowFrac) * (1.0 - sunInterior) * ndlLight;
        color += color * lightCol * litFactor * FogShadowIntensity * 0.30;
    }

    if (!isSky && dist < 64.0) {
        float ao = upsampleAO(dist);
        float direct = (1.0 - shadowFrac) * (1.0 - sunInterior) * FogShadowIntensity * dayFactor;
        float aoW = mix(0.85, 0.35, clamp(direct, 0.0, 1.0)) * smoothstep(64.0, 44.0, dist);
        color *= 1.0 - aoW * (1.0 - ao);
    }

    if (!isSky && dist < 48.0 && FogShadowIntensity > 0.0) {
        vec3 gi = upsampleGI(dist) * smoothstep(48.0, 36.0, dist);
        vec3 alb = min(color / max(dot(color, vec3(0.299, 0.587, 0.114)), 0.09), vec3(2.0));
        color += gi * alb * 0.8;
    }

    vec3 hx = max(color - vec3(0.82), vec3(0.0));
    color = min(color, vec3(0.82)) + hx / (1.0 + 4.0 * hx);

    fragColor = vec4(color, 1.0);
}
