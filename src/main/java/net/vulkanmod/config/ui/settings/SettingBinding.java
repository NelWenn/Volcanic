package net.vulkanmod.config.ui.settings;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class SettingBinding {
    private final Supplier<Object> getter;
    private final Consumer<Object> setter;
    private final Supplier<List<String>> choices;
    private final Supplier<Object> defaultValue;
    private final Function<Object, String> formatter;
    private final int min;
    private final IntSupplier max;
    private final int step;
    private final int scale;

    private SettingBinding(Supplier<Object> getter, Consumer<Object> setter,
                           Supplier<List<String>> choices, Supplier<Object> defaultValue,
                           Function<Object, String> formatter, int min, IntSupplier max, int step, int scale) {
        if (getter == null || setter == null) {
            throw new IllegalArgumentException("getter and setter must not be null");
        }
        if (max == null) {
            throw new IllegalArgumentException("max must not be null");
        }
        this.getter = getter;
        this.setter = setter;
        this.choices = choices == null ? List::of : choices;
        this.defaultValue = defaultValue;
        this.formatter = formatter;
        this.min = min;
        this.max = max;
        this.step = step;
        this.scale = scale;
    }

    public static SettingBinding of(Supplier<Object> getter, Consumer<Object> setter) {
        return new SettingBinding(getter, setter, null, null, null, 0, () -> 0, 1, 0);
    }

    public static SettingBinding ranged(Supplier<Object> getter, Consumer<Object> setter,
                                        int min, int max, int step) {
        requireGrid(min, max, step);
        return new SettingBinding(getter, setter, null, null, null, min, () -> max, step, 0);
    }

    public static SettingBinding ranged(Supplier<Object> getter, Consumer<Object> setter,
                                        int min, IntSupplier max, int step) {
        if (step <= 0) {
            throw new IllegalArgumentException("step must be positive: " + step);
        }
        return new SettingBinding(getter, setter, null, null, null, min, max, step, 0);
    }

    public static SettingBinding scaled(Supplier<Object> getter, Consumer<Object> setter,
                                        int min, int max, int step, int scale) {
        requireGrid(min, max, step);
        if (scale <= 0) {
            throw new IllegalArgumentException("scale must be positive: " + scale);
        }
        return new SettingBinding(getter, setter, null, null, null, min, () -> max, step, scale);
    }

    public static SettingBinding choosing(Supplier<Object> getter, Consumer<Object> setter,
                                          Supplier<List<String>> choices) {
        if (choices == null) {
            throw new IllegalArgumentException("choices must not be null");
        }
        return new SettingBinding(getter, setter, choices, null, null, 0, () -> 0, 1, 0);
    }

    public SettingBinding withDefault(Supplier<Object> supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("default supplier must not be null");
        }
        return copyWith(supplier, formatter);
    }

    public SettingBinding withFormatter(Function<Object, String> formatter) {
        if (formatter == null) {
            throw new IllegalArgumentException("formatter must not be null");
        }
        return copyWith(defaultValue, formatter);
    }

    public String display(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        if (formatter != null) {
            return formatter.apply(value);
        }
        return scale == 0 ? String.valueOf(value) : String.valueOf(number(value).doubleValue() / scale);
    }

    private SettingBinding copyWith(Supplier<Object> defaultValue, Function<Object, String> formatter) {
        return new SettingBinding(getter, setter, choices, defaultValue, formatter, min, max, step, scale);
    }

    public Object get() {
        Object value = getter.get();
        return scale == 0 ? value : (int) Math.round(number(value).doubleValue() * scale);
    }

    public void set(Object value) {
        setter.accept(scale == 0 ? value : number(value).doubleValue() / scale);
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
        return max.getAsInt();
    }

    public int step() {
        return step;
    }

    private static void requireGrid(int min, int max, int step) {
        if (max < min) {
            throw new IllegalArgumentException("max " + max + " is below min " + min);
        }
        if (step <= 0) {
            throw new IllegalArgumentException("step must be positive: " + step);
        }
    }

    private static Number number(Object value) {
        if (!(value instanceof Number n)) {
            throw new IllegalArgumentException("a scaled setting expects a number, got " + value);
        }
        return n;
    }
}
