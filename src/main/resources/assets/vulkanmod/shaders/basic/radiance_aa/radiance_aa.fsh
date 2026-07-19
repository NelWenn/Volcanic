#version 450

#define AA_DEBUG 0

layout(binding = 0) uniform UBO {
    mat4 FogInvMVPMat;
    float AaMode;
};

layout(binding = 1) uniform sampler2D Sampler0;
layout(binding = 2) uniform sampler2D Sampler1;
layout(binding = 3) uniform sampler2D Sampler2;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

float luma(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

vec3 fxaa(vec2 uv, vec2 rcp) {
    float lC = luma(texture(Sampler0, uv).rgb);
    float lN = luma(texture(Sampler0, uv + vec2(0.0, -rcp.y)).rgb);
    float lS = luma(texture(Sampler0, uv + vec2(0.0,  rcp.y)).rgb);
    float lE = luma(texture(Sampler0, uv + vec2( rcp.x, 0.0)).rgb);
    float lW = luma(texture(Sampler0, uv + vec2(-rcp.x, 0.0)).rgb);

    float lMin = min(lC, min(min(lN, lS), min(lE, lW)));
    float lMax = max(lC, max(max(lN, lS), max(lE, lW)));
    float range = lMax - lMin;
    if (range < max(0.0312, lMax * 0.125)) {
        return texture(Sampler0, uv).rgb;
    }

    float lNW = luma(texture(Sampler0, uv + vec2(-rcp.x, -rcp.y)).rgb);
    float lNE = luma(texture(Sampler0, uv + vec2( rcp.x, -rcp.y)).rgb);
    float lSW = luma(texture(Sampler0, uv + vec2(-rcp.x,  rcp.y)).rgb);
    float lSE = luma(texture(Sampler0, uv + vec2( rcp.x,  rcp.y)).rgb);

    vec2 dir;
    dir.x = -((lNW + lNE) - (lSW + lSE));
    dir.y =  ((lNW + lSW) - (lNE + lSE));

    float reduce = max((lNW + lNE + lSW + lSE) * 0.03125, 0.0078125);
    float minDir = 1.0 / (min(abs(dir.x), abs(dir.y)) + reduce);
    dir = clamp(dir * minDir, -8.0, 8.0) * rcp;

    vec3 rA = 0.5 * (texture(Sampler0, uv + dir * (1.0 / 3.0 - 0.5)).rgb
                   + texture(Sampler0, uv + dir * (2.0 / 3.0 - 0.5)).rgb);
    vec3 rB = rA * 0.5 + 0.25 * (texture(Sampler0, uv + dir * -0.5).rgb
                               + texture(Sampler0, uv + dir *  0.5).rgb);

    float lB = luma(rB);
    return (lB < lMin || lB > lMax) ? rA : rB;
}

float sceneDist(vec2 uv) {
    float depth = min(texture(Sampler1, uv).r, texture(Sampler2, uv).r);
    if (depth >= 0.9999) return 4096.0;
    vec4 ndc = vec4(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0, depth, 1.0);
    vec4 rel = FogInvMVPMat * ndc;
    return length(rel.xyz / rel.w);
}

vec3 edgeAA(vec2 uv, vec2 rcp) {
    vec3 c = texture(Sampler0, uv).rgb;
    float dC = sceneDist(uv);
    float dL = sceneDist(uv - vec2(rcp.x, 0.0));
    float dR = sceneDist(uv + vec2(rcp.x, 0.0));
    float dU = sceneDist(uv - vec2(0.0, rcp.y));
    float dD = sceneDist(uv + vec2(0.0, rcp.y));

    float curv = abs(dL + dR - 2.0 * dC) + abs(dU + dD - 2.0 * dC);
    float thr = 0.08 + dC * 0.02;
    float edge = smoothstep(thr, thr * 2.5, curv);
    if (edge <= 0.0) return c;

    vec3 sum = c
        + texture(Sampler0, uv + vec2(-rcp.x, -rcp.y)).rgb
        + texture(Sampler0, uv + vec2( 0.0,   -rcp.y)).rgb
        + texture(Sampler0, uv + vec2( rcp.x, -rcp.y)).rgb
        + texture(Sampler0, uv + vec2(-rcp.x,  0.0)).rgb
        + texture(Sampler0, uv + vec2( rcp.x,  0.0)).rgb
        + texture(Sampler0, uv + vec2(-rcp.x,  rcp.y)).rgb
        + texture(Sampler0, uv + vec2( 0.0,    rcp.y)).rgb
        + texture(Sampler0, uv + vec2( rcp.x,  rcp.y)).rgb;
    return mix(c, sum / 9.0, edge);
}

void main() {
    vec2 rcp = 1.0 / vec2(textureSize(Sampler0, 0));
    vec3 color;
    if (AaMode < 0.5) {
        color = texture(Sampler0, texCoord).rgb;
    } else if (AaMode < 1.5) {
        color = fxaa(texCoord, rcp);
    } else {
        color = edgeAA(texCoord, rcp);
    }
#if AA_DEBUG
    vec3 tint = AaMode < 0.5 ? vec3(1.0) : (AaMode < 1.5 ? vec3(1.0, 0.4, 0.4) : vec3(0.4, 0.4, 1.0));
    color *= tint;
#endif
    fragColor = vec4(color, 1.0);
}
