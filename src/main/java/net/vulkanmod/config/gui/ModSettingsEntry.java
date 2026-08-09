package net.vulkanmod.config.gui;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.config.option.OptionPage;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class ModSettingsEntry {
    private final String modId;
    public final FormattedText modName;
    public final Supplier<ResourceLocation> iconSupplier;
    private final Supplier<List<OptionPage>> optionPageSupplier;
    private final Runnable onApply;

    private ResourceLocation icon;
    List<OptionPage> pages;

    public ModSettingsEntry(String modId, FormattedText modName, Supplier<ResourceLocation> iconSupplier, Supplier<List<OptionPage>> optionPageSupplier, Runnable onApply) {
        if (modId == null || modId.isBlank()) {
            throw new IllegalArgumentException("modId must not be blank");
        }
        this.modId = modId;
        this.modName = modName;
        this.iconSupplier = iconSupplier;
        this.optionPageSupplier = optionPageSupplier;
        this.onApply = onApply;
    }

    // Ancien constructeur sans modId : gardé pour ne pas casser les mods déjà compilés.
    // Sans id explicite on retombe sur une correspondance par nom affiché, qui est fragile.
    public ModSettingsEntry(FormattedText modName, Supplier<ResourceLocation> iconSupplier, Supplier<List<OptionPage>> optionPageSupplier, Runnable onApply) {
        this.modId = null;
        this.modName = modName;
        this.iconSupplier = iconSupplier;
        this.optionPageSupplier = optionPageSupplier;
        this.onApply = onApply;
    }

    public Optional<String> modId() {
        return Optional.ofNullable(modId);
    }

    public List<OptionPage> initPages() {
        this.pages = this.optionPageSupplier.get();
        return this.pages;
    }

    public List<OptionPage> getPages() {
        return pages;
    }

    public ResourceLocation getIcon() {
        if (this.icon == null) {
            this.icon = this.iconSupplier.get();
        }

        return icon;
    }

    public void runOnApply() {
        onApply.run();
    }
}
