#version 150

in vec4 layerColor;
in vec2 localPos;
in vec2 halfExtent;
in vec2 shapeRadii;
in vec3 layerMode;

uniform vec4 ColorModulator;

out vec4 fragColor;

const float BORDER_WIDTH = 1.0;

float roundedBoxDistance(vec2 point, vec2 extent, float radius) {
    vec2 q = abs(point) - extent + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - radius;
}

void main() {
    float cornerRadius = clamp(shapeRadii.x, 0.0, min(halfExtent.x, halfExtent.y));
    float glowRadius = shapeRadii.y;

    float dist = roundedBoxDistance(localPos, halfExtent, cornerRadius);
    float edge = max(fwidth(dist), 0.0001);

    float shapeMask = 1.0 - smoothstep(-edge, edge, dist);
    float innerMask = 1.0 - smoothstep(-edge, edge, dist + BORDER_WIDTH);
    float borderMask = clamp(shapeMask - innerMask, 0.0, 1.0);
    float outside = smoothstep(-edge, edge, dist);

    float glowMask = 0.0;
    if (glowRadius > 0.0) {
        float falloff = 1.0 - smoothstep(0.0, glowRadius, max(dist, 0.0));
        glowMask = falloff * falloff * outside;
    }

    float coverage = layerMode.x * glowMask + layerMode.y * shapeMask + layerMode.z * borderMask;
    float alpha = layerColor.a * coverage;

    if (alpha <= 0.0) {
        discard;
    }

    fragColor = vec4(layerColor.rgb, alpha) * ColorModulator;
}
