#version 460

layout (binding = 0) uniform ExternalLodUniforms {
    mat4 ExternalLodCombinedMatrix;
    vec4 ExternalLodRenderParams;
    vec4 ExternalLodCellOrigins[1024];
};

layout (location = 0) in vec4 vertexColor;
layout (location = 1) in vec3 vertexWorldPos;
layout (location = 2) in vec4 vertexPackedPos;

layout (location = 0) out vec4 fragColor;

void main() {
    fragColor = vertexColor;
}
