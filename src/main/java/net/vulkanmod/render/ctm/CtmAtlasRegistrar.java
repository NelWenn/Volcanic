package net.vulkanmod.render.ctm;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.function.Function;

public final class CtmAtlasRegistrar {
    private static volatile Set<ResourceLocation> pending = Set.of();

    private CtmAtlasRegistrar() {}

    public static void setPending(Set<ResourceLocation> textureIds) {
        pending = textureIds == null ? Set.of() : Set.copyOf(textureIds);
    }

    public static Set<ResourceLocation> additionalSprites() {
        return pending;
    }

    public static Function<ResourceLocation, TextureAtlasSprite> spriteLookup(TextureAtlas atlas) {
        return id -> {
            TextureAtlasSprite s = atlas.getSprite(id);
            if (s == null) return null;
            ResourceLocation missing = net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation();
            return s.contents().name().equals(missing) ? null : s;
        };
    }
}
