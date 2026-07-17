package net.vulkanmod.config.option;

import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.widget.OptionWidget;
import net.vulkanmod.config.gui.widget.ShaderEntryWidget;

import java.util.function.Supplier;

public class ShaderEntryOption extends Option<Boolean> {
    private final Supplier<Boolean> selected;
    private final Runnable onSelect;
    private final Runnable onSettings;

    public ShaderEntryOption(Component name, Supplier<Boolean> selected, Runnable onSelect, Runnable onSettings) {
        super(name, v -> {}, () -> Boolean.FALSE);
        this.selected = selected;
        this.onSelect = onSelect;
        this.onSettings = onSettings;
        this.setActivationFn(() -> Boolean.TRUE);
    }

    public boolean isSelected() {
        return Boolean.TRUE.equals(this.selected.get());
    }

    public void select() {
        this.onSelect.run();
    }

    public void openSettings() {
        this.onSettings.run();
    }

    public Component displayName() {
        return this.name;
    }

    @Override
    protected OptionWidget<?> createWidget() {
        var widget = new ShaderEntryWidget(this, this.name);
        this.widget = widget;
        return widget;
    }
}
