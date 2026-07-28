package net.vulkanmod.render.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public class CustomVertexFormat {

    public static final VertexFormatElement ELEMENT_POSITION = new VertexFormatElement(0, 0,VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.POSITION, 4);
    public static final VertexFormatElement ELEMENT_COLOR = new VertexFormatElement(1, 0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4);
    public static final VertexFormatElement ELEMENT_UV0 = new VertexFormatElement(2, 0, VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.UV, 2);
    public static final VertexFormatElement ELEMENT_UV2 = new VertexFormatElement(3, 2, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2);
    public static final VertexFormatElement ELEMENT_EXTERNAL_LOD_POSITION = new VertexFormatElement(0, 0, VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.POSITION, 4);
    public static final VertexFormatElement ELEMENT_EXTERNAL_LOD_SPRITE_RECT = new VertexFormatElement(2, 0, VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.UV, 4);
    public static final VertexFormatElement ELEMENT_EXTERNAL_LOD_TILE_UV = new VertexFormatElement(3, 1, VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.UV, 2);

    public static final VertexFormat COMPRESSED_TERRAIN = VertexFormat.builder()
            .add("Position", ELEMENT_POSITION)
            .add("Color", ELEMENT_COLOR)
            .add("UV0", ELEMENT_UV0)
            .add("UV2", ELEMENT_UV2)
            .build();

    public static final VertexFormat EXTERNAL_LOD = VertexFormat.builder()
            .add("Position", ELEMENT_EXTERNAL_LOD_POSITION)
            .add("Color", ELEMENT_COLOR)
            .build();

    public static final VertexFormat EXTERNAL_LOD_TEXTURED = VertexFormat.builder()
            .add("Position", ELEMENT_EXTERNAL_LOD_POSITION)
            .add("Color", ELEMENT_COLOR)
            .add("SpriteRect", ELEMENT_EXTERNAL_LOD_SPRITE_RECT)
            .add("TileUV", ELEMENT_EXTERNAL_LOD_TILE_UV)
            .build();

    public static final VertexFormat NONE = VertexFormat.builder().build();
}
