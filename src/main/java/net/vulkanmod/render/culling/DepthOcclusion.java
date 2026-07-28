package net.vulkanmod.render.culling;

import net.vulkanmod.render.DepthSnapshot;

public final class DepthOcclusion {

    private static float[] grid;
    private static int width;
    private static int height;
    private static float[] m;
    private static double camX;
    private static double camY;
    private static double camZ;

    private DepthOcclusion() {
    }

    public static void refresh() {
        grid = null;
        if (!DepthSnapshot.available()) {
            return;
        }
        float[] g = DepthSnapshot.depthGrid();
        float[] vp = DepthSnapshot.viewProj();
        double[] cam = DepthSnapshot.cameraPos();
        int w = DepthSnapshot.gridWidth();
        int h = DepthSnapshot.gridHeight();
        if (g == null || vp == null || cam == null || w <= 0 || h <= 0
                || g.length < w * h || vp.length < 16 || cam.length < 3) {
            return;
        }
        grid = g;
        width = w;
        height = h;
        m = vp;
        camX = cam[0];
        camY = cam[1];
        camZ = cam[2];
    }

    public static boolean active() {
        return grid != null;
    }

    public static boolean hidden(double minX, double minY, double minZ,
                                 double maxX, double maxY, double maxZ, double inflate) {
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
        for (int c = 0; c < 8; c++) {
            double x = ((c & 1) == 0 ? minX : maxX) - camX;
            double y = ((c & 2) == 0 ? minY : maxY) - camY;
            double z = ((c & 4) == 0 ? minZ : maxZ) - camZ;
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
        double d = minDepth - 0.0005;
        int tx0 = Math.max((int) Math.floor((Math.max(loX, -1.0) * 0.5 + 0.5) * width), 0);
        int tx1 = Math.min((int) Math.ceil((Math.min(hiX, 1.0) * 0.5 + 0.5) * width) - 1, width - 1);
        int ty0 = Math.max((int) Math.floor((0.5 - Math.min(hiY, 1.0) * 0.5) * height), 0);
        int ty1 = Math.min((int) Math.ceil((0.5 - Math.max(loY, -1.0) * 0.5) * height) - 1, height - 1);
        if (tx1 < tx0 || ty1 < ty0) {
            return false;
        }
        for (int ty = ty0; ty <= ty1; ty++) {
            int row = ty * width;
            for (int tx = tx0; tx <= tx1; tx++) {
                float t = grid[row + tx];
                if (t >= 0.9999f || d <= t) {
                    return false;
                }
            }
        }
        return true;
    }
}
