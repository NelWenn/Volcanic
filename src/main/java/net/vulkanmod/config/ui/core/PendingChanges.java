package net.vulkanmod.config.ui.core;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PendingChanges {
    private final Map<SettingId, ApplyScope> scopes = new LinkedHashMap<>();

    public void mark(SettingId id, ApplyScope scope) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (scope == null) {
            throw new IllegalArgumentException("scope must not be null");
        }
        scopes.merge(id, scope, (left, right) -> left.ordinal() >= right.ordinal() ? left : right);
    }

    public void unmark(SettingId id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        scopes.remove(id);
    }

    public void clear() {
        scopes.clear();
    }

    public boolean isEmpty() {
        return scopes.isEmpty();
    }

    public boolean isChanged(SettingId id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        return scopes.containsKey(id);
    }

    public int count() {
        return scopes.size();
    }

    public ApplyScope heaviestScope() {
        return ApplyScope.heaviest(scopes.values());
    }

    public int countAtLeast(ApplyScope scope) {
        if (scope == null) {
            throw new IllegalArgumentException("scope must not be null");
        }
        return (int) scopes.values().stream().filter(value -> value.ordinal() >= scope.ordinal()).count();
    }
}
