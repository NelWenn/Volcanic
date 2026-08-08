package net.vulkanmod.config.ui.settings;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SettingBinding {
    private final Supplier<Object> getter;
    private final Consumer<Object> setter;
    private final Supplier<List<String>> choices;
    private final Supplier<Object> defaultValue;
    private final int min;
    private final int max;
    private final int step;

    private SettingBinding(Supplier<Object> getter, Consumer<Object> setter,
                           Supplier<List<String>> choices, Supplier<Object> defaultValue,
                           int min, int max, int step) {
        if (getter == null || setter == null) {
            throw new IllegalArgumentException("getter and setter must not be null");
        }
        this.getter = getter;
        this.setter = setter;
        this.choices = choices == null ? List::of : choices;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public static SettingBinding of(Supplier<Object> getter, Consumer<Object> setter) {
        return new SettingBinding(getter, setter, null, null, 0, 0, 1);
    }

    public static SettingBinding ranged(Supplier<Object> getter, Consumer<Object> setter,
                                        int min, int max, int step) {
        if (max < min) {
            throw new IllegalArgumentException("max " + max + " is below min " + min);
        }
        if (step <= 0) {
            throw new IllegalArgumentException("step must be positive: " + step);
        }
        return new SettingBinding(getter, setter, null, null, min, max, step);
    }

    public static SettingBinding choosing(Supplier<Object> getter, Consumer<Object> setter,
                                          Supplier<List<String>> choices) {
        if (choices == null) {
            throw new IllegalArgumentException("choices must not be null");
        }
        return new SettingBinding(getter, setter, choices, null, 0, 0, 1);
    }

    public SettingBinding withDefault(Supplier<Object> supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("default supplier must not be null");
        }
        return new SettingBinding(getter, setter, choices, supplier, min, max, step);
    }

    public Object get() {
        return getter.get();
    }

    public void set(Object value) {
        setter.accept(value);
    }

    public boolean hasDefault() {
        return defaultValue != null;
    }

    public Object defaultValue() {
        if (defaultValue == null) {
            throw new IllegalStateException("no default declared for this binding");
        }
        return defaultValue.get();
    }

    public List<String> choices() {
        return choices.get();
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    public int step() {
        return step;
    }
}
