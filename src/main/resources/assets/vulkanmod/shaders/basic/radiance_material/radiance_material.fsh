#version 450

layout(binding = 2) uniform sampler2D Sampler0;

layout(binding = 1) uniform UBO {
    vec4 FogColor;
    float FogStart;
    float FogEnd;
    float AlphaCutout;
};

layout(location = 0) in vec2 texCoord0;
layout(location = 1) flat in int materialId;

layout(location = 0) out vec4 fragColor;

void main() {
    float alpha = texture(Sampler0, texCoord0).a;
    if (materialId == 0 && alpha < AlphaCutout) {
        discard;
    }
    fragColor = vec4(float(materialId) / 255.0, 0.0, 0.0, 1.0);
}
