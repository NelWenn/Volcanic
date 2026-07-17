package net.vulkanmod.config.option;

import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.widget.ActionOptionWidget;
import net.vulkanmod.config.gui.widget.OptionWidget;

public class ActionOption extends Option<Boolean> {
    private final Runnable action;
    private final Component actionLabel;
    private boolean fullButton = false;
    private boolean backStyle = false;

    public ActionOption(Component name, Component actionLabel, Runnable action) {
        super(name, v -> {}, () -> Boolean.FALSE);
        this.action = action;
        this.actionLabel = actionLabel;
        this.setActivationFn(() -> Boolean.TRUE);
    }

    public void run() {
        this.action.run();
    }

    public Component getActionLabel() {
        return this.actionLabel;
    }

    public ActionOption setFullButton(boolean fullButton) {
        this.fullButton = fullButton;
        return this;
    }

    public boolean isFullButton() {
        return this.fullButton;
    }

    public ActionOption setBackStyle(boolean backStyle) {
        this.backStyle = backStyle;
        return this;
    }

    public boolean isBackStyle() {
        return this.backStyle;
    }

    @Override
    protected OptionWidget<?> createWidget() {
        var widget = new ActionOptionWidget(this, this.name);
        this.widget = widget;
        return widget;
    }
}
