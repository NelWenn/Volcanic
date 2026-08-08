#version 150

in vec3 Position;
in vec4 Color;
in vec2 Local;
in ivec2 Extent;
in ivec2 Radii;
in vec3 Layer;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 layerColor;
out vec2 localPos;
out vec2 halfExtent;
out vec2 shapeRadii;
out vec3 layerMode;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    layerColor = Color;
    localPos = Local;
    halfExtent = vec2(Extent) * 0.5;
    shapeRadii = vec2(Radii);
    layerMode = Layer;
}
