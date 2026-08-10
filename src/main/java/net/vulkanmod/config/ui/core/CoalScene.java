package net.vulkanmod.config.ui.core;

import java.util.Random;

public final class CoalScene {
    public static final int SCALE = 2;
    public static final int GLOW_SITES = 56;

    public static final int SPARK = 0;
    public static final int LAVA = 1;
    public static final int SMOKE = 2;

    public static final int SPARKS = 24;
    public static final int LAVAS = 9;
    public static final int SMOKES = 8;
    public static final int PARTICLES = SPARKS + LAVAS + SMOKES;

    private static final float GLOW_ALPHA = 0.30f;
    private static final int GLOW_CORE = 0xFFD022;
    private static final int GLOW_EDGE = 0xFD8515;

    private static final float GRAVITY = 300.0f;
    private static final int SPARK_HOT = 0xFFE9A8;
    private static final int SPARK_COLD = 0xFF6A12;
    private static final int LAVA_HOT = 0xFFC24A;
    private static final int LAVA_COLD = 0x6E1904;
    private static final int SMOKE_WARM = 0x6A5B54;
    private static final int SMOKE_COLD = 0x322B28;

    private final float[] px = new float[PARTICLES];
    private final float[] py = new float[PARTICLES];
    private final float[] vx = new float[PARTICLES];
    private final float[] vy = new float[PARTICLES];
    private final float[] age = new float[PARTICLES];
    private final float[] span = new float[PARTICLES];
    private final float[] phase = new float[PARTICLES];
    private final Random random;
    private float clock;

    public CoalScene(long seed) {
        this.random = new Random(seed);
        for (int index = 0; index < PARTICLES; index++) {
            spawn(index, new Rect(0, 0, tileWidth(), bedHeight() * 4));
            age[index] = random.nextFloat() * span[index];
        }
    }

    public int bedHeight() {
        return CoalArt.TEX_H * SCALE;
    }

    public int tileWidth() {
        return CoalArt.TEX_W * SCALE;
    }

    public int tiles(Rect content) {
        return content.width() <= 0 ? 0 : (content.width() + tileWidth() - 1) / tileWidth();
    }

    public Rect tileRect(int tile, Rect content) {
        return new Rect(content.x() + tile * tileWidth(), content.bottom() - bedHeight(),
                tileWidth(), bedHeight());
    }

    public int glowSize() {
        return SCALE;
    }

    public int glowX(int site, int tile, Rect content) {
        return content.x() + tile * tileWidth() + CoalArt.siteX(site) * SCALE;
    }

    public int glowY(int site, Rect content) {
        return content.bottom() - bedHeight() + CoalArt.siteY(site) * SCALE;
    }

    public int glowArgb(int site) {
        float heat = Math.min(1.0f, CoalArt.siteHeat(site) / 255.0f);
        float beat = 0.40f + 0.60f * (float) Math.sin(clock * (0.7f + 0.9f * fraction(site))
                + fraction(site + 31) * 6.2831855f);
        return Motion.fade(Motion.blend(GLOW_EDGE | 0xFF000000, GLOW_CORE | 0xFF000000, heat),
                GLOW_ALPHA * heat * beat);
    }

    public void advance(long deltaMs, Rect content) {
        if (deltaMs < 0) {
            throw new IllegalArgumentException("deltaMs must not be negative: " + deltaMs);
        }
        if (content.width() <= 0 || content.height() <= 0 || deltaMs == 0L) {
            return;
        }
        float seconds = Math.min(deltaMs, 100L) / 1000.0f;
        this.clock += seconds;
        for (int index = 0; index < PARTICLES; index++) {
            age[index] += seconds;
            px[index] += vx[index] * seconds;
            py[index] += vy[index] * seconds;
            if (kindOf(index) == LAVA) {
                vy[index] -= GRAVITY * seconds;
            }
            if (expired(index, content)) {
                spawn(index, content);
            }
        }
    }

    public int kindOf(int index) {
        return index < SPARKS ? SPARK : index < SPARKS + LAVAS ? LAVA : SMOKE;
    }

    public int xOf(int index, Rect content) {
        float drift = kindOf(index) == SMOKE
                ? (float) Math.sin(clock * 0.5f + phase[index]) * 7.0f : 0.0f;
        return content.x() + Math.round(px[index] + drift);
    }

    public int yOf(int index, Rect content) {
        return content.bottom() - Math.round(py[index]);
    }

    public int sizeOf(int index) {
        return switch (kindOf(index)) {
            case SPARK -> 1;
            case LAVA -> py[index] > bedHeight() ? 2 : 3;
            default -> 2 + Math.round(life(index) * 4.0f);
        };
    }

    public int argbOf(int index) {
        float t = life(index);
        return switch (kindOf(index)) {
            case SPARK -> Motion.fade(Motion.blend(SPARK_HOT | 0xFF000000, SPARK_COLD | 0xFF000000, t),
                    (1.0f - t * t) * 0.95f);
            case LAVA -> Motion.fade(Motion.blend(LAVA_HOT | 0xFF000000, LAVA_COLD | 0xFF000000, t * t),
                    Math.min(1.0f, 2.6f - 2.2f * t) * 0.9f);
            default -> Motion.fade(Motion.blend(SMOKE_WARM | 0xFF000000, SMOKE_COLD | 0xFF000000, t),
                    (float) Math.sin(t * 3.1415927f) * 0.20f);
        };
    }

    private float life(int index) {
        return Math.min(1.0f, age[index] / span[index]);
    }

    private boolean expired(int index, Rect content) {
        if (age[index] >= span[index]) {
            return true;
        }
        if (py[index] < 0.0f || py[index] > content.height() + bedHeight()) {
            return true;
        }
        return px[index] < -tileWidth() || px[index] > content.width() + tileWidth();
    }

    private void spawn(int index, Rect content) {
        int site = random.nextInt(CoalArt.siteCount());
        int tile = random.nextInt(Math.max(1, tiles(content)));
        this.px[index] = tile * tileWidth() + CoalArt.siteX(site) * SCALE;
        this.py[index] = (CoalArt.TEX_H - CoalArt.siteY(site)) * SCALE;
        this.age[index] = 0.0f;
        this.phase[index] = random.nextFloat() * 6.2831855f;
        switch (kindOf(index)) {
            case SPARK -> {
                this.vx[index] = (random.nextFloat() - 0.5f) * 22.0f;
                this.vy[index] = 34.0f + random.nextFloat() * 46.0f;
                this.span[index] = 0.45f + random.nextFloat() * 0.55f;
            }
            case LAVA -> {
                this.vx[index] = (random.nextFloat() - 0.5f) * 70.0f;
                this.vy[index] = 120.0f + random.nextFloat() * 90.0f;
                this.span[index] = 1.5f + random.nextFloat() * 0.6f;
            }
            default -> {
                this.vx[index] = (random.nextFloat() - 0.5f) * 6.0f;
                this.vy[index] = 11.0f + random.nextFloat() * 12.0f;
                this.span[index] = 3.0f + random.nextFloat() * 2.4f;
            }
        }
    }

    private float fraction(int site) {
        long h = site * 0xC2B2AE3D27D4EB4FL + 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return ((h >>> 40) & 0xFFFFFF) / (float) 0x1000000;
    }
}
