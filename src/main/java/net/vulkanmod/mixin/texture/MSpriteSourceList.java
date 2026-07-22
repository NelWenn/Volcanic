package net.vulkanmod.mixin.texture;

import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.ctm.CtmAtlasRegistrar;
import net.vulkanmod.render.ctm.CtmSpriteSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(SpriteSourceList.class)
public abstract class MSpriteSourceList {

    private static final AtomicBoolean volcanic$warned = new AtomicBoolean(false);

    @Inject(method = "load", at = @At("RETURN"))
    private static void volcanic$appendCtmSprites(ResourceManager resourceManager, ResourceLocation atlasLocation, CallbackInfoReturnable<SpriteSourceList> cir) {
        try {
            Initializer.LOGGER.info("CTM: SpriteSourceList.load atlas={}", atlasLocation);
            if (!ResourceLocation.withDefaultNamespace("blocks").equals(atlasLocation)) return;
            net.vulkanmod.render.ctm.CtmPackLoader.reload(resourceManager);
            if (CtmAtlasRegistrar.additionalSprites().isEmpty()) return;
            List<SpriteSource> sources = ((SpriteSourceListAccessor) cir.getReturnValue()).getSources();
            sources.add(new CtmSpriteSource());
        } catch (Throwable t) {
            if (volcanic$warned.compareAndSet(false, true)) {
                Initializer.LOGGER.error("Failed to register CTM sprite source into blocks atlas", t);
            }
        }
    }
}
