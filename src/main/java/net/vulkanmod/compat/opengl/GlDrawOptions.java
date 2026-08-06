package net.vulkanmod.compat.opengl;

public final class GlDrawOptions {
    private static final String PRESERVE_LEGACY_PROPERTY = "vulkanmod.compat.glDraw.preserveLegacyBridge";
    private static final String DEBUG_PROPERTY = "vulkanmod.compat.glDraw.debug";
    private static final String FBO_VIEWPORT_PROPERTY = "vulkanmod.compat.glDraw.fboViewport";

    private GlDrawOptions() {
    }

    public static boolean shouldPreserveLegacyBridge() {
        return Boolean.parseBoolean(System.getProperty(PRESERVE_LEGACY_PROPERTY, "true"));
    }

    public static boolean debugDrawContracts() {
        return Boolean.parseBoolean(System.getProperty(DEBUG_PROPERTY, "false"));
    }

    public static boolean fboViewportUsesFboConvention() {
        return Boolean.parseBoolean(System.getProperty(FBO_VIEWPORT_PROPERTY, "true"));
    }
}
