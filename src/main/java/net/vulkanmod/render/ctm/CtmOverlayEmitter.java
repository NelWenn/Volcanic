package net.vulkanmod.render.ctm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.vulkanmod.render.chunk.build.BlockRenderer;
import net.vulkanmod.render.chunk.build.light.data.QuadLightData;
import net.vulkanmod.render.model.quad.QuadView;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.joml.Vector3f;

public final class CtmOverlayEmitter {
    private CtmOverlayEmitter() {}

    public static void emit(TerrainBufferBuilder buffer, QuadView quad, QuadLightData light, Vector3f pos,
                            int waveCode, float blockBaseY, boolean upperHalf, int tintIndex,
                            BlockState state, BlockAndTintGetter region, BlockPos blockPos) {
        float r = 1.0F, g = 1.0F, b = 1.0F;
        if (tintIndex >= 0) {
            int color = BlockRenderer.tint(state, region, blockPos, tintIndex);
            r = ColorUtil.ARGB.unpackR(color);
            g = ColorUtil.ARGB.unpackG(color);
            b = ColorUtil.ARGB.unpackB(color);
        }
        Vec3i n = quad.getFacingDirection().getNormal();
        Vector3f biased = new Vector3f(pos.x() + n.getX() * 0.002F, pos.y() + n.getY() * 0.002F, pos.z() + n.getZ() * 0.002F);
        BlockRenderer.putQuadData(buffer, biased, quad, light, r, g, b, waveCode, blockBaseY, upperHalf);
    }
}
