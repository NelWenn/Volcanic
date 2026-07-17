package net.vulkanmod.config.option;

import net.vulkanmod.config.gui.OptionBlock;
import net.vulkanmod.config.gui.VOptionList;

public class OptionPage {
    public final String name;
    public OptionBlock[] optionBlocks;
    private VOptionList optionList;
    private int order;

    private int listX, listY, listWidth, listHeight, listItemHeight;

    public OptionPage(String name, OptionBlock[] optionBlocks) {
        this.name = name;
        this.optionBlocks = optionBlocks;
    }

    public void createList(int x, int y, int width, int height, int itemHeight) {
        this.listX = x;
        this.listY = y;
        this.listWidth = width;
        this.listHeight = height;
        this.listItemHeight = itemHeight;

        this.optionList = new VOptionList(x, y, width, height, itemHeight);
        this.optionList.addAll(optionBlocks);
    }

    public void setOptionBlocks(OptionBlock[] optionBlocks) {
        this.optionBlocks = optionBlocks;
        this.createList(this.listX, this.listY, this.listWidth, this.listHeight, this.listItemHeight);
    }

    public VOptionList getOptionList() {
        return this.optionList;
    }

    public boolean optionChanged() {
        boolean changed = false;
        for (var block : this.optionBlocks) {
            for (var option : block.options()) {
                if (option.isChanged())
                    changed = true;
            }
        }
        return changed;
    }

    public void applyOptionChanges() {
        for (var block : this.optionBlocks) {
            for (var option : block.options()) {
                if (option.isChanged()) {
                    option.apply();
                }
            }
        }
    }

    public void updateOptionStates() {
        for (var block : this.optionBlocks) {
            for (var option : block.options()) {
                option.updateActiveState();
            }
        }
    }

    public void resetToOriginalState() {
        for (var block : this.optionBlocks) {
            for (var option : block.options()) {
                option.resetValue();
            }
        }
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
}