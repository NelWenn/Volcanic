#version 460
#include "lod_common.glsl"

layout (binding = 0) uniform ExternalLodUniforms {
    mat4 ExternalLodCombinedMatrix;
    vec4 ExternalLodRenderParams;
    vec4 ExternalLodFogColor;
    vec4 ExternalLodFogParams;
    vec4 ExternalLodCellOrigins[1024];
};

layout (binding = 2) uniform sampler2D uBlockAtlas;

layout (location = 0) in vec4 vertexColor;
layout (location = 1) in float vViewDist;
layout (location = 2) flat in vec4 vSpriteRect;
layout (location = 3) smooth in vec2 vTileUV;
layout (location = 4) in vec3 vWorldPos;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 outNormal;

void main() {
    vec2 duv = vSpriteRect.zw - vSpriteRect.xy;
    vec2 dX = dFdx(vTileUV) * duv;
    vec2 dY = dFdy(vTileUV) * duv;
    if (duv.x <= 0.0) {
        fragColor = vertexColor;
    } else {
        vec2 atlasUV = vSpriteRect.xy + fract(vTileUV) * duv;
        vec4 texel = textureGrad(uBlockAtlas, atlasUV, dX, dY);
        fragColor = texel * vertexColor;
    }

    float viewDistance = vViewDist;
    fragColor.rgb = lod_atmosphere(fragColor.rgb, viewDistance, ExternalLodFogColor.rgb, ExternalLodFogParams.x, ExternalLodFogParams.y);

    outNormal = vec4(lod_geo_normal(vWorldPos), 1.0);
}
