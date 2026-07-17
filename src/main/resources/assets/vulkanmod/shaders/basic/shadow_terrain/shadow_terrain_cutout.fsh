#version 450

layout(binding = 2) uniform sampler2D Sampler0;

layout(binding = 1) uniform UBO {
    vec4 FogColor;
    float FogStart;
    float FogEnd;
    float AlphaCutout;
};

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 texCoord0;

void main() {
    // Cutout alpha test only; depth of surviving fragments is the output.
    float alpha = texture(Sampler0, texCoord0).a * vertexColor.a;
    if (alpha < AlphaCutout) {
        discard;
    }
}
