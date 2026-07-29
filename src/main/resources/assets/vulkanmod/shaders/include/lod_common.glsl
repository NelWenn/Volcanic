vec3 lod_geo_normal(vec3 camRelPos) {
    vec3 n = normalize(cross(dFdx(camRelPos), dFdy(camRelPos)));
    return dot(n, camRelPos) > 0.0 ? -n : n;
}

vec3 lod_atmosphere(vec3 color, float dist, vec3 fogColor, float fogStart, float fogEnd) {
    return mix(color, fogColor, smoothstep(fogStart, fogEnd, dist));
}
