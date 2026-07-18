#version 460

#include "wave.glsl"

layout (binding = 0) uniform UniformBufferObject {
    mat4 MVP;
    float HeldLightLevel;
    float WindTime;
    float WindStrength;
    vec3 CameraWorldPos;
};

layout (push_constant) uniform pushConstant {
    vec3 ChunkOffset;
};

layout (location = 0) out vec4 vertexColor;
layout (location = 1) out vec2 texCoord0;

layout (location = 0) in ivec4 Position;
layout (location = 1) in vec4 Color;
layout (location = 2) in uvec2 UV0;

const float UV_INV = 1.0 / 32768.0;
const vec3 POSITION_INV = vec3(1.0 / 2048.0);

void main() {
    const vec3 baseOffset = bitfieldExtract(ivec3(gl_InstanceIndex) >> ivec3(0, 16, 8), 0, 8);
    vec4 pos = vec4(fma(Position.xyz, POSITION_INV, ChunkOffset + baseOffset), 1.0);

    int waveCode = int(Position.a) & 0xF;
    float heightWeight = float((int(Position.a) >> 8) & 0xF) / 15.0;
    vec3 worldPos = pos.xyz + CameraWorldPos;
    pos.xyz = volcanic_wave(worldPos, pos.xyz, waveCode, heightWeight, WindTime, WindStrength);

    gl_Position = MVP * pos;

    vertexColor = Color;
    texCoord0 = UV0 * UV_INV;
}
