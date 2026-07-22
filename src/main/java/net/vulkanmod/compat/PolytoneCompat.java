package net.vulkanmod.compat;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

/**
 * Optional bridge to Polytone's block-render hooks. VulkanMod's terrain builder replaces the vanilla
 * ModelBlockRenderer, so Polytone's own mixins never fire; we call its public API directly instead.
 */
public final class PolytoneCompat {

    private static volatile boolean initialized;

    private static boolean variantTexturesActive;
    private static MethodHandle maybeModifyQuadHandle;

    private static boolean blockOffsetActive;
    private static MethodHandle hasVisualOffsetHandle;
    private static MethodHandle maybeModifyOffsetHandle;

    private PolytoneCompat() {
    }

    private static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        if (!CompatDetector.isModLoaded("polytone")) {
            return;
        }

        Class<?> polytone;
        try {
            polytone = Class.forName("net.mehvahdjukaar.polytone.Polytone");
        } catch (Throwable t) {
            Initializer.LOGGER.warn("Polytone present but its main class was not found", t);
            return;
        }

        MethodHandles.Lookup lookup = MethodHandles.publicLookup();

        try {
            Field field = polytone.getField("VARIANT_TEXTURES");
            Object manager = field.get(null);
            MethodType type = MethodType.methodType(BakedQuad.class, BakedQuad.class, BlockAndTintGetter.class,
                    BlockState.class, BlockPos.class);
            maybeModifyQuadHandle = lookup.findVirtual(field.getType(), "maybeModify", type).bindTo(manager);
            variantTexturesActive = true;
        } catch (Throwable t) {
            Initializer.LOGGER.warn("Polytone variant-texture integration failed to initialise", t);
        }

        try {
            Field field = polytone.getField("BLOCK_MODIFIERS");
            Object manager = field.get(null);
            hasVisualOffsetHandle = lookup.findVirtual(field.getType(), "hasVisualOffset",
                    MethodType.methodType(boolean.class, BlockState.class)).bindTo(manager);
            maybeModifyOffsetHandle = lookup.findVirtual(field.getType(), "maybeModifyOffset",
                    MethodType.methodType(Vec3.class, BlockState.class, BlockGetter.class, BlockPos.class)).bindTo(manager);
            blockOffsetActive = true;
        } catch (Throwable t) {
            Initializer.LOGGER.warn("Polytone block-offset integration failed to initialise", t);
        }

        if (variantTexturesActive || blockOffsetActive) {
            Initializer.LOGGER.info("Polytone block-render integration enabled (variantTextures={}, blockOffset={})",
                    variantTexturesActive, blockOffsetActive);
        }
    }

    public static BakedQuad maybeModifyQuad(BakedQuad quad, BlockAndTintGetter level, BlockState state, BlockPos pos) {
        if (!initialized) {
            init();
        }
        if (!variantTexturesActive) {
            return quad;
        }
        try {
            BakedQuad modified = (BakedQuad) maybeModifyQuadHandle.invoke(quad, level, state, pos);
            return modified != null ? modified : quad;
        } catch (Throwable t) {
            return quad;
        }
    }

    public static Vec3 modifyOffset(Vec3 vanillaOffset, BlockState state, BlockGetter level, BlockPos pos) {
        if (!initialized) {
            init();
        }
        if (!blockOffsetActive) {
            return vanillaOffset;
        }
        try {
            if ((boolean) hasVisualOffsetHandle.invoke(state)) {
                Vec3 modified = (Vec3) maybeModifyOffsetHandle.invoke(state, level, pos);
                if (modified != null) {
                    return modified;
                }
            }
        } catch (Throwable t) {
            return vanillaOffset;
        }
        return vanillaOffset;
    }
}
