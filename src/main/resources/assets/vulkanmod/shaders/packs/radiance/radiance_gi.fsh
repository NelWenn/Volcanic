#version 450

layout(binding = 0) uniform UBO {
    mat4 FogInvMVPMat;
    mat4 FogRsmMVP;
    mat4 FogRsmInvMVP;
    mat4 FogPrevMVP;
    vec3 FogCameraPos;
    float FogShadowIntensity;
    vec3 FogSunDir;
    float FogTaaStrength;
    vec3 FogRsmCameraPos;
    float FogTaaFrame;
    vec3 FogPrevCameraPos;
    float FrameDelta;
};

layout(binding = 1) uniform sampler2D Sampler0;
layout(binding = 2) uniform sampler2D Sampler1;
layout(binding = 3) uniform sampler2D Sampler2;
layout(binding = 4) uniform sampler2D Sampler3;
layout(binding = 5) uniform sampler2D Sampler4;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

const float PI = 3.14159265;
const float GI_MAX_DIST = 48.0;
const float GI_WORLD_RADIUS = 8.0;
const float GI_STRENGTH = 2.2;
const int GI_TAPS = 8;

const vec2 VOGEL8[8] = vec2[](
    vec2(0.2500000, 0.0000000),
    vec2(-0.3192963, 0.2924952),
    vec2(0.0488772, -0.5568742),
    vec2(0.4024323, 0.5249242),
    vec2(-0.7385533, -0.1306276),
    vec2(0.6995708, -0.4450364),
    vec2(-0.2340157, 0.8704779),
    vec2(-0.4462884, -0.8592734)
);

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

vec3 rsmPos(vec2 uv, float rd) {
    vec4 q = FogRsmInvMVP * vec4(uv.x * 2.0 - 1.0, 1.0 - uv.y * 2.0, rd, 1.0);
    return q.xyz / q.w;
}

void main() {
    ivec2 fullResTexel = ivec2(gl_FragCoord.xy) * 2;
    vec2 fullResSize = vec2(textureSize(Sampler0, 0));
    float depth = min(texelFetch(Sampler0, fullResTexel, 0).r, texelFetch(Sampler1, fullResTexel, 0).r);
    vec2 fullUV = (vec2(fullResTexel) + 0.5) / fullResSize;

    if (depth >= 0.9999 || FogShadowIntensity <= 0.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 4096.0);
        return;
    }

    vec4 ndc4 = vec4(fullUV.x * 2.0 - 1.0, 1.0 - fullUV.y * 2.0, depth, 1.0);
    vec4 relW = FogInvMVPMat * ndc4;
    vec3 p = relW.xyz / relW.w;
    float dist = length(p);
    if (dist > GI_MAX_DIST) {
        fragColor = vec4(0.0, 0.0, 0.0, dist);
        return;
    }

    vec3 N = normalize(cross(dFdx(p), dFdy(p)));
    vec3 V = -p / max(dist, 1e-4);
    if (dot(N, V) < 0.0) N = -N;
    vec3 lightDir = FogSunDir.y >= 0.0 ? FogSunDir : -FogSunDir;
    vec3 lightCol = FogSunDir.y >= 0.0 ? vec3(1.0, 0.92, 0.75) : vec3(0.58, 0.68, 1.0);

    vec3 shadowRel = p + (FogCameraPos - FogRsmCameraPos);
    vec4 sc = FogRsmMVP * vec4(shadowRel, 1.0);
    vec3 gi = vec3(0.0);
    if (sc.w > 0.0) {
        vec3 ndc = sc.xyz / sc.w;
        vec2 uv = vec2(ndc.x * 0.5 + 0.5, 0.5 - ndc.y * 0.5);
        if (uv.x > 0.0 && uv.x < 1.0 && uv.y > 0.0 && uv.y < 1.0) {
            float uvPerBlock = 0.5 * length(vec3(FogRsmMVP[0][0], FogRsmMVP[1][0], FogRsmMVP[2][0]));
            float rUV = GI_WORLD_RADIUS * uvPerBlock;
            float px = 1.0 / float(textureSize(Sampler3, 0).x);

            float tj = FogTaaStrength > 0.0 ? FogTaaFrame * 0.6180339887 : 0.0;
            float phi = fract(hash(vec3(gl_FragCoord.xy, 11.3)) + tj) * 2.0 * PI;
            float s = sin(phi), c = cos(phi);

            for (int i = 0; i < GI_TAPS; i++) {
                vec2 v = VOGEL8[i];
                vec2 o = vec2(v.x * c - v.y * s, v.x * s + v.y * c) * rUV;
                vec2 tapUV = uv + o;
                if (any(lessThan(tapUV, vec2(px))) || any(greaterThan(tapUV, vec2(1.0 - px)))) continue;
                float rd = texture(Sampler3, tapUV).r;
                if (rd >= 0.9999) continue;
                vec3 flux = texture(Sampler2, tapUV).rgb;
                if (dot(flux, flux) < 1e-4) continue;

                vec3 vpl = rsmPos(tapUV, rd);
                vec3 du = rsmPos(tapUV + vec2(px, 0.0), texture(Sampler3, tapUV + vec2(px, 0.0)).r) - vpl;
                vec3 dv = rsmPos(tapUV + vec2(0.0, px), texture(Sampler3, tapUV + vec2(0.0, px)).r) - vpl;
                vec3 nV = normalize(cross(du, dv));
                if (dot(nV, lightDir) < 0.0) nV = -nV;

                vec3 w3 = vpl - shadowRel;
                float d2 = dot(w3, w3);
                float t = sqrt(max(d2, 1e-6));
                if (t < 0.35) continue;
                vec3 omega = w3 / t;
                float emitCos = max(dot(nV, -omega), 0.0);
                float recvCos = max(dot(N, omega), 0.0);
                gi += flux * (emitCos * recvCos / (0.4 * d2 + 0.8));
            }
            gi *= (GI_WORLD_RADIUS * GI_WORLD_RADIUS / float(GI_TAPS)) * GI_STRENGTH * FogShadowIntensity * lightCol;
            gi = min(gi, vec3(2.5));
        }
    }

    if (FogTaaStrength > 0.0) {
        vec3 prevRel = p + (FogCameraPos - FogPrevCameraPos);
        vec4 prevClip = FogPrevMVP * vec4(prevRel, 1.0);
        if (prevClip.w > 0.0001) {
            vec2 pndc = prevClip.xy / prevClip.w;
            vec2 prevUV = vec2(pndc.x * 0.5 + 0.5, 0.5 - pndc.y * 0.5);
            if (prevUV.x > 0.0 && prevUV.x < 1.0 && prevUV.y > 0.0 && prevUV.y < 1.0) {
                vec4 hist = texture(Sampler4, prevUV);
                if (abs(hist.a - dist) < 0.04 * dist + 0.25) {
                    gi = mix(gi, hist.rgb, 0.70);
                }
            }
        }
    }

    fragColor = vec4(gi, dist);
}
