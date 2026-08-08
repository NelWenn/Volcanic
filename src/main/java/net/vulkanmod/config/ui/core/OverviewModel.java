package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class OverviewModel {

    public record Row(String labelKey, String value) {
        public Row {
            requireText(labelKey, "labelKey");
            requireText(value, "value");
        }
    }

    private final List<Row> rows = new ArrayList<>();

    public OverviewModel add(String labelKey, Optional<String> value) {
        requireText(labelKey, "labelKey");
        if (value == null) {
            throw new IllegalArgumentException("value must not be null: " + labelKey);
        }
        value.ifPresent(text -> rows.add(new Row(labelKey, text)));
        return this;
    }

    public List<Row> rows() {
        return List.copyOf(rows);
    }

    private static void requireText(String text, String name) {
        if (text == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        if (text.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
