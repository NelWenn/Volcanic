#version 450

#include "light.glsl"
#include "fog.glsl"

layout(binding = 2) uniform sampler2D Sampler0;
layout(binding = 4) uniform sampler2D Sampler4;

layout(binding = 1) uniform UBO {
    vec4 FogColor;
    float FogStart;
    float FogEnd;
    float AlphaCutout;
    float PbrDebug;
    float CamilleActive;
    float GameTime;
    float SunAngle;
    vec3 FogSunDir;
};

layout(location = 0) in float vertexDistance;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) in vec2 texCoord0;
layout(location = 3) in vec3 worldPos;
layout(location = 4) flat in vec4 tileAM;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 outNormal;

const float POM_DEPTH = 0.14;
const float POM_MAX_OFFSET = 0.6;
const float SHADOW_DEPTH = 0.6;

vec2 tileWrap(vec2 inTile) {
    return fract(inTile) * tileAM.zw + tileAM.xy;
}

vec3 slopeNormal(vec2 inTile, float traceZ, vec3 viewDir, vec2 dcdx, vec2 dcdy) {
    vec2 atlasSize = vec2(textureSize(Sampler4, 0));
    vec2 atlasPixelSize = 1.0 / atlasSize;
    float atlasAspect = atlasSize.x / atlasSize.y;
    vec2 atlasCoord = tileWrap(inTile);
    vec2 tilePixelSize = 1.0 / (atlasSize * tileAM.zw);
    vec2 texSnapped = floor(atlasCoord * atlasSize) * atlasPixelSize;
    vec2 texOffset = atlasCoord - (texSnapped + 0.5 * atlasPixelSize);
    vec2 stepSign = sign(texOffset);
    vec2 viewSign = sign(viewDir.xy);
    bool dir = abs(texOffset.x * atlasAspect) < abs(texOffset.y);
    vec2 texX, texY;
    if (dir) {
        texX = inTile - vec2(tilePixelSize.x * viewSign.x, 0.0);
        texY = inTile + vec2(0.0, stepSign.y * tilePixelSize.y);
    } else {
        texX = inTile + vec2(tilePixelSize.x * stepSign.x, 0.0);
        texY = inTile - vec2(0.0, viewSign.y * tilePixelSize.y);
    }
    float heightX = textureGrad(Sampler4, tileWrap(texX), dcdx, dcdy).a;
    float heightY = textureGrad(Sampler4, tileWrap(texY), dcdx, dcdy).a;
    if (dir) {
        if (!(traceZ > heightY && viewSign.y != stepSign.y)) {
            if (traceZ > heightX) return vec3(-viewSign.x, 0.0, 0.0);
            if (abs(viewDir.y) > abs(viewDir.x)) return vec3(0.0, -viewSign.y, 0.0);
            return vec3(-viewSign.x, 0.0, 0.0);
        }
        return vec3(0.0, -viewSign.y, 0.0);
    }
    if (!(traceZ > heightX && viewSign.x != stepSign.x)) {
        if (traceZ > heightY) return vec3(0.0, -viewSign.y, 0.0);
        if (abs(viewDir.y) > abs(viewDir.x)) return vec3(0.0, -viewSign.y, 0.0);
        return vec3(-viewSign.x, 0.0, 0.0);
    }
    return vec3(-viewSign.x, 0.0, 0.0);
}

