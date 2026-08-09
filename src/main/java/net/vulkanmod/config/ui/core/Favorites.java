package net.vulkanmod.config.ui.core;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Favorites {
    private final Set<SettingId> ids = new LinkedHashSet<>();

    public void toggle(SettingId id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (!ids.remove(id)) {
            ids.add(id);
        }
    }

    public boolean contains(SettingId id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        return ids.contains(id);
    }

    public List<SettingId> ids() {
        return List.copyOf(ids);
    }

    public int count() {
        return ids.size();
    }

    public void replaceAll(List<SettingId> replacement) {
        if (replacement == null) {
            throw new IllegalArgumentException("replacement must not be null");
        }
        for (SettingId id : replacement) {
            if (id == null) {
                throw new IllegalArgumentException("replacement must not contain a null id");
            }
        }
        ids.clear();
        ids.addAll(replacement);
    }
}
