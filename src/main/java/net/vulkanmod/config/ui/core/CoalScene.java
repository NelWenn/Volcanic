package net.vulkanmod.config.ui.core;

import java.util.Random;

public final class CoalScene {
    public static final int SCALE = 2;
    public static final int GLOW_SITES = 52;
    public static final int PARTICLES = 34;
    private static final float GLOW_ALPHA = 0.34f;
    private static final int GLOW_CORE = 0xFFD022;
    private static final int GLOW_EDGE = 0xFD8515;
    private static final float RISE_MIN = 9.0f;
    private static final float RISE_MAX = 26.0f;
    private static final float SWAY_PX = 6.0f;
    private static final float SPARK_ALPHA = 0.55f;
    private static final int SPARK_HOT = 0xFAC856;
    private static final int SPARK_COLD = 0xC2440E;

    private final float[] originX = new float[PARTICLES];
    private final float[] originY = new float[PARTICLES];
    private final float[] life = new float[PARTICLES];
    private final float[] rise = new float[PARTICLES];
    private final float[] phase = new float[PARTICLES];
    private final float[] sway = new float[PARTICLES];
    private final int[] span = new int[PARTICLES];
    private final Random random;
    private float clock;

    public CoalScene(long seed) {
        this.random = new Random(seed);
        for (int index = 0; index < PARTICLES; index++) {
            respawn(index);
            life[index] = random.nextFloat();
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

    public void advance(long deltaMs, Rect content) {
        if (deltaMs < 0) {
            throw new IllegalArgumentException("deltaMs must not be negative: " + deltaMs);
        }
        if (content.height() <= 0 || deltaMs == 0L) {
            return;
        }
        float seconds = Math.min(deltaMs, 100L) / 1000.0f;
        this.clock += seconds;
        float reach = Math.max(1, content.height() - bedHeight() / 2);
        for (int index = 0; index < PARTICLES; index++) {
            life[index] += rise[index] * seconds / reach;
            if (life[index] >= 1.0f) {
                respawn(index);
            }
        }
    }

    public int glowX(int site, int tile, Rect content) {
        return content.x() + tile * tileWidth() + CoalArt.siteX(site) * SCALE - glowSize(site) / 2;
    }

    public int glowY(int site, Rect content) {
        return content.bottom() - bedHeight() + CoalArt.siteY(site) * SCALE - glowSize(site) / 2;
    }

    public int glowSize(int site) {
        return 3 + (CoalArt.siteHeat(site) > 200 ? 3 : CoalArt.siteHeat(site) > 150 ? 2 : 0);
    }

    public int glowArgb(int site) {
        float heat = Math.min(1.0f, CoalArt.siteHeat(site) / 255.0f);
        float beat = 0.45f + 0.55f * (float) Math.sin(clock * (0.7f + 0.9f * fraction(site))
                + fraction(site + 31) * 6.2831855f);
        return Motion.fade(Motion.blend(GLOW_EDGE | 0xFF000000, GLOW_CORE | 0xFF000000, heat),
                GLOW_ALPHA * heat * beat);
    }

    public int particleX(int index, Rect content) {
        float drift = (float) Math.sin(clock * sway[index] + phase[index]) * SWAY_PX;
        int x = Math.round(content.x() + originX[index] * Math.max(1, content.width()) + drift);
        return Math.max(content.x(), Math.min(content.right() - 1, x));
    }

    public int particleY(int index, Rect content) {
        float reach = Math.max(1, content.height() - bedHeight() / 2);
        int y = Math.round(originY[index] * content.height() - life[index] * reach);
        return content.y() + Math.max(0, Math.min(content.height() - 1, y));
    }

    public int particleSize(int index) {
        return span[index];
    }

    public int particleArgb(int index) {
        float heat = life[index];
        float flicker = 0.6f + 0.4f * (float) Math.abs(Math.sin(clock * 2.6f + phase[index]));
        return Motion.fade(Motion.blend(SPARK_HOT | 0xFF000000, SPARK_COLD | 0xFF000000, heat),
                (1.0f - heat) * flicker * SPARK_ALPHA);
    }

    private void respawn(int index) {
        int site = random.nextInt(CoalArt.siteCount());
        int tile = random.nextInt(4);
        originX[index] = Math.min(0.999f,
                (tile * CoalArt.TEX_W + CoalArt.siteX(site)) * SCALE / (float) (tileWidth() * 4));
        originY[index] = 1.0f - (CoalArt.TEX_H - CoalArt.siteY(site)) * SCALE / (float) (bedHeight() * 4);
        life[index] = 0.0f;
        rise[index] = RISE_MIN + random.nextFloat() * (RISE_MAX - RISE_MIN);
        phase[index] = random.nextFloat() * 6.2831855f;
        sway[index] = 0.4f + random.nextFloat() * 0.6f;
        span[index] = random.nextInt(9) == 0 ? 2 : 1;
    }

    private float fraction(int site) {
        long h = site * 0xC2B2AE3D27D4EB4FL + 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return ((h >>> 40) & 0xFFFFFF) / (float) 0x1000000;
    }
}
