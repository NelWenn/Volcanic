#version 460

layout (binding = 0) uniform ExternalLodUniforms {
    mat4 ExternalLodCombinedMatrix;
    vec4 ExternalLodRenderParams;
    vec4 ExternalLodFogColor;
    vec4 ExternalLodFogParams;
    vec4 ExternalLodCellOrigins[1024];
};

layout (location = 0) in vec4 vertexColor;
layout (location = 1) in float vViewDist;

layout (location = 0) out vec4 fragColor;

void main() {
    fragColor = vertexColor;

    float viewDistance = vViewDist;
    float fogFactor = smoothstep(ExternalLodFogParams.x, ExternalLodFogParams.y, viewDistance);
    fragColor.rgb = mix(fragColor.rgb, ExternalLodFogColor.rgb, fogFactor);
}
