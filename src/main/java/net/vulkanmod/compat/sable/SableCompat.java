package net.vulkanmod.compat.sable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class SableCompat {

    private static boolean resolved;
    private static Field subLevelField;
    private static Method getLatestSkyLightScale;

    private static boolean poseResolved;
    private static Method logicalPose;
    private static Method posePosition;
    private static Method posX;
    private static Method posY;
    private static Method posZ;

    private SableCompat() {
    }

    public static void beginSubLevelBiome(Object chunkedRenderData) {
        try {
            if (!resolved) {
                resolve(chunkedRenderData.getClass());
                resolved = true;
            }
            if (subLevelField == null) {
                return;
            }

            Object subLevel = subLevelField.get(chunkedRenderData);
            if (subLevel == null) {
                return;
            }

            if (!poseResolved) {
                resolvePose(subLevel);
                poseResolved = true;
            }
            if (logicalPose == null || posePosition == null || posX == null) {
                return;
            }

            Object pose = logicalPose.invoke(subLevel);
            Object position = posePosition.invoke(pose);
            double x = ((Number) posX.invoke(position)).doubleValue();
            double y = ((Number) posY.invoke(position)).doubleValue();
            double z = ((Number) posZ.invoke(position)).doubleValue();

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }

            BlockPos blockPos = BlockPos.containing(x, y, z);
            Biome biome = mc.level.getBiome(blockPos).value();
            SableBiomeContext.setPending(swapRedBlue(biome.getGrassColor(x, z)), swapRedBlue(biome.getFoliageColor()), swapRedBlue(biome.getWaterColor()));
        } catch (Throwable t) {
        }
    }

    private static int swapRedBlue(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (b << 16) | (g << 8) | r;
    }

    private static void resolvePose(Object subLevel) {
        try {
            logicalPose = subLevel.getClass().getMethod("logicalPose");
            Object pose = logicalPose.invoke(subLevel);
            posePosition = pose.getClass().getMethod("position");
            Object position = posePosition.invoke(pose);
            posX = position.getClass().getMethod("x");
            posY = position.getClass().getMethod("y");
            posZ = position.getClass().getMethod("z");
        } catch (Throwable t) {
            logicalPose = null;
            posePosition = null;
            posX = null;
        }
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
