#version 450

layout (local_size_x = 8, local_size_y = 8) in;

layout (binding = 0) uniform sampler2D srcTexture;
layout (binding = 1, r32f) uniform writeonly image2D dstImage;

layout (push_constant) uniform DownsampleData {
    ivec2 dstSize;
    int srcLod;
    int mode;
};

void main() {
    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);
    if (pos.x >= dstSize.x || pos.y >= dstSize.y) {
        return;
    }

    if (mode == 0) {
        imageStore(dstImage, pos, vec4(texelFetch(srcTexture, pos, 0).r));
        return;
    }

    ivec2 srcSize = textureSize(srcTexture, srcLod);
    ivec2 maxCoord = srcSize - 1;
    ivec2 base = pos * 2;

    float d = texelFetch(srcTexture, min(base, maxCoord), srcLod).r;
    d = max(d, texelFetch(srcTexture, min(base + ivec2(1, 0), maxCoord), srcLod).r);
    d = max(d, texelFetch(srcTexture, min(base + ivec2(0, 1), maxCoord), srcLod).r);
    d = max(d, texelFetch(srcTexture, min(base + ivec2(1, 1), maxCoord), srcLod).r);

    bool oddX = (srcSize.x & 1) != 0;
    bool oddY = (srcSize.y & 1) != 0;

    if (oddX) {
        d = max(d, texelFetch(srcTexture, min(base + ivec2(2, 0), maxCoord), srcLod).r);
        d = max(d, texelFetch(srcTexture, min(base + ivec2(2, 1), maxCoord), srcLod).r);
    }
    if (oddY) {
        d = max(d, texelFetch(srcTexture, min(base + ivec2(0, 2), maxCoord), srcLod).r);
        d = max(d, texelFetch(srcTexture, min(base + ivec2(1, 2), maxCoord), srcLod).r);
        if (oddX) {
            d = max(d, texelFetch(srcTexture, min(base + ivec2(2, 2), maxCoord), srcLod).r);
        }
    }

    imageStore(dstImage, pos, vec4(d));
}
