package net.vulkanmod.render.ctm;

public final class UvRemap {
    private UvRemap() {}

    public static float remap(float u, float origU0, float origU1, float newU0, float newU1) {
        float span = origU1 - origU0;
        float local = Math.abs(span) < 1e-7f ? 0.0f : (u - origU0) / span;
        return newU0 + local * (newU1 - newU0);
    }
}
