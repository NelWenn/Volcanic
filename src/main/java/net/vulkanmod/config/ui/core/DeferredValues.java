package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public final class DeferredValues {
    private final Map<SettingId, Object> held = new LinkedHashMap<>();

    public boolean set(SettingId id, Object value, Object original) {
        requireId(id);
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        if (original == null) {
            throw new IllegalArgumentException("original must not be null");
        }
        if (Objects.equals(value, original)) {
            held.remove(id);
            return false;
        }
        held.put(id, value);
        return true;
    }

    public void drainTo(BiConsumer<SettingId, Object> sink) {
        if (sink == null) {
            throw new IllegalArgumentException("sink must not be null");
        }
        for (Map.Entry<SettingId, Object> entry : new ArrayList<>(held.entrySet())) {
            sink.accept(entry.getKey(), entry.getValue());
            held.remove(entry.getKey());
        }
    }

    public Object valueOr(SettingId id, Object current) {
        requireId(id);
        if (current == null) {
            throw new IllegalArgumentException("current must not be null");
        }
        return held.getOrDefault(id, current);
    }

    public void clear(SettingId id) {
        requireId(id);
        held.remove(id);
    }

    public void clear() {
        held.clear();
    }

    public boolean isEmpty() {
        return held.isEmpty();
    }

    public int count() {
        return held.size();
    }

    private static void requireId(SettingId id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
    }
}
