#version 450

layout(binding = 1) uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

const float GOLDEN = 2.39996323;

vec2 vogel(int i, int n) {
    float r = sqrt((float(i) + 0.5) / float(n));
    float theta = float(i) * GOLDEN;
    return r * vec2(cos(theta), sin(theta));
}

void main() {
    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
    vec3 sum = texture(Sampler0, texCoord).rgb;
    float wSum = 1.0;
    for (int i = 0; i < 16; i++) {
        vec2 o = vogel(i, 16) * texel * 9.0;
        sum += texture(Sampler0, texCoord + o).rgb;
        wSum += 1.0;
    }
    fragColor = vec4(sum / wSum, 1.0);
}
