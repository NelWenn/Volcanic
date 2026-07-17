package net.vulkanmod.config.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.option.ActionOption;
import net.vulkanmod.vulkan.util.ColorUtil;

public class ActionOptionWidget extends OptionWidget<ActionOption> {

    public ActionOptionWidget(ActionOption option, Component name) {
        super(option, name);
        updateDisplayedValue();
    }

    @Override
    public void renderWidget(double mouseX, double mouseY) {
        if (this.option.isBackStyle()) {
            this.renderBack(mouseX, mouseY);
        } else if (this.option.isFullButton()) {
            this.renderFullButton(mouseX, mouseY);
        } else {
            super.renderWidget(mouseX, mouseY);
        }
    }

    private void renderBack(double mouseX, double mouseY) {
        int x0 = this.x;
        int y0 = this.y + 1;
        int width = this.width;
        int height = this.height - 2;

        boolean hovered = mouseX >= this.x && mouseY >= this.y
                && mouseX < this.x + this.width && mouseY < this.y + this.height;

        int color = hovered
                ? ColorUtil.ARGB.pack(0.30f, 0.30f, 0.30f, 0.85f)
                : ColorUtil.ARGB.pack(0.15f, 0.15f, 0.15f, 0.65f);
        GuiRenderer.fillBox(x0, y0, width, height, color);

        color = this.active ? 0xFFFFFFFF : 0xFFA0A0A0;
        Font textRenderer = Minecraft.getInstance().font;
        int y = this.y + (this.height - 8) / 2;
        GuiRenderer.drawString(textRenderer, this.option.getActionLabel(), this.x + 8, y, color);
    }

    private void renderFullButton(double mouseX, double mouseY) {
        int x0 = this.x;
        int y0 = this.y + 1;
        int width = this.width;
        int height = this.height - 2;

        boolean hovered = mouseX >= this.x && mouseY >= this.y
                && mouseX < this.x + this.width && mouseY < this.y + this.height;

        int color = hovered
                ? ColorUtil.ARGB.pack(0.6f, 0.25f, 0.25f, 1.0f)
                : ColorUtil.ARGB.pack(0.45f, 0.18f, 0.18f, 1.0f);
        GuiRenderer.fillBox(x0, y0, width, height, color);

        color = ColorUtil.ARGB.pack(0.6f, 0.6f, 0.6f, 1.0f);
        GuiRenderer.renderBoxBorder(x0, y0, width, height, 1, color);

        color = this.active ? 0xFFFFFFFF : 0xFFA0A0A0;
        Font textRenderer = Minecraft.getInstance().font;
        int x = this.x + this.width / 2;
        int y = this.y + (this.height - 8) / 2;
        GuiRenderer.drawCenteredString(textRenderer, this.option.getActionLabel(), x, y, color);
    }

    @Override
    protected void renderControls(double mouseX, double mouseY) {
        int x0 = this.controlX;
        int y0 = this.y + 2;
        int width = this.controlWidth;
        int height = this.height - 4;

        int color = this.controlHovered
                ? ColorUtil.ARGB.pack(0.5f, 0.2f, 0.2f, 1.0f)
                : ColorUtil.ARGB.pack(0.25f, 0.25f, 0.25f, 1.0f);
        GuiRenderer.fillBox(x0, y0, width, height, color);

        color = ColorUtil.ARGB.pack(0.6f, 0.6f, 0.6f, 1.0f);
        GuiRenderer.renderBoxBorder(x0, y0, width, height, 1, color);

        color = this.active ? 0xFFFFFFFF : 0xFFA0A0A0;
        Font textRenderer = Minecraft.getInstance().font;
        int x = x0 + width / 2;
        int y = this.y + (this.height - 8) / 2;
        GuiRenderer.drawCenteredString(textRenderer, this.option.getActionLabel(), x, y, color);
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return this.active && this.visible
                && mouseX >= (double) this.x && mouseY >= (double) this.y
                && mouseX < (double) (this.x + this.width) && mouseY < (double) (this.y + this.height);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.option.run();
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {

    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {

    }

    @Override
    protected void updateDisplayedValue() {
        this.displayedValue = this.option.getActionLabel();
    }
}
