package net.vulkanmod.compat.capabilities;

import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;

public final class ExternalRenderPathOptions {
    private static final String EXTERNAL_LOD = "vulkanmod.compat.externalLod";
    private static final String EXTERNAL_LOD_DRAW = "vulkanmod.compat.externalLod.draw";
    private static final String EXTERNAL_LOD_DEBUG_DRAW = "vulkanmod.compat.externalLod.debugDraw";

    private ExternalRenderPathOptions() {
    }

    public static String externalLodMode() {
        String property = System.getProperty(EXTERNAL_LOD);
        if (property != null) {
            return property;
        }
        Config config = Initializer.CONFIG;
        return config == null || config.externalLod == null ? "off" : config.externalLod;
    }

    public static boolean externalLodDrawEnabled() {
        String property = System.getProperty(EXTERNAL_LOD_DRAW);
        if (property != null) {
            return Boolean.parseBoolean(property);
        }
        Config config = Initializer.CONFIG;
        return config == null || config.externalLodDraw;
    }

    public static boolean externalLodOverridden() {
        return System.getProperty(EXTERNAL_LOD) != null;
    }

    public static boolean externalLodDrawOverridden() {
        return System.getProperty(EXTERNAL_LOD_DRAW) != null;
    }

    public static boolean externalLodDebugDrawEnabled() {
        return Boolean.parseBoolean(System.getProperty(EXTERNAL_LOD_DEBUG_DRAW, "false"));
    }
}
