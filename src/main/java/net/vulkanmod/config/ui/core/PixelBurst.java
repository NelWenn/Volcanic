package net.vulkanmod.config.ui.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class PixelBurst {
    public static final int SPARKS = 8;
    public static final int FRAMES = 4;
    public static final int TICK_MS = 50;

    private static final int[] DIR_X = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] DIR_Y = {0, 1, 1, 1, 0, -1, -1, -1};

    private final Map<String, Integer> live = new HashMap<>();
    private long carry;

    public void trigger(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        live.put(key, 0);
    }

    public void advance(long deltaMs) {
        if (deltaMs < 0) {
            throw new IllegalArgumentException("deltaMs must not be negative: " + deltaMs);
        }
        this.carry += Math.min(deltaMs, 250L);
        while (carry >= TICK_MS) {
            carry -= TICK_MS;
            Iterator<Map.Entry<String, Integer>> each = live.entrySet().iterator();
            while (each.hasNext()) {
                Map.Entry<String, Integer> entry = each.next();
                if (entry.getValue() + 1 >= FRAMES) {
                    each.remove();
                } else {
                    entry.setValue(entry.getValue() + 1);
                }
            }
        }
    }

    public int frame(String key) {
        Integer frame = live.get(key);
        return frame == null ? -1 : frame;
    }

    public boolean idle() {
        return live.isEmpty();
    }

    public static int sparkX(int spark, int frame) {
        return DIR_X[Math.floorMod(spark, SPARKS)] * reach(spark, frame);
    }

    public static int sparkY(int spark, int frame) {
        return DIR_Y[Math.floorMod(spark, SPARKS)] * reach(spark, frame) + frame;
    }

    public static int sparkSize(int spark) {
        return spark % 3 == 0 ? 2 : 1;
    }

    private static int reach(int spark, int frame) {
        return 2 + frame * (3 + Math.floorMod(spark, 2));
    }
}
