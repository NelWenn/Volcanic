#version 460

layout (binding = 0) uniform ExternalLodUniforms {
    mat4 ExternalLodCombinedMatrix;
    vec4 ExternalLodModelOffsetAndYOffset;
    vec4 ExternalLodRenderParams;
};

layout (binding = 2) uniform sampler2D uBlockAtlas;

layout (location = 0) in vec4 vertexColor;
layout (location = 1) in vec3 vertexWorldPos;
layout (location = 2) flat in vec4 vSpriteRect;
layout (location = 3) smooth in vec2 vTileUV;

layout (location = 0) out vec4 fragColor;

float bayer4x4(vec2 st) {
    int x = int(mod(st.x, 4.0));
    int y = int(mod(st.y, 4.0));
    int index = y * 4 + x;
    float values[16] = float[16](
        0.0, 8.0, 2.0, 10.0,
        12.0, 4.0, 14.0, 6.0,
        3.0, 11.0, 1.0, 9.0,
        15.0, 7.0, 13.0, 5.0
    );
    return values[index] / 16.0;
}

void main() {
    float clipDistance = ExternalLodRenderParams.y;
    bool dither = ExternalLodRenderParams.w != 0.0;
    float viewDistance = length(vertexWorldPos);

    if (clipDistance > 0.0) {
        if (dither) {
            float noise = bayer4x4(gl_FragCoord.xy) + 0.001;
            float fadeStep = smoothstep(clipDistance, clipDistance * 1.5, viewDistance);
            if (fadeStep <= noise) {
                discard;
            }
        } else if (viewDistance < clipDistance) {
            discard;
        }
    }

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
}
