#version 460

#include "light.glsl"
#include "fog.glsl"
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

layout (binding = 3) uniform sampler2D Sampler2;


layout (location = 0) out float vertexDistance;
layout (location = 1) out vec4 vertexColor;
layout (location = 2) out vec2 texCoord0;

layout (location = 0) in ivec4 Position;
layout (location = 1) in vec4 Color;
layout (location = 2) in uvec2 UV0;

const float UV_INV = 1.0 / 32768.0;
const vec3 POSITION_INV = vec3(1.0 / 2048.0);
const vec3 POSITION_OFFSET = vec3(4.0);

vec4 sample_lightmap_boosted(sampler2D lightMap, uint uv, float heldBoost) {
    const ivec2 lm = ivec2(bitfieldExtract(uv, 4, 4), bitfieldExtract(uv, 12, 4));
    float blockLm = max(float(lm.x), heldBoost);
    int b0 = int(floor(blockLm));
    int b1 = min(b0 + 1, 15);
    float fr = blockLm - float(b0);
    vec4 c0 = texelFetch(lightMap, ivec2(b0, lm.y), 0);
    vec4 c1 = texelFetch(lightMap, ivec2(b1, lm.y), 0);
    return mix(c0, c1, fr);
}

void main() {
    const vec3 baseOffset = bitfieldExtract(ivec3(gl_InstanceIndex) >> ivec3(0, 16, 8), 0, 8);
    vec4 pos = vec4(fma(Position.xyz, POSITION_INV, ChunkOffset + baseOffset), 1.0);

    int waveCode = int(Position.a) & 0xF;
    float heightWeight = float((int(Position.a) >> 8) & 0xF) / 15.0;
    vec3 worldPos = pos.xyz + CameraWorldPos;
    pos.xyz = volcanic_wave(worldPos, pos.xyz, waveCode, heightWeight, WindTime, WindStrength);

    gl_Position = MVP * pos;

    float heldDist = length(pos.xyz);
    float heldBoost = clamp(HeldLightLevel - heldDist * 1.15, 0.0, 15.0);

    vertexDistance = fog_distance(pos.xyz, 0);
    vertexColor = Color * sample_lightmap_boosted(Sampler2, Position.a, heldBoost);
    texCoord0 = UV0 * UV_INV;
}