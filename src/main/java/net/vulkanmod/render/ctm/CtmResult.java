package net.vulkanmod.render.ctm;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.vulkanmod.render.vertex.TerrainRenderType;

public final class CtmResult {
    public enum Kind { NONE, SWAP, OVERLAY }

    private static final CtmResult NONE = new CtmResult(Kind.NONE, null, null, -1);

    private final Kind kind;
    private final TextureAtlasSprite sprite;
    private final TerrainRenderType layer;
    private final int tintIndex;

    private CtmResult(Kind kind, TextureAtlasSprite sprite, TerrainRenderType layer, int tintIndex) {
        this.kind = kind;
        this.sprite = sprite;
        this.layer = layer;
        this.tintIndex = tintIndex;
    }

    public static CtmResult none() { return NONE; }
    public static CtmResult swap(TextureAtlasSprite sprite) { return new CtmResult(Kind.SWAP, sprite, null, -1); }
    public static CtmResult overlay(TextureAtlasSprite sprite, TerrainRenderType layer, int tintIndex) {
        return new CtmResult(Kind.OVERLAY, sprite, layer, tintIndex);
    }

    public Kind kind() { return kind; }
    public TextureAtlasSprite sprite() { return sprite; }
    public TerrainRenderType layer() { return layer; }
    public int tintIndex() { return tintIndex; }
}
