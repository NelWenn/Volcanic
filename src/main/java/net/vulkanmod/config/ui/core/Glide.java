package net.vulkanmod.config.ui.core;

public final class Glide {
    private static final float LN2 = 0.6931472f;
    private static final float SETTLE = 0.35f;

    private final float halfLifeMs;
    private float value;
    private boolean primed;

    public Glide(float halfLifeMs) {
        if (halfLifeMs <= 0.0f) {
            throw new IllegalArgumentException("halfLifeMs must be positive: " + halfLifeMs);
        }
        this.halfLifeMs = halfLifeMs;
    }

    public float advance(float target, long deltaMs) {
        if (deltaMs < 0) {
            throw new IllegalArgumentException("deltaMs must not be negative: " + deltaMs);
        }
        if (!primed) {
            this.primed = true;
            this.value = target;
            return this.value;
        }
        if (deltaMs == 0L || value == target) {
            return this.value;
        }
        float k = 1.0f - (float) Math.exp(-deltaMs * LN2 / halfLifeMs);
        this.value += (target - value) * k;
        if (Math.abs(target - value) < SETTLE) {
            this.value = target;
        }
        return this.value;
    }

    public float value() {
        return this.value;
    }

    public int rendered() {
        return Math.round(this.value);
    }

    public boolean settled(float target) {
        return primed && value == target;
    }

    public void jumpTo(float target) {
        this.primed = true;
        this.value = target;
    }
}
