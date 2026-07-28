#version 460

layout (binding = 0) uniform ExternalLodUniforms {
    mat4 ExternalLodCombinedMatrix;
    vec4 ExternalLodRenderParams;
    vec4 ExternalLodFogColor;
    vec4 ExternalLodFogParams;
    vec4 ExternalLodCellOrigins[1024];
};

layout (binding = 2) uniform sampler2D uBlockAtlas;

layout (location = 0) in vec4 vertexColor;
layout (location = 1) in vec3 vertexWorldPos;
layout (location = 2) flat in vec4 vSpriteRect;
layout (location = 3) smooth in vec2 vTileUV;

layout (location = 0) out vec4 fragColor;

float bayer8x8(vec2 st) {
    int x = int(mod(st.x, 8.0));
    int y = int(mod(st.y, 8.0));
    int index = y * 8 + x;
    float values[64] = float[64](
        0.0, 32.0, 8.0, 40.0, 2.0, 34.0, 10.0, 42.0,
        48.0, 16.0, 56.0, 24.0, 50.0, 18.0, 58.0, 26.0,
        12.0, 44.0, 4.0, 36.0, 14.0, 46.0, 6.0, 38.0,
        60.0, 28.0, 52.0, 20.0, 62.0, 30.0, 54.0, 22.0,
        3.0, 35.0, 11.0, 43.0, 1.0, 33.0, 9.0, 41.0,
        51.0, 19.0, 59.0, 27.0, 49.0, 17.0, 57.0, 25.0,
        15.0, 47.0, 7.0, 39.0, 13.0, 45.0, 5.0, 37.0,
        63.0, 31.0, 55.0, 23.0, 61.0, 29.0, 53.0, 21.0
    );
    return values[index] / 64.0;
}

void main() {
    float clipDistance = ExternalLodRenderParams.y;
    bool dither = ExternalLodRenderParams.w != 0.0;
    float viewDistance = length(vertexWorldPos);

    if (clipDistance > 0.0) {
        if (dither) {
            float noise = bayer8x8(gl_FragCoord.xy) + 0.001;
            float fadeStep = smoothstep(clipDistance * 0.85, clipDistance * 1.7, viewDistance);
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

    float fogFactor = smoothstep(ExternalLodFogParams.x, ExternalLodFogParams.y, viewDistance);
    fragColor.rgb = mix(fragColor.rgb, ExternalLodFogColor.rgb, fogFactor);
}
