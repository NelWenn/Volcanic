package net.vulkanmod.config.ui.core;

import java.util.Random;

public final class EmberField {
    public static final int SPARKS = 34;
    private static final float RISE_MIN = 7.0f;
    private static final float RISE_MAX = 20.0f;
    private static final float SWAY_PX = 5.0f;
    private static final float PEAK_ALPHA = 0.42f;
    private static final int HOT = 0xFFB05A;
    private static final int COLD = 0xC2440E;

    private final float[] x = new float[SPARKS];
    private final float[] life = new float[SPARKS];
    private final float[] rise = new float[SPARKS];
    private final float[] phase = new float[SPARKS];
    private final float[] swaySpeed = new float[SPARKS];
    private final int[] size = new int[SPARKS];
    private final Random random;
    private float clock;

    public EmberField(long seed) {
        this.random = new Random(seed);
        for (int index = 0; index < SPARKS; index++) {
            respawn(index);
            life[index] = random.nextFloat();
        }
    }

    public void advance(long deltaMs, int height) {
        if (deltaMs < 0) {
            throw new IllegalArgumentException("deltaMs must not be negative: " + deltaMs);
        }
        if (height <= 0 || deltaMs == 0L) {
            return;
        }
        float seconds = Math.min(deltaMs, 100L) / 1000.0f;
        this.clock += seconds;
        for (int index = 0; index < SPARKS; index++) {
            life[index] += rise[index] * seconds / height;
            if (life[index] >= 1.0f) {
                respawn(index);
            }
        }
    }

    public int xOf(int index, Rect area) {
        float sway = (float) Math.sin(clock * swaySpeed[index] + phase[index]) * SWAY_PX;
        return area.x() + Math.round(x[index] * area.width() + sway);
    }

    public int yOf(int index, Rect area) {
        return area.bottom() - Math.round(life[index] * area.height());
    }

    public int sizeOf(int index) {
        return size[index];
    }

    public int colorOf(int index) {
        float heat = life[index];
        float flicker = 0.65f + 0.35f * (float) Math.abs(Math.sin(clock * 2.4f + phase[index]));
        float visible = (1.0f - heat) * flicker * PEAK_ALPHA;
        int argb = Motion.blend(0xFF000000 | HOT, 0xFF000000 | COLD, heat);
        return Motion.fade(argb, visible);
    }

    private void respawn(int index) {
        x[index] = random.nextFloat();
        life[index] = 0.0f;
        rise[index] = RISE_MIN + random.nextFloat() * (RISE_MAX - RISE_MIN);
        phase[index] = random.nextFloat() * 6.2831855f;
        swaySpeed[index] = 0.35f + random.nextFloat() * 0.5f;
        size[index] = random.nextInt(10) == 0 ? 2 : 1;
    }
}
