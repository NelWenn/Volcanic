package net.vulkanmod.config.option;

import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SwitchOption extends Option<Boolean> {
    public SwitchOption(Component name, Consumer<Boolean> setter, Supplier<Boolean> getter) {
        super(name, setter, getter, i -> Component.nullToEmpty(String.valueOf(i)));
    }

}
