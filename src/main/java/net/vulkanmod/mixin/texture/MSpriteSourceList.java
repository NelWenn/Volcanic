package net.vulkanmod.mixin.texture;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.ctm.CtmAtlasRegistrar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(SpriteSourceList.class)
public abstract class MSpriteSourceList {

    private static final AtomicBoolean volcanic$warned = new AtomicBoolean(false);

    @Inject(method = "load", at = @At("RETURN"))
    private static void volcanic$appendCtmSprites(ResourceManager resourceManager, ResourceLocation atlasLocation, CallbackInfoReturnable<SpriteSourceList> cir) {
        try {
            Set<ResourceLocation> ids = CtmAtlasRegistrar.additionalSprites();
            if (ids.isEmpty()) return;
            if (!TextureAtlas.LOCATION_BLOCKS.equals(atlasLocation)) return;
            List<SpriteSource> sources = ((SpriteSourceListAccessor) cir.getReturnValue()).getSources();
            for (ResourceLocation id : ids) {
                sources.add(new SingleFile(id, Optional.empty()));
            }
        } catch (Throwable t) {
            if (volcanic$warned.compareAndSet(false, true)) {
                Initializer.LOGGER.error("Failed to stitch CTM sprites into blocks atlas", t);
            }
        }
    }
}
