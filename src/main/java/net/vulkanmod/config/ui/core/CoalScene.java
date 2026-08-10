package net.vulkanmod.config.ui.core;

import java.util.Random;

public final class CoalScene {
    private static final float MAX_HEIGHT_SHARE = 0.5f;
    private static final int REFERENCE_HEIGHT = 112;

    public static final int SPARK = 0;
    public static final int LAVA = 1;
    public static final int SMOKE = 2;

    public static final int SPARKS = 3;
    public static final int LAVAS = 2;
    public static final int SMOKES = 16;
    public static final int ZONES = 8;
    public static final int SMOKE_FRAMES = 12;
    public static final int PARTICLES = SPARKS + LAVAS + SMOKES;

    private static final float WAVE_SPEED = 0.85f;
    private static final int HEAT_HIGH = 0xFFD98C;
    private static final int HEAT_LOW = 0x2A1310;
    private static final float TINT_ALPHA = 0.9f;

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
            spawn(index, new Rect(0, 0, CoalArt.TEX_W * 2, REFERENCE_HEIGHT * 4));
            age[index] = random.nextFloat() * span[index];
        }
    }

    public Rect bedRect(Rect content) {
        if (content.width() <= 0 || content.height() <= 0) {
            return Rect.EMPTY;
        }
        int natural = Math.round(CoalArt.TEX_H * content.width() / (float) CoalArt.TEX_W);
        int height = Math.max(1, Math.min(natural, Math.round(content.height() * MAX_HEIGHT_SHARE)));
        return new Rect(content.x(), content.bottom() - height, content.width(), height);
    }

    public int bedHeight(Rect content) {
        return bedRect(content).height();
    }

    public float particleScale(Rect content) {
        float ratio = bedHeight(content) / (float) REFERENCE_HEIGHT;
        return Math.max(0.7f, Math.min(2.2f, ratio));
    }

    public float zoneHeat(int zone) {
        float phase = zone * (6.2831855f / ZONES);
        float wave = (float) Math.sin(clock * WAVE_SPEED - phase);
        float slow = (float) Math.sin(clock * 0.31f - phase * 0.5f);
        return Math.max(0.0f, Math.min(1.0f, 0.5f + 0.36f * wave + 0.14f * slow));
    }

    public int zoneTint(int zone) {
        return Motion.fade(Motion.blend(HEAT_LOW | 0xFF000000, HEAT_HIGH | 0xFF000000,
                zoneHeat(zone)), TINT_ALPHA);
    }

    public int smokeFrame(int index) {
        return Math.min(SMOKE_FRAMES - 1, (int) (life(index) * SMOKE_FRAMES));
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
            if (age[index] < 0.0f) {
                continue;
            }
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
            case SPARK -> 4;
            case LAVA -> py[index] > REFERENCE_HEIGHT ? 5 : 7;
            default -> 8 + Math.round(life(index) * 14.0f);
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
                    (float) Math.sin(t * 3.1415927f) * 0.32f);
        };
    }

    private float life(int index) {
        return Math.max(0.0f, Math.min(1.0f, age[index] / span[index]));
    }

    public boolean waiting(int index) {
        return age[index] < 0.0f;
    }

    private boolean expired(int index, Rect content) {
        if (age[index] >= span[index]) {
            return true;
        }
        if (py[index] < 0.0f || py[index] > content.height() + REFERENCE_HEIGHT) {
            return true;
        }
        return px[index] < -80.0f || px[index] > content.width() + 80.0f;
    }

    private void spawn(int index, Rect content) {
        int site = random.nextInt(CoalArt.siteCount());
        Rect bed = bedRect(content);
        float across = bed.width() <= 0 ? 1.0f : bed.width() / (float) CoalArt.TEX_W;
        float down = bed.height() <= 0 ? 1.0f : bed.height() / (float) CoalArt.TEX_H;
        this.px[index] = CoalArt.siteX(site) * across;
        this.py[index] = (CoalArt.TEX_H - CoalArt.siteY(site)) * down;
        this.age[index] = 0.0f;
        this.phase[index] = random.nextFloat() * 6.2831855f;
        switch (kindOf(index)) {
            case SPARK -> {
                this.vx[index] = (random.nextFloat() - 0.5f) * 22.0f;
                this.vy[index] = 34.0f + random.nextFloat() * 46.0f;
                this.span[index] = 0.45f + random.nextFloat() * 0.55f;
                this.age[index] = -random.nextFloat() * 3.5f;
            }
            case LAVA -> {
                this.vx[index] = (random.nextFloat() - 0.5f) * 70.0f;
                this.vy[index] = 120.0f + random.nextFloat() * 90.0f;
                this.span[index] = 1.5f + random.nextFloat() * 0.6f;
                this.age[index] = -random.nextFloat() * 5.0f;
            }
            default -> {
                this.vx[index] = (random.nextFloat() - 0.5f) * 7.0f;
                this.vy[index] = 10.0f + random.nextFloat() * 14.0f;
                this.span[index] = 3.4f + random.nextFloat() * 3.0f;
            }
        }
    }

}
