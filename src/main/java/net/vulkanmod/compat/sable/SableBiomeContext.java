package net.vulkanmod.compat.sable;

public final class SableBiomeContext {

    private static volatile int[] pending;

    private SableBiomeContext() {
    }

    public static void setPending(int grass, int foliage, int water) {
        pending = new int[]{grass, foliage, water};
    }

    public static int[] pending() {
        return pending;
    }
}
