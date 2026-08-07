#version 450

layout(binding = 0) uniform UBO {
    mat4 FogInvMVPMat;
    mat4 FogPrevMVP;
    vec3 FogCameraPos;
    vec3 FogPrevCameraPos;
    vec4 VsrInputInfo;
    vec4 VsrOutputInfo;
    vec4 VsrUvBounds;
    vec4 VsrParams;
};

layout(binding = 1) uniform sampler2D Sampler0;
layout(binding = 2) uniform sampler2D Sampler1;
layout(binding = 3) uniform sampler2D Sampler2;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

vec3 fetch(vec2 uv) {
    return texture(Sampler0, clamp(uv, VsrUvBounds.xy, VsrUvBounds.zw)).rgb;
}

float luma(vec3 c) {
    return dot(c, vec3(0.5, 1.0, 0.5));
}

float rcpSafe(float v) {
    return 1.0 / max(v, 1.0 / 32768.0);
}

const float NOISE_FLOOR = 3.0 / 255.0;
const float FLAT_THRESHOLD = 5.0 / 255.0;

float denoise(float gradient) {
    return gradient * smoothstep(0.0, NOISE_FLOOR, abs(gradient));
}

void easuSet(inout vec2 dir, inout float len, vec2 pp, float w,
             float lA, float lB, float lC, float lD, float lE) {
    float dc = lD - lC;
    float cb = lC - lB;
    float lenX = rcpSafe(max(abs(dc), abs(cb)));
    float dirX = denoise(lD - lB);
    dir.x += dirX * w;
    lenX = clamp(abs(dirX) * lenX, 0.0, 1.0);
    lenX *= lenX;
    len += lenX * w;

    float ec = lE - lC;
    float ca = lC - lA;
    float lenY = rcpSafe(max(abs(ec), abs(ca)));
    float dirY = denoise(lE - lA);
    dir.y += dirY * w;
    lenY = clamp(abs(dirY) * lenY, 0.0, 1.0);
    lenY *= lenY;
    len += lenY * w;
}

void easuTap(inout vec3 acc, inout float accW, vec2 off, vec2 dir, vec2 len2,
             float lob, float clp, vec3 c) {
    vec2 v = vec2(off.x * dir.x + off.y * dir.y, off.x * -dir.y + off.y * dir.x);
    v *= len2;

    float d2 = min(dot(v, v), clp);
    float wB = 0.4 * d2 - 1.0;
    float wA = lob * d2 - 1.0;
    wB *= wB;
    wA *= wA;
    wB = 1.5625 * wB - 0.5625;

    float w = wB * wA;
    acc += c * w;
    accW += w;
}

vec3 sampleHistory(vec2 uv) {
    vec2 texSize = VsrOutputInfo.xy;
    vec2 samplePos = uv * texSize;
    vec2 texPos1 = floor(samplePos - 0.5) + 0.5;
    vec2 f = samplePos - texPos1;

    vec2 w0 = f * (-0.5 + f * (1.0 - 0.5 * f));
    vec2 w1 = 1.0 + f * f * (-2.5 + 1.5 * f);
    vec2 w2 = f * (0.5 + f * (2.0 - 1.5 * f));
    vec2 w3 = f * f * (-0.5 + 0.5 * f);

    vec2 w12 = w1 + w2;
    vec2 offset12 = w2 / max(w12, vec2(1.0e-5));

    vec2 uv0 = (texPos1 - 1.0) / texSize;
    vec2 uv3 = (texPos1 + 2.0) / texSize;
    vec2 uv12 = (texPos1 + offset12) / texSize;

    vec3 acc = vec3(0.0);
    float accW = 0.0;

    acc += texture(Sampler2, vec2(uv12.x, uv0.y)).rgb * (w12.x * w0.y);
    accW += w12.x * w0.y;
    acc += texture(Sampler2, vec2(uv0.x, uv12.y)).rgb * (w0.x * w12.y);
    accW += w0.x * w12.y;
    acc += texture(Sampler2, vec2(uv12.x, uv12.y)).rgb * (w12.x * w12.y);
    accW += w12.x * w12.y;
    acc += texture(Sampler2, vec2(uv3.x, uv12.y)).rgb * (w3.x * w12.y);
    accW += w3.x * w12.y;
    acc += texture(Sampler2, vec2(uv12.x, uv3.y)).rgb * (w12.x * w3.y);
    accW += w12.x * w3.y;

    return abs(accW) < 1.0e-5 ? texture(Sampler2, uv).rgb : acc / accW;
}

