#version 450

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 Local;
layout(location = 3) in ivec2 Extent;
layout(location = 4) in ivec2 Radii;
layout(location = 5) in vec3 Layer;

layout(binding = 0) uniform UniformBufferObject {
    mat4 MVP;
};

layout(location = 0) out vec4 layerColor;
layout(location = 1) out vec2 localPos;
layout(location = 2) out vec2 halfExtent;
layout(location = 3) out vec2 shapeRadii;
layout(location = 4) out vec3 layerMode;

void main() {
    gl_Position = MVP * vec4(Position, 1.0);

    layerColor = Color;
    localPos = Local;
    halfExtent = vec2(Extent) * 0.5;
    shapeRadii = vec2(Radii);
    layerMode = Layer;
}
