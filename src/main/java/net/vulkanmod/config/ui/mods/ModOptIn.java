package net.vulkanmod.config.ui.mods;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.gui.ModSettingsEntry;
import net.vulkanmod.config.gui.ModSettingsRegistry;
import net.vulkanmod.config.gui.OptionBlock;
import net.vulkanmod.config.option.Option;
import net.vulkanmod.config.option.OptionPage;
import net.vulkanmod.config.option.RangeOption;
import net.vulkanmod.config.option.SwitchOption;
import net.vulkanmod.config.ui.core.ApplyScope;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingSource;
import net.vulkanmod.config.ui.core.SettingType;
import net.vulkanmod.config.ui.settings.SettingBinding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ModOptIn {

    private ModOptIn() {
    }

    public static List<ModSettings> readAll() {
        Set<ModSettingsEntry> entries;
        try {
            entries = ModSettingsRegistry.INSTANCE.getModEntries();
        } catch (Throwable t) {
            Initializer.LOGGER.warn("Opt-in mod settings disabled: the registry could not be read", t);
            return List.of();
        }
        List<ModSettings> settings = new ArrayList<>();
        Set<String> claimed = new LinkedHashSet<>();
        for (ModSettingsEntry entry : entries) {
            read(entry, claimed).ifPresent(settings::add);
        }
        return List.copyOf(settings);
    }

    private static Optional<ModSettings> read(ModSettingsEntry entry, Set<String> claimed) {
        String modId = null;
        try {
            if (entry == null) {
                throw new IllegalArgumentException("entry must not be null");
            }
            modId = modIdOf(entry);
            if (!claimed.add(modId)) {
                Initializer.LOGGER.warn("Ignored a second opt-in settings entry for {}", modId);
                return Optional.empty();
            }
            List<SettingMeta> metas = new ArrayList<>();
            Map<SettingId, SettingBinding> bindings = new LinkedHashMap<>();
            int skipped = collect(entry, modId, metas, bindings);
            Initializer.LOGGER.debug("Opt-in mod settings: {} contributed {} settings, {} unmappable",
                    modId, metas.size(), skipped);
            return metas.isEmpty() ? Optional.empty() : Optional.of(new ModSettings(modId, metas, bindings));
        } catch (Throwable t) {
            Initializer.LOGGER.warn("Skipped the opt-in settings of {}: they could not be read",
                    modId == null ? "an unnamed mod" : modId, t);
            return Optional.empty();
        }
    }

    private static int collect(ModSettingsEntry entry, String modId, List<SettingMeta> metas,
                               Map<SettingId, SettingBinding> bindings) {
        List<OptionPage> pages = entry.initPages();
        if (pages == null) {
            return 0;
        }
        int skipped = 0;
        for (OptionPage page : pages) {
            if (page == null || page.optionBlocks == null) {
                skipped++;
                continue;
            }
            for (OptionBlock block : page.optionBlocks) {
                if (block == null || block.options() == null) {
                    skipped++;
                    continue;
                }
                for (Option<?> option : block.options()) {
                    if (!add(entry, modId, page, block, option, metas, bindings)) {
                        skipped++;
                    }
                }
            }
        }
        return skipped;
    }

    private static boolean add(ModSettingsEntry entry, String modId, OptionPage page, OptionBlock block,
                               Option<?> option, List<SettingMeta> metas,
                               Map<SettingId, SettingBinding> bindings) {
        try {
            Mapped mapped = map(entry, option);
            if (mapped == null) {
                return false;
            }
            String title = keyOf(labelOf(option));
            SettingId id = SettingId.of(modId, pathOf(page, block, title));
            if (bindings.containsKey(id)) {
                return false;
            }
            SettingMeta.Builder meta = new SettingMeta.Builder(id, ModSettings.routeOf(modId), title,
                    mapped.type(), SettingSource.MODS).scope(ApplyScope.INSTANT);
            String description = descriptionOf(option);
            if (!description.isBlank()) {
                meta.descriptionKey(description);
            }
            metas.add(meta.build());
            bindings.put(id, mapped.binding());
            return true;
        } catch (Throwable t) {
            Initializer.LOGGER.debug("Skipped a setting of {}", modId, t);
            return false;
        }
    }

    private static Mapped map(ModSettingsEntry entry, Option<?> option) {
        if (option instanceof SwitchOption toggle) {
            return new Mapped(SettingType.BOOL,
                    SettingBinding.of(toggle::getNewValue, raw -> write(entry, toggle, boolValue(raw))));
        }
        if (option instanceof RangeOption range) {
            return new Mapped(SettingType.INT,
                    SettingBinding.ranged(range::getNewValue, raw -> write(entry, range, intValue(raw)),
                                    range.getMin(), range.getMax(), range.getStep())
                            .withFormatter(value -> displayOf(range, value)));
        }
        return null;
    }

    private static <T> void write(ModSettingsEntry entry, Option<T> option, T value) {
        option.setNewValue(value);
        option.apply();
        entry.runOnApply();
    }

    private static String displayOf(RangeOption option, Object value) {
        try {
            if (option.getNewValue().equals(value)) {
                return option.getDisplayedValue().getString();
            }
        } catch (Throwable unusable) {
            Initializer.LOGGER.debug("A mod could not label the value {}", value, unusable);
        }
        return String.valueOf(value);
    }

    private static String descriptionOf(Option<?> option) {
        try {
            Component tooltip = option.getTooltip();
            return tooltip == null ? "" : keyOf(tooltip);
        } catch (Throwable unusable) {
            return "";
        }
    }

    private static Component labelOf(Option<?> option) {
        return option instanceof RangeOption range ? range.getBaseName() : option.getName();
    }

    private static String keyOf(Component text) {
        if (text == null) {
            throw new IllegalArgumentException("an option must be named");
        }
        if (text.getContents() instanceof TranslatableContents translatable) {
            return translatable.getKey();
        }
        return text.getString();
    }

    private static String pathOf(OptionPage page, OptionBlock block, String title) {
        return segment(page.name) + "/" + segment(block.title()) + "/" + title;
    }

    private static String segment(String value) {
        return value == null ? "" : value.trim();
    }

    private static String modIdOf(ModSettingsEntry entry) {
        Optional<String> declared = entry.modId();
        if (declared.isPresent()) {
            return declared.get();
        }
        FormattedText modName = entry.modName;
        if (modName == null) {
            throw new IllegalArgumentException("modName must not be null");
        }
        String displayed = modName.getString().trim();
        if (displayed.isEmpty()) {
            throw new IllegalArgumentException("modName must not be blank");
        }
        try {
            for (ModContainer container : ModList.get().getSortedMods()) {
                if (displayed.equalsIgnoreCase(container.getModInfo().getDisplayName())) {
                    return container.getModId();
                }
            }
        } catch (Throwable t) {
            Initializer.LOGGER.warn("Opt-in mod settings: the mod list could not be read", t);
        }
        String slug = ModSettings.slugOf(displayed);
        Initializer.LOGGER.warn("No installed mod is named \"{}\": its settings are filed under the id {}",
                displayed, slug);
        return slug;
    }

    private static boolean boolValue(Object raw) {
        if (!(raw instanceof Boolean flag)) {
            throw new IllegalArgumentException("expected a boolean, got " + raw);
        }
        return flag;
    }

    private static int intValue(Object raw) {
        if (!(raw instanceof Number number)) {
            throw new IllegalArgumentException("expected a number, got " + raw);
        }
        return number.intValue();
    }

    private record Mapped(SettingType type, SettingBinding binding) {
    }
}
