package net.vulkanmod.render.vsr;

import net.vulkanmod.vulkan.util.MappedBuffer;

public final class Vsr {

    public static final int BILINEAR = 0;
    public static final int FSR1 = 1;
    public static final int SHARPEN_ONLY = 2;
    public static final int VTU = 3;

    private static final MappedBuffer INPUT_INFO = new MappedBuffer(4 * Float.BYTES);
    private static final MappedBuffer OUTPUT_INFO = new MappedBuffer(4 * Float.BYTES);
    private static final MappedBuffer UV_BOUNDS = new MappedBuffer(4 * Float.BYTES);
    private static final MappedBuffer PARAMS = new MappedBuffer(4 * Float.BYTES);

    private static int passes;
    private static int lastBackend = -1;
    private static int lastInputWidth, lastInputHeight;
    private static int lastOutputWidth, lastOutputHeight;
    private static float lastSharpness;

    public static int getInputWidth() {
        return lastInputWidth;
    }

    public static int getInputHeight() {
        return lastInputHeight;
    }

    public static int getOutputWidth() {
        return lastOutputWidth;
    }

    public static int getOutputHeight() {
        return lastOutputHeight;
    }

    public static float getSharpness() {
        return lastSharpness;
    }

    public static String backendName(int backend) {
        return switch (backend) {
            case BILINEAR -> "Bilinear";
            case FSR1 -> "FSR 1";
            case SHARPEN_ONLY -> "Sharpen";
            case VTU -> "VTU";
            default -> "off";
        };
    }

    private Vsr() {
    }

    public static int consumePasses() {
        int value = passes;
        passes = 0;
        return value;
    }

    public static int getLastBackend() {
        return lastBackend;
    }

    public static int clampBackend(int backend) {
        return Math.max(BILINEAR, Math.min(VTU, backend));
    }

    public static void update(int inputWidth, int inputHeight, int usedWidth, int usedHeight,
                              int outputWidth, int outputHeight, int backend, float sharpness) {
        float iw = Math.max(1.0f, inputWidth);
        float ih = Math.max(1.0f, inputHeight);
        float ow = Math.max(1.0f, outputWidth);
        float oh = Math.max(1.0f, outputHeight);

        INPUT_INFO.putFloat(0, iw);
        INPUT_INFO.putFloat(4, ih);
        INPUT_INFO.putFloat(8, 1.0f / iw);
        INPUT_INFO.putFloat(12, 1.0f / ih);

        OUTPUT_INFO.putFloat(0, ow);
        OUTPUT_INFO.putFloat(4, oh);
        OUTPUT_INFO.putFloat(8, 1.0f / ow);
        OUTPUT_INFO.putFloat(12, 1.0f / oh);

        float halfU = 0.5f / iw;
        float halfV = 0.5f / ih;
        float maxU = Math.min(1.0f, Math.max(1, usedWidth) / iw) - halfU;
        float maxV = Math.min(1.0f, Math.max(1, usedHeight) / ih) - halfV;

        UV_BOUNDS.putFloat(0, halfU);
        UV_BOUNDS.putFloat(4, halfV);
        UV_BOUNDS.putFloat(8, Math.max(halfU, maxU));
        UV_BOUNDS.putFloat(12, Math.max(halfV, maxV));

        passes++;
        lastBackend = clampBackend(backend);
        lastInputWidth = inputWidth;
        lastInputHeight = inputHeight;
        lastOutputWidth = outputWidth;
        lastOutputHeight = outputHeight;
        lastSharpness = sharpness;

        PARAMS.putFloat(0, sharpness);
        PARAMS.putFloat(4, lastBackend);
        PARAMS.putFloat(8, 0.0f);
        PARAMS.putFloat(12, 0.0f);
    }

    public static MappedBuffer getInputInfo() {
        return INPUT_INFO;
    }

    public static MappedBuffer getOutputInfo() {
        return OUTPUT_INFO;
    }

    public static MappedBuffer getUvBounds() {
        return UV_BOUNDS;
    }

    public static MappedBuffer getParams() {
        return PARAMS;
    }
}
