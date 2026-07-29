vec3 lod_geo_normal(vec3 camRelPos) {
    vec3 n = normalize(cross(dFdx(camRelPos), dFdy(camRelPos)));
    return dot(n, camRelPos) > 0.0 ? -n : n;
}
