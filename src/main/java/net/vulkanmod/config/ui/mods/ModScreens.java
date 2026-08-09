package net.vulkanmod.config.ui.mods;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.vulkanmod.Initializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ModScreens {

    private ModScreens() {
    }

    public static List<String> without(Collection<String> excluded) {
        if (excluded == null) {
            throw new IllegalArgumentException("excluded must not be null");
        }
        List<String> ids = new ArrayList<>();
        try {
            for (ModContainer container : ModList.get().getSortedMods()) {
                String modId = container.getModId();
                if (!excluded.contains(modId)
                        && container.getCustomExtension(IConfigScreenFactory.class).isPresent()) {
                    ids.add(modId);
                }
            }
        } catch (Throwable t) {
            Initializer.LOGGER.warn("Mod settings passthrough: the mod list could not be read", t);
        }
        ids.sort(Comparator.naturalOrder());
        return List.copyOf(ids);
    }

    public static Optional<Screen> screenOf(String modId, Screen parent) {
        if (modId == null || modId.isBlank()) {
            throw new IllegalArgumentException("modId must not be blank");
        }
        if (parent == null) {
            throw new IllegalArgumentException("parent must not be null");
        }
        try {
            return ModList.get().getModContainerById(modId)
                    .flatMap(container -> container.getCustomExtension(IConfigScreenFactory.class)
                            .map(factory -> factory.createScreen(container, parent)));
        } catch (Throwable t) {
            Initializer.LOGGER.warn("Could not open the settings screen of {}", modId, t);
            return Optional.empty();
        }
    }
}
