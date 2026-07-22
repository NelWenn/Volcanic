package net.vulkanmod.render.ctm;

public final class PositionRng {
    private PositionRng() {}

    public static long seed(int x, int y, int z, int face) {
        long h = 0x100000001B3L;
        h = (h ^ (x * 3129871L)) * 116129781L;
        h = (h ^ (y * 116129781L)) * 116129781L;
        h = (h ^ (z * 3129871L)) * 116129781L;
        h = (h ^ (face * 0x9E3779B1L)) * 116129781L;
        return h ^ (h >>> 29);
    }
}
