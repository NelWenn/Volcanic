package net.vulkanmod.render.culling;

import net.vulkanmod.render.DepthSnapshot;

public final class DepthOcclusion {

    private static final float EMPTY_DEPTH_THRESHOLD = 1.0f - 1.0e-5f;

    private static float[] grid;
    private static int width;
    private static int height;

    private static float[][] mips;
    private static int[] mipW;
    private static int[] mipH;
    private static int mipLevels;

    private static float[] m;
    private static double camX;
    private static double camY;
    private static double camZ;

    private static int frameStamp;
    private static int testedCount;
    private static int culledCount;

    private DepthOcclusion() {
    }

    public static void refresh() {
        frameStamp++;
        grid = null;
        testedCount = 0;
        culledCount = 0;

        if (!DepthSnapshot.available()) {
            return;
        }

        float[] g = DepthSnapshot.depthGrid();
        float[] vp = DepthSnapshot.viewProj();
        double[] cam = DepthSnapshot.cameraPos();
        int w = DepthSnapshot.gridWidth();
        int h = DepthSnapshot.gridHeight();

        if (g == null || vp == null || cam == null
                || w <= 0 || h <= 0
                || g.length < w * h
                || vp.length < 16
                || cam.length < 3) {
            return;
        }

        grid = g;
        width = w;
        height = h;

        m = vp;
        camX = cam[0];
        camY = cam[1];
        camZ = cam[2];

        buildMips();
    }

    private static void buildMips() {
        int levels = 1;
        int w = width;
        int h = height;

        while (w > 6 || h > 6) {
            w = (w + 1) >> 1;
            h = (h + 1) >> 1;
            levels++;
        }

        if (mips == null || mips.length != levels) {
            mips = new float[levels][];
            mipW = new int[levels];
            mipH = new int[levels];
        }

        mips[0] = grid;
        mipW[0] = width;
        mipH[0] = height;

        for (int level = 1; level < levels; level++) {
            int prevW = mipW[level - 1];
            int prevH = mipH[level - 1];
            int curW = (prevW + 1) >> 1;
            int curH = (prevH + 1) >> 1;

            if (mips[level] == null || mips[level].length < curW * curH) {
                mips[level] = new float[curW * curH];
            }

            float[] prev = mips[level - 1];
            float[] cur = mips[level];

            for (int y = 0; y < curH; y++) {
                int y0 = y << 1;
                int y1 = Math.min(y0 + 1, prevH - 1);

                for (int x = 0; x < curW; x++) {
                    int x0 = x << 1;
                    int x1 = Math.min(x0 + 1, prevW - 1);

                    float a = prev[y0 * prevW + x0];
                    float b = prev[y0 * prevW + x1];
                    float c = prev[y1 * prevW + x0];
                    float d = prev[y1 * prevW + x1];

                    cur[y * curW + x] = Math.max(Math.max(a, b), Math.max(c, d));
                }
            }

            mipW[level] = curW;
            mipH[level] = curH;
        }

        mipLevels = levels;
    }

    public static boolean active() {
        return grid != null;
    }

    public static int frameStamp() {
        return frameStamp;
    }

    public static void recordTest(boolean culled) {
        testedCount++;
        if (culled) {
            culledCount++;
        }
    }

    public static int getTestedCount() {
        return testedCount;
    }

    public static int getCulledCount() {
        return culledCount;
    }

    public static boolean hidden(double minX, double minY, double minZ,
                                 double maxX, double maxY, double maxZ,
                                 double inflate) {
        if (grid == null) {
            return false;
        }

        minX -= inflate;
        minY -= inflate;
        minZ -= inflate;
        maxX += inflate;
        maxY += inflate;
        maxZ += inflate;

        double loX = Double.POSITIVE_INFINITY;
        double loY = Double.POSITIVE_INFINITY;
        double hiX = Double.NEGATIVE_INFINITY;
        double hiY = Double.NEGATIVE_INFINITY;
        double minDepth = Double.POSITIVE_INFINITY;

        for (int corner = 0; corner < 8; corner++) {
            double x = ((corner & 1) == 0 ? minX : maxX) - camX;
            double y = ((corner & 2) == 0 ? minY : maxY) - camY;
            double z = ((corner & 4) == 0 ? minZ : maxZ) - camZ;

            double cw = m[3] * x + m[7] * y + m[11] * z + m[15];
            if (cw <= 1.0e-4) {
                return false;
            }

            double cx = (m[0] * x + m[4] * y + m[8] * z + m[12]) / cw;
            double cy = (m[1] * x + m[5] * y + m[9] * z + m[13]) / cw;
            double cz = (m[2] * x + m[6] * y + m[10] * z + m[14]) / cw;

            loX = Math.min(loX, cx);
            hiX = Math.max(hiX, cx);
            loY = Math.min(loY, cy);
            hiY = Math.max(hiY, cy);
            minDepth = Math.min(minDepth, cz);
        }

        if (hiX < -1.0 || loX > 1.0 || hiY < -1.0 || loY > 1.0) {
            return false;
        }

        double depth = minDepth - 0.0005;

        int tx0 = Math.max((int) Math.floor((Math.max(loX, -1.0) * 0.5 + 0.5) * width), 0);
        int tx1 = Math.min((int) Math.ceil((Math.min(hiX, 1.0) * 0.5 + 0.5) * width) - 1, width - 1);

        int ty0 = Math.max((int) Math.floor((0.5 - Math.min(hiY, 1.0) * 0.5) * height), 0);
        int ty1 = Math.min((int) Math.ceil((0.5 - Math.max(loY, -1.0) * 0.5) * height) - 1, height - 1);

        if (tx1 < tx0 || ty1 < ty0) {
            return false;
        }

        int level = 0;
        while (level < mipLevels - 1
                && (((tx1 >> level) - (tx0 >> level) > 3)
                || ((ty1 >> level) - (ty0 >> level) > 3))) {
            level++;
        }

        int mipWidth = mipW[level];
        int lx0 = tx0 >> level;
        int lx1 = Math.min(tx1 >> level, mipWidth - 1);
        int ly0 = ty0 >> level;
        int ly1 = Math.min(ty1 >> level, mipH[level] - 1);

        float[] data = mips[level];

        for (int y = ly0; y <= ly1; y++) {
            int row = y * mipWidth;

            for (int x = lx0; x <= lx1; x++) {
                float sample = data[row + x];

                if (sample >= EMPTY_DEPTH_THRESHOLD || depth <= sample) {
                    return false;
                }
            }
        }

        return true;
    }
}