vec3 volcanic_wave(vec3 worldPos, vec3 relPos, int code, float heightWeight, float time, float strength) {
    if (code == 0 || strength <= 0.0) return relPos;
    vec2 wind = normalize(vec2(0.8, 0.55));
    float d = dot(worldPos.xz, wind);
    float gust = 0.65 + 0.35 * sin(d * 0.07 - time * 0.5);
    float sway = sin(d * 0.45 - time * 1.6) * gust;
    sway += sin(d * 0.9 - time * 2.7 + 1.3) * 0.25;
    float anchor = (code == 2) ? 0.5 : heightWeight * heightWeight;
    float amp = strength * ((code == 2) ? 0.05 : 0.12);
    vec2 off = wind * sway * amp * anchor;
    return relPos + vec3(off.x, 0.0, off.y);
}
