package net.vulkanmod.compat.opengl;

import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;

public final class GlDrawOptions {
    private static final String PRESERVE_LEGACY_PROPERTY = "vulkanmod.compat.glDraw.preserveLegacyBridge";
    private static final String DEBUG_PROPERTY = "vulkanmod.compat.glDraw.debug";
    private static final String FBO_VIEWPORT_PROPERTY = "vulkanmod.compat.glDraw.fboViewport";

    private GlDrawOptions() {
    }

    public static boolean shouldPreserveLegacyBridge() {
        Config config = Initializer.CONFIG;
        return resolve(PRESERVE_LEGACY_PROPERTY, config == null || config.glLegacyBridge);
    }

    public static boolean debugDrawContracts() {
        return Boolean.parseBoolean(System.getProperty(DEBUG_PROPERTY, "false"));
    }

    public static boolean fboViewportUsesFboConvention() {
        Config config = Initializer.CONFIG;
        return resolve(FBO_VIEWPORT_PROPERTY, config == null || config.glFboViewport);
    }

    public static boolean legacyBridgeOverridden() {
        return System.getProperty(PRESERVE_LEGACY_PROPERTY) != null;
    }

    public static boolean fboViewportOverridden() {
        return System.getProperty(FBO_VIEWPORT_PROPERTY) != null;
    }

    private static boolean resolve(String property, boolean configured) {
        String value = System.getProperty(property);
        return value == null ? configured : Boolean.parseBoolean(value);
    }
}
