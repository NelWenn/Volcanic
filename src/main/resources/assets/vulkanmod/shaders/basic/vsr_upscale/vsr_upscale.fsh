#version 450

layout(binding = 0) uniform UBO {
    vec4 VsrInputInfo;
    vec4 VsrOutputInfo;
    vec4 VsrUvBounds;
    vec4 VsrParams;
};

layout(binding = 1) uniform sampler2D Sampler0;

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

    vec3 range = mx - mn;
    if (max(max(range.r, range.g), range.b) < FLAT_THRESHOLD) {
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

    fragColor = vec4(result, 1.0);
}
