#version 450

layout(binding = 0) uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = vec4(texture(Sampler0, texCoord).rgb, 1.0);
}
