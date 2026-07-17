#version 450

// Render-scale upscale — AMD FidelityFX Super Resolution 1.0, EASU pass (Edge-Adaptive Spatial
// Upsampling), float-path fragment adaptation. FSR 1.0 is distributed under the MIT license
// (Copyright (c) 2021 Advanced Micro Devices, Inc.). Runs when render scale < 100%.
// Input resolution is queried from the sampler; texCoord (0..1) is the output position.
// RCAS sharpening is applied as a following pass.

layout(binding = 0) uniform sampler2D DiffuseSampler;

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

vec3 tap(vec2 texelXY, vec2 invSize) {
    vec2 uv = clamp((texelXY + 0.5) * invSize, invSize * 0.5, 1.0 - invSize * 0.5);
    return textureLod(DiffuseSampler, uv, 0.0).rgb;
}

float rcp(float x) { return 1.0 / max(x, 1.0 / 32768.0); }

// Analyse edge direction/length contribution of one corner (green channel as luma).
void FsrEasuSet(inout vec2 dir, inout float len, vec2 pp, float w,
                float lA, float lB, float lC, float lD, float lE) {
    // Horizontal (B-C-D around center C).
    float lenX = max(abs(lD - lC), abs(lC - lB));
    lenX = rcp(lenX);
    float dirX = lD - lB;
    dir.x += dirX * w;
    lenX = clamp(abs(dirX) * lenX, 0.0, 1.0);
    lenX *= lenX;
    len += lenX * w;
    // Vertical (A-C-E around center C).
    float lenY = max(abs(lE - lC), abs(lC - lA));
    lenY = rcp(lenY);
    float dirY = lE - lA;
    dir.y += dirY * w;
    lenY = clamp(abs(dirY) * lenY, 0.0, 1.0);
    lenY *= lenY;
    len += lenY * w;
}

// Anisotropic Lanczos(2)-approx tap.
void FsrEasuTap(inout vec3 aC, inout float aW, vec2 off, vec2 dir, vec2 len2,
                float lob, float clp, vec3 c) {
    vec2 v = vec2(dot(off, dir), dot(off, vec2(-dir.y, dir.x)));
    v *= len2;
    float d2 = min(dot(v, v), clp);
    float wB = (2.0 / 5.0) * d2 - 1.0;
    float wA = lob * d2 - 1.0;
    wB *= wB;
    wA *= wA;
    wB = 1.5625 * wB - 0.5625;
    float w = wB * wA;
    aC += c * w;
    aW += w;
}

void main() {
    vec2 inSize = vec2(textureSize(DiffuseSampler, 0));
    vec2 invSize = 1.0 / inSize;

    // Output pixel position in input-texel space.
    vec2 pp = texCoord * inSize - 0.5;
    vec2 fp = floor(pp);
    pp -= fp; // fractional in [0,1)

    // 12-tap window relative to fp:
    //     b c
    //   e f g h
    //   i j k l
    //     n o
    vec3 b = tap(fp + vec2( 0.0, -1.0), invSize);
    vec3 c = tap(fp + vec2( 1.0, -1.0), invSize);
    vec3 e = tap(fp + vec2(-1.0,  0.0), invSize);
    vec3 f = tap(fp + vec2( 0.0,  0.0), invSize);
    vec3 g = tap(fp + vec2( 1.0,  0.0), invSize);
    vec3 h = tap(fp + vec2( 2.0,  0.0), invSize);
    vec3 i = tap(fp + vec2(-1.0,  1.0), invSize);
    vec3 j = tap(fp + vec2( 0.0,  1.0), invSize);
    vec3 k = tap(fp + vec2( 1.0,  1.0), invSize);
    vec3 l = tap(fp + vec2( 2.0,  1.0), invSize);
    vec3 n = tap(fp + vec2( 0.0,  2.0), invSize);
    vec3 o = tap(fp + vec2( 1.0,  2.0), invSize);

    float bL = b.g, cL = c.g, eL = e.g, fL = f.g, gL = g.g, hL = h.g;
    float iL = i.g, jL = j.g, kL = k.g, lL = l.g, nL = n.g, oL = o.g;

    // Edge direction & length from the 4 nearest corners, bilinear-weighted.
    vec2 dir = vec2(0.0);
    float len = 0.0;
    FsrEasuSet(dir, len, pp, (1.0 - pp.x) * (1.0 - pp.y), bL, eL, fL, gL, jL);
    FsrEasuSet(dir, len, pp,        pp.x  * (1.0 - pp.y), cL, fL, gL, hL, kL);
    FsrEasuSet(dir, len, pp, (1.0 - pp.x) *        pp.y,  fL, iL, jL, kL, nL);
    FsrEasuSet(dir, len, pp,        pp.x  *        pp.y,  gL, jL, kL, lL, oL);

    // Normalize direction; derive anisotropy and kernel shape.
    float dir2 = dir.x * dir.x + dir.y * dir.y;
    bool zero = dir2 < (1.0 / 32768.0);
    float dirRcp = inversesqrt(max(dir2, 1.0 / 32768.0));
    dir = zero ? vec2(1.0, 0.0) : dir * dirRcp;

    len = len * 0.5;
    len *= len;

    float stretch = 1.0 / max(abs(dir.x), abs(dir.y));
    vec2 len2 = vec2(1.0 + (stretch - 1.0) * len, 1.0 - 0.5 * len);
    float lob = 0.5 - 0.29 * len;
    float clp = 1.0 / lob;

    // Accumulate the 12 weighted taps.
    vec3 aC = vec3(0.0);
    float aW = 0.0;
    FsrEasuTap(aC, aW, vec2( 0.0, -1.0) - pp, dir, len2, lob, clp, b);
    FsrEasuTap(aC, aW, vec2( 1.0, -1.0) - pp, dir, len2, lob, clp, c);
    FsrEasuTap(aC, aW, vec2(-1.0,  1.0) - pp, dir, len2, lob, clp, i);
    FsrEasuTap(aC, aW, vec2( 0.0,  1.0) - pp, dir, len2, lob, clp, j);
    FsrEasuTap(aC, aW, vec2( 0.0,  0.0) - pp, dir, len2, lob, clp, f);
    FsrEasuTap(aC, aW, vec2(-1.0,  0.0) - pp, dir, len2, lob, clp, e);
    FsrEasuTap(aC, aW, vec2( 1.0,  1.0) - pp, dir, len2, lob, clp, k);
    FsrEasuTap(aC, aW, vec2( 2.0,  1.0) - pp, dir, len2, lob, clp, l);
    FsrEasuTap(aC, aW, vec2( 2.0,  0.0) - pp, dir, len2, lob, clp, h);
    FsrEasuTap(aC, aW, vec2( 1.0,  0.0) - pp, dir, len2, lob, clp, g);
    FsrEasuTap(aC, aW, vec2( 1.0,  2.0) - pp, dir, len2, lob, clp, o);
    FsrEasuTap(aC, aW, vec2( 0.0,  2.0) - pp, dir, len2, lob, clp, n);

    vec3 color = aC / aW;
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