void main() {
    if (CamilleActive < 0.5) {
        vec4 vanillaColor = texture(Sampler0, texCoord0) * vertexColor;
        if (vanillaColor.a < AlphaCutout) {
            discard;
        }
        outNormal = vec4(0.0, 0.0, 1.0, 1.0);
        fragColor = linear_fog(vanillaColor, vertexDistance, FogStart, FogEnd, FogColor);
        return;
    }

    vec3 dp1 = dFdx(worldPos);
    vec3 dp2 = dFdy(worldPos);
    vec3 geoN = normalize(cross(dp1, dp2));
    if (dot(geoN, worldPos) > 0.0) {
        geoN = -geoN;
    }
    vec2 duv1 = dFdx(texCoord0);
    vec2 duv2 = dFdy(texCoord0);
    vec3 dp2perp = cross(dp2, geoN);
    vec3 dp1perp = cross(geoN, dp1);
    vec3 T = dp2perp * duv1.x + dp1perp * duv2.x;
    vec3 B = dp2perp * duv1.y + dp1perp * duv2.y;
    vec3 Tn = T * inversesqrt(dot(T, T) + 1e-30);
    vec3 Bn = B * inversesqrt(dot(B, B) + 1e-30);

    vec3 eyeDir = normalize(-worldPos);
    vec3 tV = vec3(dot(eyeDir, Tn), dot(eyeDir, Bn), dot(eyeDir, geoN));

    vec2 uv = texCoord0;
    float pomDepth = 0.0;
    float traceZ = 1.0;
    bool axisAligned = max(max(abs(geoN.x), abs(geoN.y)), abs(geoN.z)) > 0.95;
    float pomActive = axisAligned ? (1.0 - smoothstep(24.0, 44.0, vertexDistance)) : 0.0;
    float heightStart = texture(Sampler4, texCoord0).a;
    float pomStrength = pomActive * (1.0 - pow(heightStart, 32.0));

    if (pomStrength > 0.01) {
        float tvz = max(tV.z, 0.2);
        vec2 P = -(tV.xy / tvz) * POM_DEPTH * pomStrength;
        float pl = length(P);
        if (pl > POM_MAX_OFFSET) { P *= POM_MAX_OFFSET / pl; }
        const int LAYERS = 48;
        float layerDepth = 1.0 / float(LAYERS);
        vec2 deltaUV = P * layerDepth;

        float rayHeight = 1.0;
        vec2 curTile = (texCoord0 - tileAM.xy) / max(tileAM.zw, vec2(1e-6));
        float surfH = textureGrad(Sampler4, tileWrap(curTile), duv1, duv2).a;
        for (int i = 0; i < LAYERS; i++) {
            if (surfH >= rayHeight) break;
            rayHeight -= layerDepth;
            curTile -= deltaUV;
            surfH = textureGrad(Sampler4, tileWrap(curTile), duv1, duv2).a;
        }
        uv = tileWrap(curTile);
        vec2 atlasTexel = 1.0 / vec2(textureSize(Sampler0, 0));
        uv = clamp(uv, tileAM.xy + atlasTexel, tileAM.xy + tileAM.zw - atlasTexel);
        pomDepth = clamp(1.0 - rayHeight, 0.0, 1.0);
        traceZ = rayHeight;
    }

    vec4 color = textureLod(Sampler0, uv, 0.0) * vertexColor;
    if (color.a < AlphaCutout) {
        discard;
    }

    vec4 nSample = textureLod(Sampler4, uv, 0.0);
    vec2 nxy = nSample.rg * 2.0 - 1.0;
    nxy = -nxy;
    float nz = sqrt(max(1.0 - dot(nxy, nxy), 0.0));
    vec3 tN = vec3(nxy, nz);

    if (pomDepth > 0.001 && nSample.a - traceZ > 0.04) {
        vec2 inTile = (uv - tileAM.xy) / max(tileAM.zw, vec2(1e-6));
        vec3 sn = slopeNormal(inTile, traceZ, tV, duv1, duv2);
        sn.xy = -sn.xy;
        tN = normalize(mix(tN, sn, 0.65 * pomActive));
    }
    vec3 worldN = normalize(Tn * tN.x + Bn * tN.y + geoN * tN.z);
    outNormal = vec4(worldN, 1.0);

    if (PbrDebug > 0.5) {
        fragColor = vec4(worldN * 0.5 + 0.5, 1.0);
        return;
    }

    vec3 sunDir = normalize((FogSunDir.y >= 0.0 ? FogSunDir : -FogSunDir) + vec3(1e-4));
    float relief = dot(worldN, sunDir) - dot(geoN, sunDir);
    color.rgb *= clamp(1.0 + relief * 1.0, 0.6, 1.35);
    color.rgb *= mix(1.0, nSample.b, 0.35);

    if (pomActive > 0.01 && heightStart < 0.999) {
        vec3 lt = vec3(dot(sunDir, Tn), dot(sunDir, Bn), dot(sunDir, geoN));
        if (lt.z > 0.05) {
            float psh = 1.0;
            vec2 inTileS = (uv - tileAM.xy) / max(tileAM.zw, vec2(1e-6));
            for (int i = 0; i < 6 && psh > 0.05; i++) {
                float sc = 0.03 * (float(i) + 0.5);
                float rayH = nSample.a + lt.z * sc;
                float offH = textureLod(Sampler4, tileWrap(inTileS + lt.xy * SHADOW_DEPTH * sc), 0.0).a;
                psh *= clamp(1.0 - (offH - rayH) * 3.0, 0.0, 1.0);
            }
            color.rgb *= mix(0.4, 1.0, psh) * pomActive + (1.0 - pomActive);
        }
    }

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
