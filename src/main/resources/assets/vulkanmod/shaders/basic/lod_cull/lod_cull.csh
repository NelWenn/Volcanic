#version 450

layout (local_size_x = 64) in;

struct AabbRecord {
    vec4 minBound;
    vec4 maxBound;
};

layout (std430, binding = 0) readonly buffer AabbData {
    AabbRecord aabbs[];
};

layout (std430, binding = 1) buffer CommandData {
    uint cmds[];
};

layout (std430, binding = 2) buffer CompactedCommandData {
    uint outCmds[];
};

layout (std430, binding = 3) buffer CompactedCountData {
    uint outCounts[];
};

layout (binding = 4) uniform sampler2D depthPyramid;

layout (push_constant) uniform CullData {
    mat4 viewProj;
    uvec4 counts;
    uvec4 bases;
    vec4 pyramid;
};

const uint FLAG_OCCLUSION = 1u;
const uint FLAG_COMPACT = 2u;
const uint CMD_STRIDE = 5u;

bool frustumVisible(vec3 mn, vec3 mx) {
    vec4 r0 = vec4(viewProj[0][0], viewProj[1][0], viewProj[2][0], viewProj[3][0]);
    vec4 r1 = vec4(viewProj[0][1], viewProj[1][1], viewProj[2][1], viewProj[3][1]);
    vec4 r2 = vec4(viewProj[0][2], viewProj[1][2], viewProj[2][2], viewProj[3][2]);
    vec4 r3 = vec4(viewProj[0][3], viewProj[1][3], viewProj[2][3], viewProj[3][3]);

    vec4 planes[6] = vec4[](r3 + r0, r3 - r0, r3 + r1, r3 - r1, r2, r3 - r2);

    for (int i = 0; i < 6; ++i) {
        vec4 plane = planes[i];
        vec3 support = mix(mn, mx, greaterThan(plane.xyz, vec3(0.0)));
        if (dot(plane.xyz, support) + plane.w < 0.0) {
            return false;
        }
    }
    return true;
}

bool occluded(vec3 mn, vec3 mx) {
    vec2 uvMin = vec2(1.0);
    vec2 uvMax = vec2(0.0);
    float nearestDepth = 1.0;

    for (int i = 0; i < 8; ++i) {
        vec3 corner = mix(mn, mx, bvec3((i & 1) != 0, (i & 2) != 0, (i & 4) != 0));
        vec4 clip = viewProj * vec4(corner, 1.0);
        if (clip.w <= 1.0e-4) {
            return false;
        }
        vec3 ndc = clip.xyz / clip.w;
        vec2 uv = vec2(ndc.x * 0.5 + 0.5, 0.5 - ndc.y * 0.5);
        uvMin = min(uvMin, uv);
        uvMax = max(uvMax, uv);
        nearestDepth = min(nearestDepth, ndc.z);
    }

    uvMin = clamp(uvMin, vec2(0.0), vec2(1.0));
    uvMax = clamp(uvMax, vec2(0.0), vec2(1.0));
    nearestDepth = clamp(nearestDepth - pyramid.z, 0.0, 1.0);

    vec2 sizePx = (uvMax - uvMin) * pyramid.xy;
    float maxDim = max(max(sizePx.x, sizePx.y), 1.0);
    int lod = clamp(int(ceil(log2(maxDim))), 0, int(counts.z) - 1);

    ivec2 lodSize = textureSize(depthPyramid, lod);
    ivec2 maxTexel = lodSize - 1;
    ivec2 t0 = clamp(ivec2(uvMin * vec2(lodSize)), ivec2(0), maxTexel);
    ivec2 t1 = clamp(ivec2(uvMax * vec2(lodSize)), ivec2(0), maxTexel);

    float farthest = texelFetch(depthPyramid, t0, lod).r;
    farthest = max(farthest, texelFetch(depthPyramid, ivec2(t1.x, t0.y), lod).r);
    farthest = max(farthest, texelFetch(depthPyramid, ivec2(t0.x, t1.y), lod).r);
    farthest = max(farthest, texelFetch(depthPyramid, t1, lod).r);

    return nearestDepth > farthest;
}

void main() {
    uint idx = gl_GlobalInvocationID.x;
    if (idx >= counts.x) {
        return;
    }

    AabbRecord box = aabbs[bases.x + idx];
    vec3 mn = box.minBound.xyz;
    vec3 mx = box.maxBound.xyz;

    bool visible = frustumVisible(mn, mx);
    if (visible && (counts.y & FLAG_OCCLUSION) != 0u && counts.z > 0u) {
        visible = !occluded(mn, mx);
    }

    uint srcBase = counts.w + idx * CMD_STRIDE;
    if ((counts.y & FLAG_COMPACT) != 0u) {
        if (visible) {
            uint slot = atomicAdd(outCounts[bases.z], 1u);
            uint dstBase = bases.y + slot * CMD_STRIDE;
            outCmds[dstBase] = cmds[srcBase];
            outCmds[dstBase + 1u] = cmds[srcBase + 1u];
            outCmds[dstBase + 2u] = cmds[srcBase + 2u];
            outCmds[dstBase + 3u] = cmds[srcBase + 3u];
            outCmds[dstBase + 4u] = cmds[srcBase + 4u];
        }
    } else if (!visible) {
        cmds[srcBase] = 0u;
    }
}
