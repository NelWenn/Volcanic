#version 460

#include "light.glsl"
#include "fog.glsl"
#include "wave.glsl"

layout (binding = 0) uniform UniformBufferObject {
    mat4 MVP;
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
layout (location = 3) out vec3 worldPos;
layout (location = 4) flat out vec4 tileAM;

layout (location = 0) in ivec4 Position;
layout (location = 1) in vec4 Color;
layout (location = 2) in uvec2 UV0;
layout (location = 3) in ivec2 UV2;

const float UV_INV = 1.0 / 32768.0;
const vec3 POSITION_INV = vec3(1.0 / 2048.0);

void main() {
    const vec3 baseOffset = bitfieldExtract(ivec3(gl_InstanceIndex) >> ivec3(0, 16, 8), 0, 8);
    vec4 pos = vec4(fma(Position.xyz, POSITION_INV, ChunkOffset + baseOffset), 1.0);

    int waveCode = int(Position.a) & 0xF;
    float heightWeight = float((int(Position.a) >> 8) & 0xF) / 15.0;
    vec3 wavePos = pos.xyz + CameraWorldPos;
    pos.xyz = volcanic_wave(wavePos, pos.xyz, waveCode, heightWeight, WindTime, WindStrength);

    gl_Position = MVP * pos;

    vertexDistance = fog_distance(pos.xyz, 0);
    vertexColor = Color * sample_lightmap2(Sampler2, Position.a);
    texCoord0 = UV0 * UV_INV;
    worldPos = pos.xyz;

    uint packed = (uint(UV2.y) << 16) | (uint(UV2.x) & 0xFFFFu);
    vec2 midCoord = vec2(float(packed & 0xFFFu), float((packed >> 12u) & 0xFFFu)) / 4095.0;
    vec2 texMinMid = texCoord0 - midCoord;
    tileAM.zw = abs(texMinMid) * 2.0;
    tileAM.xy = min(texCoord0, midCoord - texMinMid);
}
