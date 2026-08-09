package net.vulkanmod.config.option;

import net.vulkanmod.config.gui.OptionBlock;

public class OptionPage {
    public final String name;
    public OptionBlock[] optionBlocks;
    private int order;

    private int listX, listY, listWidth, listHeight, listItemHeight;

    public OptionPage(String name, OptionBlock[] optionBlocks) {
        this.name = name;
        this.optionBlocks = optionBlocks;
    }


    public void setOptionBlocks(OptionBlock[] optionBlocks) {
        this.optionBlocks = optionBlocks;
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