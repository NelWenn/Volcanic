package net.vulkanmod.config.ui.mods;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.ui.core.ApplyScope;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingSource;
import net.vulkanmod.config.ui.core.SettingType;
import net.vulkanmod.config.ui.settings.SettingBinding;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ModConfigReader {
    private static final int DOUBLE_SCALE = 100;
    private static final List<String> GENERATED_COMMENTS = List.of("Default:", "Range:", "Allowed Values:");

    private static boolean resolved;
    private static Map<?, ?> configsByMod;
    private static List<ModSettings> cached;

    private ModConfigReader() {
    }

    public static synchronized boolean available() {
        resolve();
        return configsByMod != null;
    }

    public static synchronized List<ModSettings> readAll() {
        if (cached == null) {
            resolve();
            cached = configsByMod == null ? List.of() : collect();
        }
        return cached;
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Field field = ConfigTracker.class.getDeclaredField("configsByMod");
            field.setAccessible(true);
            Object registry = field.get(ConfigTracker.INSTANCE);
            if (registry instanceof Map<?, ?> map) {
                configsByMod = map;
            } else {
                Initializer.LOGGER.warn("Mod settings disabled: NeoForge's config registry is a {}, not a map",
                        registry == null ? "null" : registry.getClass().getName());
            }
        } catch (Throwable t) {
            Initializer.LOGGER.warn("Mod settings disabled: NeoForge's config registry could not be read", t);
        }
    }

    private static List<ModSettings> collect() {
        Map<String, Accumulator> byMod = new LinkedHashMap<>();
        for (Object entry : configsByMod.values()) {
            if (!(entry instanceof Collection<?> configs)) {
                continue;
            }
            for (Object candidate : new ArrayList<>(configs)) {
                if (candidate instanceof ModConfig config) {
                    read(config, byMod);
                }
            }
        }

        List<ModSettings> settings = new ArrayList<>();
        for (Accumulator accumulator : byMod.values()) {
            if (!accumulator.metas.isEmpty()) {
                settings.add(new ModSettings(accumulator.modId, accumulator.metas, accumulator.bindings));
            }
            Initializer.LOGGER.debug("Mod settings: {} contributed {} settings, {} unmappable",
                    accumulator.modId, accumulator.metas.size(), accumulator.skipped);
        }
        return List.copyOf(settings);
    }

    private static void read(ModConfig config, Map<String, Accumulator> byMod) {
        try {
            if (config.getType() != ModConfig.Type.CLIENT || !(config.getSpec() instanceof ModConfigSpec spec)
                    || !spec.isLoaded() || !RenderingMods.isRenderingMod(config.getModId())) {
                return;
            }
            Accumulator accumulator = byMod.computeIfAbsent(config.getModId(), Accumulator::new);
            walk(spec.getValues(), config, accumulator);
        } catch (Throwable t) {
            Initializer.LOGGER.warn("Skipped the settings of {}: its config could not be read", modIdOf(config), t);
        }
    }

    private static void walk(UnmodifiableConfig values, ModConfig config, Accumulator accumulator) {
        for (UnmodifiableConfig.Entry entry : values.entrySet()) {
            Object child = entry.getValue();
            if (child instanceof ModConfigSpec.ConfigValue<?> value) {
                accumulator.add(config, value);
            } else if (child instanceof UnmodifiableConfig nested) {
                walk(nested, config, accumulator);
            }
        }
    }

    private static SettingId idOf(ModConfig config, String path) {
        return SettingId.of(config.getModId(),
                config.getType().name().toLowerCase(Locale.ROOT) + "/" + path);
    }

    private static ApplyScope scopeOf(ModConfigSpec.RestartType restart) {
        return switch (restart) {
            case WORLD -> ApplyScope.CHUNK_REBUILD;
            case GAME -> ApplyScope.RESTART;
            default -> ApplyScope.INSTANT;
        };
    }

    private static String titleOf(ModConfigSpec.ValueSpec spec, String path) {
        String key = spec.getTranslationKey();
        if (key != null && !key.isBlank()) {
            return key;
        }
        int separator = path.lastIndexOf('.');
        return path.substring(separator + 1);
    }

    private static String descriptionOf(ModConfigSpec.ValueSpec spec) {
        String comment = spec.getComment();
        if (comment == null) {
            return "";
        }
        StringBuilder authored = new StringBuilder();
        for (String line : comment.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || GENERATED_COMMENTS.stream().anyMatch(trimmed::startsWith)) {
                continue;
            }
            if (authored.length() > 0) {
                authored.append(' ');
            }
            authored.append(trimmed);
        }
        return authored.toString();
    }

    private static Class<?> classOf(ModConfigSpec.ValueSpec spec) {
        Class<?> declared = spec.getClazz();
        if (declared != null && declared != Object.class) {
            return declared;
        }
        Object fallback = spec.getDefault();
        return fallback == null ? null : fallback.getClass();
    }

    private static Mapped map(ModConfigSpec.ConfigValue<?> value, ModConfigSpec.ValueSpec spec) {
        Class<?> clazz = classOf(spec);
        if (clazz == Boolean.class || clazz == boolean.class) {
            return new Mapped(SettingType.BOOL,
                    SettingBinding.of(() -> current(value, spec), raw -> write(value, raw))
                            .withDefault(spec::getDefault));
        }
        if (clazz == Integer.class || clazz == int.class) {
            ModConfigSpec.Range<Integer> range = spec.getRange();
            if (range == null || range.getMax() <= range.getMin()) {
                return null;
            }
            return new Mapped(SettingType.INT,
                    SettingBinding.ranged(() -> number(current(value, spec)).intValue(),
                            raw -> write(value, number(raw).intValue()), range.getMin(), range.getMax(), 1)
                            .withDefault(() -> number(spec.getDefault()).intValue()));
        }
        if (clazz == Double.class || clazz == double.class) {
            ModConfigSpec.Range<Double> range = spec.getRange();
            if (range == null || range.getMax() <= range.getMin()) {
                return null;
            }
            double min = Math.floor(range.getMin() * DOUBLE_SCALE);
            double max = Math.ceil(range.getMax() * DOUBLE_SCALE);
            if (min < Integer.MIN_VALUE || max > Integer.MAX_VALUE) {
                return null;
            }
            return new Mapped(SettingType.INT,
                    SettingBinding.scaled(() -> number(current(value, spec)).doubleValue(),
                            raw -> write(value, number(raw).doubleValue()), (int) min, (int) max, 1, DOUBLE_SCALE)
                            .withDefault(() -> (int) Math.round(number(spec.getDefault()).doubleValue() * DOUBLE_SCALE)));
        }
        if (clazz != null && clazz.isEnum()) {
            List<String> choices = new ArrayList<>();
            for (Object constant : clazz.getEnumConstants()) {
                if (spec.test(constant)) {
                    choices.add(((Enum<?>) constant).name());
                }
            }
            if (choices.size() < 2) {
                return null;
            }
            return new Mapped(SettingType.ENUM,
                    SettingBinding.choosing(() -> nameOf(current(value, spec)),
                            raw -> write(value, constantOf(clazz, String.valueOf(raw))), () -> choices)
                            .withDefault(() -> nameOf(spec.getDefault())));
        }
        return null;
    }

    private static Object current(ModConfigSpec.ConfigValue<?> value, ModConfigSpec.ValueSpec spec) {
        Object fallback = spec.getDefault();
        Object raw;
        try {
            raw = value.get();
        } catch (Throwable t) {
            return fallback;
        }
        if (raw == null) {
            return fallback;
        }
        if (fallback instanceof Boolean && !(raw instanceof Boolean)) {
            return Boolean.parseBoolean(String.valueOf(raw));
        }
        if (fallback instanceof Number && !(raw instanceof Number)) {
            return fallback;
        }
        if (fallback instanceof Enum<?> && !(raw instanceof Enum<?>)) {
            return fallback;
        }
        return raw;
    }

    @SuppressWarnings("unchecked")
    private static void write(ModConfigSpec.ConfigValue<?> value, Object raw) {
        ((ModConfigSpec.ConfigValue<Object>) value).set(raw);
        value.save();
    }

    private static Object constantOf(Class<?> clazz, String name) {
        for (Object constant : clazz.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(name)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("unknown value " + name + " for " + clazz.getName());
    }

    private static String nameOf(Object constant) {
        if (!(constant instanceof Enum<?> value)) {
            throw new IllegalArgumentException("expected an enum constant, got " + constant);
        }
        return value.name();
    }

    private static Number number(Object raw) {
        if (!(raw instanceof Number value)) {
            throw new IllegalArgumentException("expected a number, got " + raw);
        }
        return value;
    }

    private static String modIdOf(ModConfig config) {
        try {
            return config.getModId();
        } catch (Throwable t) {
            return "an unknown mod";
        }
    }

    private record Mapped(SettingType type, SettingBinding binding) {
    }

    private static final class Accumulator {
        private final String modId;
        private final List<SettingMeta> metas = new ArrayList<>();
        private final Map<SettingId, SettingBinding> bindings = new LinkedHashMap<>();
        private int skipped;

        private Accumulator(String modId) {
            this.modId = modId;
        }

        private void add(ModConfig config, ModConfigSpec.ConfigValue<?> value) {
            try {
                ModConfigSpec.ValueSpec spec = value.getSpec();
                String path = String.join(".", value.getPath());
                String description = descriptionOf(spec);
                Mapped mapped = map(value, spec);
                SettingId id = idOf(config, path);
                if (mapped == null || bindings.containsKey(id)) {
                    skipped++;
                    return;
                }
                SettingMeta.Builder meta = new SettingMeta.Builder(id, ModSettings.routeOf(modId),
                        titleOf(spec, path), mapped.type(), SettingSource.MODS)
                        .scope(scopeOf(spec.restartType()));
                if (!description.isBlank()) {
                    meta.descriptionKey(description);
                }
                metas.add(meta.build());
                bindings.put(id, mapped.binding());
            } catch (Throwable t) {
                skipped++;
                Initializer.LOGGER.debug("Skipped a setting of {}", modId, t);
            }
        }
    }
}