vec3 accumulate(vec3 current, vec3 mn, vec3 mx) {
    float depth = texture(Sampler1, texCoord).r;
    if (depth >= 1.0) {
        return current;
    }

    vec4 ndc = vec4(texCoord.x * 2.0 - 1.0, 1.0 - texCoord.y * 2.0, depth, 1.0);
    vec4 world = FogInvMVPMat * ndc;
    if (abs(world.w) < 1.0e-6) {
        return current;
    }

    vec3 p = world.xyz / world.w;
    vec3 prevRel = p + (FogCameraPos - FogPrevCameraPos);
    vec4 pc = FogPrevMVP * vec4(prevRel, 1.0);
    if (pc.w <= 0.0) {
        return current;
    }

    vec2 pndc = pc.xy / pc.w;
    vec2 prevUV = vec2(pndc.x * 0.5 + 0.5, 0.5 - pndc.y * 0.5);
    if (any(lessThan(prevUV, vec2(0.0))) || any(greaterThan(prevUV, vec2(1.0)))) {
        return current;
    }

    vec3 history = max(sampleHistory(prevUV), vec3(0.0));
    vec3 slack = (mx - mn) * 0.15;
    history = clamp(history, mn - slack, mx + slack);

    float motion = length(prevUV - texCoord);
    float feedback = 0.92 * (1.0 - smoothstep(0.02, 0.12, motion));

    return mix(current, history, feedback);
}

