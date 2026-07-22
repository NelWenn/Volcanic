package net.vulkanmod.render.ctm;

public final class WeightedPicker {
    private WeightedPicker() {}

    public static int pick(int[] weights, long seed) {
        if (weights == null || weights.length == 0) return 0;
        int total = 0;
        for (int w : weights) total += Math.max(0, w);
        if (total <= 0) return 0;
        long h = seed * 0x9E3779B97F4A7C15L;
        h ^= (h >>> 32);
        int r = (int) Math.floorMod(h, total);
        for (int i = 0; i < weights.length; i++) {
            r -= Math.max(0, weights[i]);
            if (r < 0) return i;
        }
        return weights.length - 1;
    }
}
