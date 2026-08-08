package net.vulkanmod.config.ui.shell;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.ui.core.ColorToken;
import net.vulkanmod.config.ui.core.KeyAction;
import net.vulkanmod.config.ui.core.Rect;
import net.vulkanmod.config.ui.core.ShellLayout;
import net.vulkanmod.config.ui.core.Theme;
import net.vulkanmod.config.ui.render.SurfacePainter;

public class VolcanicScreen extends Screen {
    private static final int CARD_RADIUS = 5;

    private final Screen parent;
    private final Theme theme = Theme.volcanic();
    private ShellLayout layout = ShellLayout.of(0, 0);

    public VolcanicScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.layout = ShellLayout.of(this.width, this.height);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        SurfacePainter painter = SurfacePainter.create(guiGraphics, this.font);

        painter.fill(new Rect(0, 0, this.width, this.height), theme.color(ColorToken.SURFACE_BASE));
        painter.fill(layout.topBar(), theme.color(ColorToken.SURFACE_CHROME));
        painter.fill(layout.bottomBar(), theme.color(ColorToken.SURFACE_CHROME));
        painter.fill(layout.sidebar(), theme.color(ColorToken.SURFACE_BASE));

        if (layout.hasDetailsPanel()) {
            painter.fill(layout.details(), theme.color(ColorToken.SURFACE_CHROME));
        }

        painter.fill(new Rect(layout.sidebar().right(), layout.sidebar().y(), 1, layout.sidebar().height()),
                theme.color(ColorToken.BORDER_DEFAULT));
        painter.fill(new Rect(layout.topBar().x(), layout.topBar().bottom() - 1, layout.topBar().width(), 1),
                theme.color(ColorToken.BORDER_DEFAULT));
        painter.fill(new Rect(layout.bottomBar().x(), layout.bottomBar().y(), layout.bottomBar().width(), 1),
                theme.color(ColorToken.BORDER_DEFAULT));

        painter.surface(layout.content().inset(12).withHeight(40), CARD_RADIUS,
                theme.color(ColorToken.SURFACE_CARD), theme.color(ColorToken.BORDER_DEFAULT),
                theme.color(ColorToken.ACCENT, 0.13f), 16);

        painter.text(layout.topBar().x() + 12, layout.topBar().y() + 12, "VOLCANIC",
                theme.color(ColorToken.TEXT_PRIMARY), false);
        painter.text(layout.content().x() + 20, layout.content().y() + 26,
                layout.breakpoint().name() + "  " + this.width + " x " + this.height,
                theme.color(ColorToken.TEXT_MUTED), false);

        painter.flush();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        KeyAction action = UiKeys.actionFor(keyCode, Screen.hasControlDown());
        if (action == KeyAction.BACK) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
