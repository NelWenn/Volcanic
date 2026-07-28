package net.vulkanmod.mixin.render.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.cit.CitPackLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.Reader;
import java.util.Optional;

@Mixin(ModelBakery.class)
public class MModelBakery {
    @Inject(method = "loadBlockModel", at = @At("HEAD"), cancellable = true)
    private void volcanic$citModelSource(ResourceLocation location, CallbackInfoReturnable<BlockModel> cir) {
        if (!CitPackLoader.isRenderable(location)) return;

        ResourceLocation json = ResourceLocation.fromNamespaceAndPath(location.getNamespace(), location.getPath() + ".json");
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(json);
        if (res.isEmpty()) return;

        try (Reader reader = res.get().openAsReader()) {
            BlockModel model = BlockModel.fromStream(reader);
            model.name = location.toString();
            cir.setReturnValue(model);
        } catch (Exception e) {
            Initializer.LOGGER.warn("CIT: failed to load model {}", location, e);
        }
    }
}
