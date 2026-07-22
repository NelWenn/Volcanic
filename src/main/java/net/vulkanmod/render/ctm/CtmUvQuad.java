package net.vulkanmod.render.ctm;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.vulkanmod.render.model.quad.QuadView;

public final class CtmUvQuad implements QuadView {
    private final QuadView base;
    private final float o0u, o1u, o0v, o1v;
    private final float n0u, n1u, n0v, n1v;

    public CtmUvQuad(QuadView base, TextureAtlasSprite from, TextureAtlasSprite to) {
        this.base = base;
        this.o0u = from.getU0(); this.o1u = from.getU1();
        this.o0v = from.getV0(); this.o1v = from.getV1();
        this.n0u = to.getU0(); this.n1u = to.getU1();
        this.n0v = to.getV0(); this.n1v = to.getV1();
    }

    @Override public int getFlags() { return base.getFlags(); }
    @Override public float getX(int idx) { return base.getX(idx); }
    @Override public float getY(int idx) { return base.getY(idx); }
    @Override public float getZ(int idx) { return base.getZ(idx); }
    @Override public int getColor(int idx) { return base.getColor(idx); }
    @Override public int getColorIndex() { return base.getColorIndex(); }
    @Override public Direction getFacingDirection() { return base.getFacingDirection(); }
    @Override public float getU(int idx) { return UvRemap.remap(base.getU(idx), o0u, o1u, n0u, n1u); }
    @Override public float getV(int idx) { return UvRemap.remap(base.getV(idx), o0v, o1v, n0v, n1v); }
}
