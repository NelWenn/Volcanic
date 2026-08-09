package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SettingRegistry {
    private final Map<SettingId, SettingMeta> byId = new LinkedHashMap<>();
    private final Map<RouteId, List<SettingMeta>> byRoute = new LinkedHashMap<>();

    public void register(SettingMeta meta) {
        if (meta == null) {
            throw new IllegalArgumentException("meta must not be null");
        }
        SettingMeta existing = byId.putIfAbsent(meta.id(), meta);
        if (existing != null) {
            throw new IllegalArgumentException("duplicate setting id " + meta.id()
                    + ", already placed at " + existing.route());
        }
        byRoute.computeIfAbsent(meta.route(), route -> new ArrayList<>()).add(meta);
    }

    public boolean contains(SettingId id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        return byId.containsKey(id);
    }

    public SettingMeta get(SettingId id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        SettingMeta meta = byId.get(id);
        if (meta == null) {
            throw new IllegalArgumentException("unknown setting id " + id);
        }
        return meta;
    }

    public List<SettingMeta> all() {
        return List.copyOf(byId.values());
    }

    public List<SettingMeta> forRoute(RouteId route) {
        if (route == null) {
            throw new IllegalArgumentException("route must not be null");
        }
        return List.copyOf(byRoute.getOrDefault(route, List.of()));
    }

    public int size() {
        return byId.size();
    }
}
