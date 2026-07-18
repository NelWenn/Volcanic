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

// stained/tinted glass transmittance: opaque colour tints, transparent lets light through (white)
void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    float a = tex.a * vertexColor.a;
    if (a < 0.1) discard;   // transparent parts (glass pane borders) neither occlude nor tint
    vec3 dye = tex.rgb * vertexColor.rgb;
    // punch up saturation so the alpha-blended dye doesn't wash out to a pale pastel
    float lum = dot(dye, vec3(0.299, 0.587, 0.114));
    vec3 vivid = clamp(lum + (dye - lum) * 1.5, 0.0, 1.0);
    fragColor = vec4(mix(vec3(1.0), vivid, clamp(a * 1.5, 0.0, 1.0)), 1.0);
}
