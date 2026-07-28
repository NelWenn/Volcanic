package net.vulkanmod.render.fusion;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

public final class FusionModelLoader implements IGeometryLoader<FusionModelGeometry> {
    public static final FusionModelLoader INSTANCE = new FusionModelLoader();

    private FusionModelLoader() {}

    @Override
    public FusionModelGeometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
        JsonObject copy = jsonObject.deepCopy();
        copy.remove("loader");
        BlockModel base = deserializationContext.deserialize(copy, BlockModel.class);
        return new FusionModelGeometry(base);
    }
}
