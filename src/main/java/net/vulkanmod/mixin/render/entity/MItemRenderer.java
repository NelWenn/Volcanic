package net.vulkanmod.mixin.render.entity;

import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.vulkanmod.render.cit.Cit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public class MItemRenderer {
    @Inject(method = "getModel", at = @At("RETURN"), cancellable = true)
    private void volcanic$citModel(ItemStack stack, Level level, LivingEntity entity, int seed, CallbackInfoReturnable<BakedModel> cir) {
        if (!Cit.isActive()) return;
        BakedModel cit = Cit.resolve(stack);
        if (cit != null) cir.setReturnValue(cit);
    }
}
