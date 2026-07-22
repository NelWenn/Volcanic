package net.vulkanmod.mixin.texture;

import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.render.material.PbrAtlas;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public abstract class MTextureAtlas {

    @Shadow @Final private ResourceLocation location;

    @Inject(method = "upload", at = @At("TAIL"))
    private void volcanic$buildPbrAtlas(SpriteLoader.Preparations preparations, CallbackInfo ci) {
        PbrAtlas.build(this.location, preparations);
    }
}
