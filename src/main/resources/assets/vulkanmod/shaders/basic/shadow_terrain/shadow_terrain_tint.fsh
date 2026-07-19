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

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    vec3 albedo = tex.rgb * vertexColor.rgb;
    float coverage = tex.a * vertexColor.a;
    fragColor = vec4(mix(vec3(1.0), albedo, coverage), 1.0);
}
