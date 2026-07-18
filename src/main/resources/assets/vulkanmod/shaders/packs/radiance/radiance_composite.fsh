#version 450

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

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

const float GOLDEN = 2.39996323;
const float GI_STRENGTH = 0.6;
const float AO_STRENGTH = 0.5;
const float HIGHLIGHT = 0.28;

vec2 vogel(int i, int n) {
    float r = sqrt((float(i) + 0.5) / float(n));
    float theta = float(i) * GOLDEN;
    return r * vec2(cos(theta), sin(theta));
}

vec4 upsampleLight(float dist) {
    vec2 ht = 2.2 / vec2(textureSize(Sampler3, 0));
    vec4 sum = texture(Sampler3, texCoord);
    float wSum = 1.0;
    for (int i = 0; i < 16; i++) {
        vec4 t = texture(Sampler3, texCoord + vogel(i, 16) * ht);
        float w = 1.0 / (1.0 + 40.0 * abs(dist - t.a) / max(dist, 1.0));
        sum += t * w;
        wSum += w;
    }
    return sum / wSum;
}

void main() {
    vec3 color = texture(Sampler0, texCoord).rgb;
    float depth = min(texture(Sampler1, texCoord).r, texture(Sampler2, texCoord).r);

    vec4 ndc = vec4(texCoord.x * 2.0 - 1.0, 1.0 - texCoord.y * 2.0, depth, 1.0);
    vec4 rel = FogInvMVPMat * ndc;
    rel.xyz /= rel.w;

    bool isSky = depth >= 0.9999;
    if (isSky) {
        fragColor = vec4(color, 1.0);
        return;
    }

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

    vec3 bleed = texture(Sampler4, texCoord).rgb;
    vec3 tint = bleed - dot(bleed, vec3(0.299, 0.587, 0.114));
    float indirectAmt = clamp(shadowFrac + (1.0 - ao), 0.0, 1.0);
    color += color * tint * GI_STRENGTH * indirectAmt;

    vec3 hx = max(color - vec3(0.82), vec3(0.0));
    color = min(color, vec3(0.82)) + hx / (1.0 + 4.0 * hx);

    fragColor = vec4(color, 1.0);
}
