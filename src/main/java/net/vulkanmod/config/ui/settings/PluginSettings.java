package net.vulkanmod.config.ui.settings;

import net.vulkanmod.Initializer;
import net.vulkanmod.config.ui.core.ApplyScope;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingSource;
import net.vulkanmod.config.ui.core.SettingType;
import net.vulkanmod.plugin.PluginEntry;
import net.vulkanmod.plugin.SettingsRegistry;

import java.util.*;

public final class PluginSettings {

    public static final RouteId ROOT = RouteId.parse("plugins");
    private static final String PATH_PREFIX = "plugin/";

    public record Converted(List<SettingMeta> metas, Map<SettingId, SettingBinding> bindings) {
    }

    private PluginSettings() {
    }

    public static RouteId routeOf(String pluginId) {
        return ROOT.child(requireId(pluginId));
    }

    public static RouteId routeOf(String pluginId, String group) {
        return routeOf(pluginId).child(group.toLowerCase(Locale.ROOT));
    }

    public static SettingId idOf(String pluginId, String categoryId, String fieldName) {
        return SettingId.of(requireId(pluginId),
                PATH_PREFIX + categoryId.toLowerCase(Locale.ROOT) + "/" + fieldName);
    }

    public static Converted convert(String pluginId, Collection<SettingsRegistry.CategoryEntry> categories) {
        requireId(pluginId);
        if (categories == null) {
            throw new IllegalArgumentException("categories must not be null");
        }

        List<SettingMeta> metas = new ArrayList<>();
        Map<SettingId, SettingBinding> bindings = new LinkedHashMap<>();

        for (SettingsRegistry.CategoryEntry category : categories) {
            for (Map.Entry<String, SettingsRegistry.FieldEntry> field : category.fields.entrySet()) {
                String fieldName = field.getKey();
                SettingsRegistry.FieldEntry fieldEntry = field.getValue();

                SettingType type = typeOf(fieldEntry.field.value);
                if (type == null) {
                    Initializer.LOGGER.warn("Plugin {} field {}/{} has an unsupported value type ({}), skipping",
                            pluginId, category.id, fieldName,
                            fieldEntry.field.value == null ? "null" : fieldEntry.field.value.getClass());
                    continue;
                }

                SettingId id = idOf(pluginId, category.id, fieldName);
                if (bindings.containsKey(id))
                    continue;

                metas.add(metaOf(pluginId, id, category, fieldEntry, type));
                bindings.put(id, bindingOf(pluginId, category.id, fieldName, fieldEntry, type));
            }
        }

        return new Converted(List.copyOf(metas), Map.copyOf(bindings));
    }

    public static Converted convert(PluginEntry plugin, Collection<SettingsRegistry.CategoryEntry> categories) {
        return convert(plugin.getPlugin().id(), categories);
    }

    private static SettingMeta metaOf(String pluginId, SettingId id, SettingsRegistry.CategoryEntry category,
                                      SettingsRegistry.FieldEntry entry, SettingType type) {
        SettingMeta.Builder builder = new SettingMeta.Builder(id,
                routeOf(pluginId, category.id), entry.field.nameKey, type, SettingSource.PLUGINS)
                .scope(ApplyScope.INSTANT);

        if (entry.field.descriptionKey != null) {
            builder.descriptionKey(entry.field.descriptionKey);
        }

        return builder.build();
    }

    private static SettingType typeOf(Object value) {
        if (value instanceof Boolean) {
            return SettingType.BOOL;
        }
        if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
            return SettingType.INT;
        }
        if (value instanceof Enum<?>) {
            return SettingType.ENUM;
        }
        return null;
    }

    private static SettingBinding bindingOf(String pluginId, String categoryId, String fieldName,
                                            SettingsRegistry.FieldEntry entry, SettingType type) {
        Object initial = entry.field.value;

        SettingBinding binding = switch (type) {
            case BOOL, INT, KEY -> SettingBinding.of(
                    () -> entry.field.value,
                    newValue -> SettingsRegistry.setValue(pluginId, categoryId, fieldName, newValue));
            case ENUM -> {
                Object[] choices = initial.getClass().getEnumConstants();
                yield SettingBinding.choosing(
                        () -> entry.field.value,
                        newValue -> SettingsRegistry.setValue(pluginId, categoryId, fieldName, newValue),
                        () -> List.of(Arrays.toString(choices)));
            }
        };

        return binding.withDefault(() -> initial);
    }

    private static String requireId(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("pluginId must not be blank");
        }
        return pluginId;
    }
}