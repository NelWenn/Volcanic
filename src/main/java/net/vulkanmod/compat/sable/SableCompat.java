package net.vulkanmod.compat.sable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class SableCompat {

    private static boolean resolved;
    private static Field subLevelField;
    private static Method getLatestSkyLightScale;

    private SableCompat() {
    }

    public static float skyLightScale(Object chunkedRenderData) {
        try {
            if (!resolved) {
                resolve(chunkedRenderData.getClass());
                resolved = true;
            }

            if (subLevelField == null || getLatestSkyLightScale == null)
                return 1.0f;

            Object subLevel = subLevelField.get(chunkedRenderData);
            if (subLevel == null)
                return 1.0f;

            int scale = (int) getLatestSkyLightScale.invoke(subLevel);
            return Math.max(0.0f, Math.min(1.0f, scale / 15.0f));
        } catch (Throwable t) {
            return 1.0f;
        }
    }

    private static void resolve(Class<?> renderDataClass) {
        try {
            Field field = renderDataClass.getDeclaredField("subLevel");
            field.setAccessible(true);
            subLevelField = field;
            getLatestSkyLightScale = field.getType().getMethod("getLatestSkyLightScale");
        } catch (Throwable t) {
            subLevelField = null;
            getLatestSkyLightScale = null;
        }
    }
}
