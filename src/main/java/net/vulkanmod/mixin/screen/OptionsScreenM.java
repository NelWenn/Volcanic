package net.vulkanmod.mixin.screen;

import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.ui.shell.VolcanicScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OptionsScreen.class)
public class OptionsScreenM extends Screen {

    @Shadow @Final private Screen lastScreen;

    @Shadow @Final private Options options;

    protected OptionsScreenM(Component title) {
        super(title);
    }

    @Inject(method = "lambda$init$2", at = @At("HEAD"), cancellable = true, require = 1)
    private void injectVideoOptionScreen(CallbackInfoReturnable<Screen> cir) {
        cir.setReturnValue(new VolcanicScreen(Component.translatable("vulkanmod.options.title"), this));
    }
}
