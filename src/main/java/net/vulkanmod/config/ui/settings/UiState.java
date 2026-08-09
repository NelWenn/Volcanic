package net.vulkanmod.config.ui.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.ui.core.Favorites;
import net.vulkanmod.config.ui.core.SettingId;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class UiState {
    public static final Path PATH = Path.of("config", "volcanic_ui.json");

    private static final int UI_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private UiState() {
    }

    public record State(Favorites favorites, List<String> collapsed) {
    }

    public static State load(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        Favorites favorites = new Favorites();
        if (!Files.isRegularFile(path)) {
            return new State(favorites, null);
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            Stored stored = GSON.fromJson(reader, Stored.class);
            if (stored == null) {
                return new State(favorites, null);
            }
            if (stored.favorites != null) {
                favorites.replaceAll(parseAll(stored.favorites));
            }
            return new State(favorites, stored.collapsed);
        } catch (IOException | RuntimeException failure) {
            Initializer.LOGGER.warn("Ignoring unreadable UI state at {}: {}", path, failure.toString());
            return new State(new Favorites(), null);
        }
    }

    public static void save(Path path, Favorites favorites, java.util.Set<String> collapsed) {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        if (favorites == null || collapsed == null) {
            throw new IllegalArgumentException("favorites and collapsed must not be null");
        }
        Stored stored = new Stored();
        stored.favorites = favorites.ids().stream().map(SettingId::toString).toList();
        stored.collapsed = List.copyOf(collapsed);
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, GSON.toJson(stored));
        } catch (IOException | RuntimeException failure) {
            Initializer.LOGGER.warn("Failed to write UI state to {}: {}", path, failure.toString());
        }
    }

    private static List<SettingId> parseAll(List<String> ids) {
        List<SettingId> parsed = new ArrayList<>();
        for (String id : ids) {
            if (id == null) {
                continue;
            }
            try {
                parsed.add(SettingId.parse(id));
            } catch (IllegalArgumentException unresolved) {
                Initializer.LOGGER.debug("Dropping unusable favourite id {}", id);
            }
        }
        return parsed;
    }

    private static final class Stored {
        int uiVersion = UI_VERSION;
        List<String> favorites = List.of();
        List<String> collapsed = null;
    }
}