void main() {
    float mode = VsrParams.y;

    if (mode < 0.5) {
        fragColor = vec4(texture(Sampler0, texCoord).rgb, 1.0);
        return;
    }

    vec2 pp = texCoord * VsrInputInfo.xy - 0.5;
    vec2 fp = floor(pp);
    pp -= fp;

    vec2 base = (fp + 0.5) * VsrInputInfo.zw;
    vec2 t = VsrInputInfo.zw;

    vec3 cF = fetch(base);
    vec3 cG = fetch(base + vec2(t.x, 0.0));
    vec3 cJ = fetch(base + vec2(0.0, t.y));
    vec3 cK = fetch(base + vec2(t.x, t.y));

    vec3 mn = min(min(cF, cG), min(cJ, cK));
    vec3 mx = max(max(cF, cG), max(cJ, cK));
    vec3 bilinear = mix(mix(cF, cG, pp.x), mix(cJ, cK, pp.x), pp.y);

    bool temporal = mode > 2.5;
    vec3 range = mx - mn;

    if (!temporal && max(max(range.r, range.g), range.b) < FLAT_THRESHOLD) {
        fragColor = vec4(bilinear, 1.0);
        return;
    }

    vec3 cB = fetch(base + vec2(0.0, -t.y));
    vec3 cC = fetch(base + vec2(t.x, -t.y));
    vec3 cE = fetch(base + vec2(-t.x, 0.0));
    vec3 cH = fetch(base + vec2(2.0 * t.x, 0.0));
    vec3 cI = fetch(base + vec2(-t.x, t.y));
    vec3 cL = fetch(base + vec2(2.0 * t.x, t.y));
    vec3 cN = fetch(base + vec2(0.0, 2.0 * t.y));
    vec3 cO = fetch(base + vec2(t.x, 2.0 * t.y));

    if (temporal) {
        vec3 wideMin = min(min(min(cB, cC), min(cE, cH)), min(min(cI, cL), min(cN, cO)));
        vec3 wideMax = max(max(max(cB, cC), max(cE, cH)), max(max(cI, cL), max(cN, cO)));
        mn = min(mn, wideMin);
        mx = max(mx, wideMax);
        range = mx - mn;
    }

    vec3 result;

    if (mode < 1.5) {
        float lB = luma(cB);
        float lC = luma(cC);
        float lE = luma(cE);
        float lF = luma(cF);
        float lG = luma(cG);
        float lH = luma(cH);
        float lI = luma(cI);
        float lJ = luma(cJ);
        float lK = luma(cK);
        float lL = luma(cL);
        float lN = luma(cN);
        float lO = luma(cO);

        vec2 dir = vec2(0.0);
        float len = 0.0;

        easuSet(dir, len, pp, (1.0 - pp.x) * (1.0 - pp.y), lB, lE, lF, lG, lJ);
        easuSet(dir, len, pp, pp.x * (1.0 - pp.y), lC, lF, lG, lH, lK);
        easuSet(dir, len, pp, (1.0 - pp.x) * pp.y, lF, lI, lJ, lK, lN);
        easuSet(dir, len, pp, pp.x * pp.y, lG, lJ, lK, lL, lO);

        vec2 dir2 = dir * dir;
        float dirR = dir2.x + dir2.y;
        bool zro = dirR < (1.0 / 32768.0);
        dirR = inversesqrt(max(dirR, 1.0 / 32768.0));
        dirR = zro ? 1.0 : dirR;
        dir.x = zro ? 1.0 : dir.x;
        dir.y = zro ? 0.0 : dir.y;
        dir *= dirR;

        len = len * 0.5;
        len *= len;

        float stretch = dot(dir, dir) * rcpSafe(max(abs(dir.x), abs(dir.y)));
        vec2 len2 = vec2(1.0 + (stretch - 1.0) * len, 1.0 - 0.5 * len);
        float lob = 0.5 + ((1.0 / 4.0 - 0.04) - 0.5) * len;
        float clp = rcpSafe(lob);

        vec3 acc = vec3(0.0);
        float accW = 0.0;

        easuTap(acc, accW, vec2(0.0, -1.0) - pp, dir, len2, lob, clp, cB);
        easuTap(acc, accW, vec2(1.0, -1.0) - pp, dir, len2, lob, clp, cC);
        easuTap(acc, accW, vec2(-1.0, 0.0) - pp, dir, len2, lob, clp, cE);
        easuTap(acc, accW, vec2(0.0, 0.0) - pp, dir, len2, lob, clp, cF);
        easuTap(acc, accW, vec2(1.0, 0.0) - pp, dir, len2, lob, clp, cG);
        easuTap(acc, accW, vec2(2.0, 0.0) - pp, dir, len2, lob, clp, cH);
        easuTap(acc, accW, vec2(-1.0, 1.0) - pp, dir, len2, lob, clp, cI);
        easuTap(acc, accW, vec2(0.0, 1.0) - pp, dir, len2, lob, clp, cJ);
        easuTap(acc, accW, vec2(1.0, 1.0) - pp, dir, len2, lob, clp, cK);
        easuTap(acc, accW, vec2(2.0, 1.0) - pp, dir, len2, lob, clp, cL);
        easuTap(acc, accW, vec2(0.0, 2.0) - pp, dir, len2, lob, clp, cN);
        easuTap(acc, accW, vec2(1.0, 2.0) - pp, dir, len2, lob, clp, cO);

        result = acc / max(accW, 1.0 / 32768.0);
        result = clamp(result, mn, mx);
    } else {
        result = bilinear;
    }

    float sharpness = VsrParams.x;

    if (sharpness > 0.0) {
        vec3 soft = 0.125 * (cE + cH + cB + cN + cF + cG + cJ + cK);
        vec3 detail = result - soft;

        float confidence = smoothstep(FLAT_THRESHOLD, 4.0 * FLAT_THRESHOLD,
                max(max(range.r, range.g), range.b));

        vec3 slack = range * 0.25;
        vec3 lo = max(mn - slack, vec3(0.0));
        vec3 hi = min(mx + slack, vec3(1.0));

        result = clamp(result + detail * (sharpness * confidence), lo, hi);
    }

    if (temporal) {
        result = accumulate(result, mn, mx);
    }

    fragColor = vec4(result, 1.0);
}
