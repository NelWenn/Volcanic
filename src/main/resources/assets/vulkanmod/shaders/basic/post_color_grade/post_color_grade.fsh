#version 450

// Post color grade: exposure / contrast / saturation / temperature.

layout(binding = 0) uniform UBO {
    float CgExposure;     // 0.5 .. 2.0
    float CgContrast;     // 0.5 .. 2.0
    float CgSaturation;   // 0.0 .. 2.0
    float CgTemperature;  // -1.0 .. 1.0 (cool .. warm)
};

layout(binding = 1) uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec3 c = texture(Sampler0, texCoord).rgb;

    c *= CgExposure;
    c = (c - 0.5) * CgContrast + 0.5;
    float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));  // Rec.709 luma
    c = mix(vec3(luma), c, CgSaturation);
    c += vec3(CgTemperature, 0.0, -CgTemperature) * 0.15;

    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
