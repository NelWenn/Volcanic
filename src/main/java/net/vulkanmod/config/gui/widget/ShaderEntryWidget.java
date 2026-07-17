package net.vulkanmod.config.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.option.ShaderEntryOption;
import net.vulkanmod.vulkan.util.ColorUtil;

public class ShaderEntryWidget extends OptionWidget<ShaderEntryOption> {
    private int settingsX;
    private int settingsWidth;

    public ShaderEntryWidget(ShaderEntryOption option, Component name) {
        super(option, name);
        updateDisplayedValue();
    }

    @Override
    public void renderWidget(double mouseX, double mouseY) {
        boolean selected = this.option.isSelected();
        boolean rowHovered = mouseX >= this.x && mouseY >= this.y
                && mouseX < this.x + this.width && mouseY < this.y + this.height;

        int bg = selected
                ? ColorUtil.ARGB.pack(0.45f, 0.18f, 0.18f, 0.9f)
                : ColorUtil.ARGB.pack(0.0f, 0.0f, 0.0f, 0.45f);
        GuiRenderer.fillBox(this.x, this.y, this.width, this.height, bg);
        if (rowHovered) {
            GuiRenderer.fillBox(this.x, this.y, this.width, this.height,
                    ColorUtil.ARGB.pack(1.0f, 1.0f, 1.0f, 0.08f));
        }

        Font textRenderer = Minecraft.getInstance().font;
        int textColor = this.active ? 0xFFFFFFFF : 0xFFA0A0A0;

        GuiRenderer.drawString(textRenderer, this.option.displayName(),
                this.x + 8, this.y + (this.height - 8) / 2, textColor);

        int sw = 64;
        int sx = this.x + this.width - sw - 6;
        int sy = this.y + 2;
        int sh = this.height - 4;

        if (selected) {
            this.settingsX = sx;
            this.settingsWidth = sw;

            boolean settingsHovered = mouseX >= sx && mouseY >= sy
                    && mouseX < sx + sw && mouseY < sy + sh;

            int box = settingsHovered
                    ? ColorUtil.ARGB.pack(0.5f, 0.2f, 0.2f, 1.0f)
                    : ColorUtil.ARGB.pack(0.25f, 0.25f, 0.25f, 1.0f);
            GuiRenderer.fillBox(sx, sy, sw, sh, box);
            GuiRenderer.renderBoxBorder(sx, sy, sw, sh, 1, ColorUtil.ARGB.pack(0.6f, 0.6f, 0.6f, 1.0f));

            GuiRenderer.drawCenteredString(textRenderer, Component.literal("⚙ Settings"),
                    sx + sw / 2, this.y + (this.height - 8) / 2, textColor);
        } else {
            this.settingsX = 0;
            this.settingsWidth = 0;

            Component hint = Component.literal("Select");
            int hintColor = 0xFF808080;
            int hintWidth = textRenderer.width(hint);
            GuiRenderer.drawString(textRenderer, hint,
                    this.x + this.width - hintWidth - 8, this.y + (this.height - 8) / 2, hintColor);
        }
    }

    @Override
    protected void renderControls(double mouseX, double mouseY) {

    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return this.active && this.visible
                && mouseX >= (double) this.x && mouseY >= (double) this.y
                && mouseX < (double) (this.x + this.width) && mouseY < (double) (this.y + this.height);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (this.option.isSelected() && this.settingsWidth > 0
                && mouseX >= this.settingsX && mouseX < this.settingsX + this.settingsWidth) {
            this.option.openSettings();
        } else {
            this.option.select();
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {

    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {

    }

    @Override
    protected void updateDisplayedValue() {
        this.displayedValue = this.option.displayName();
    }
}
