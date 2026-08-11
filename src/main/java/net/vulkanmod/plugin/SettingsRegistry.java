package net.vulkanmod.plugin;

import net.vulkanmod.plugin.hooks.annotations.SettingsCategory;
import net.vulkanmod.plugin.hooks.annotations.SettingsField;
import net.vulkanmod.plugin.hooks.events.SettingsFieldObject;
import net.vulkanmod.plugin.hooks.events.settings.SettingsModifiedFieldEvent;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SettingsRegistry {

    public static final class FieldEntry {
        public final SettingsFieldObject<Object>   field;
        public final String                        description;

        private final Field    reflectField;
        private final Object   owner;

        private FieldEntry(SettingsFieldObject<Object> field, String description, Field reflectField, Object owner) {
            this.field = field;
            this.description = description;
            this.reflectField = reflectField;
            this.owner = owner;
        }
    }

    public static final class CategoryEntry {
        public final String                        id;
        public final String                        nameKey;
        public final Map<String, FieldEntry>       fields = new LinkedHashMap<>();

        private CategoryEntry(String id, String nameKey) {
            this.id = id;
            this.nameKey = nameKey;
        }
    }

    private static final Map<String, Map<String, CategoryEntry>> PLUGIN_SETTINGS = new ConcurrentHashMap<>();

    private SettingsRegistry() {}

    public static void register(String pluginId, Object settingsHolder) {
        Class<?> clazz = settingsHolder.getClass();
        SettingsCategory categoryAnnotation = clazz.getAnnotation(SettingsCategory.class);

        if (categoryAnnotation == null)
            throw new IllegalArgumentException(clazz.getName() + " is not annotated with @SettingsCategory");

        CategoryEntry category = PLUGIN_SETTINGS
                .computeIfAbsent(pluginId, id -> new LinkedHashMap<>())
                .computeIfAbsent(categoryAnnotation.id(), id -> new CategoryEntry(id, categoryAnnotation.nameKey()));

        for (Field field : clazz.getDeclaredFields()) {
            SettingsField fieldAnnotation = field.getAnnotation(SettingsField.class);

            if (fieldAnnotation == null)
                continue;

            field.setAccessible(true);
            Object value;

            try {
                value = field.get(settingsHolder);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Unable to read " + field.getName() + " on " + clazz.getName(), e);
            }

            SettingsFieldObject<Object> fieldObject = new SettingsFieldObject<>(fieldAnnotation.nameKey(), fieldAnnotation.descriptionKey(), value);
            category.fields.put(field.getName(), new FieldEntry(fieldObject, fieldAnnotation.descriptionKey(), field, settingsHolder));
        }
    }

    public static Map<String, CategoryEntry> getCategories(String pluginId) {
        return Collections.unmodifiableMap(PLUGIN_SETTINGS.getOrDefault(pluginId, Collections.emptyMap()));
    }

    public static CategoryEntry getCategory(String pluginId, String categoryId) {
        Map<String, CategoryEntry> categories = PLUGIN_SETTINGS.get(pluginId);
        return categories == null ? null : categories.get(categoryId);
    }

    public static FieldEntry getField(String pluginId, String categoryId, String fieldName) {
        CategoryEntry category = getCategory(pluginId, categoryId);
        return category == null ? null : category.fields.get(fieldName);
    }

    @SuppressWarnings("unchecked")
    public static <T> void setValue(String pluginId, String categoryId, String fieldName, T newValue) {
        FieldEntry entry = getField(pluginId, categoryId, fieldName);
        if (entry == null)
            throw new IllegalStateException("Unknown field: " + pluginId + "/" + categoryId + "/" + fieldName);

        try {
            entry.reflectField.set(entry.owner, newValue);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to read field " + fieldName + " on " + entry.owner.getClass().getName(), e);
        }

        entry.field.value = newValue;

        HookRegistry.post(new SettingsModifiedFieldEvent<T>() {
            @Override public long when() { return System.currentTimeMillis(); }
            @Override public T type() { return newValue; }
            @Override public SettingsFieldObject<T> field() { return (SettingsFieldObject<T>) entry.field; }
        });
    }

    public static void clear(String pluginId) {
        PLUGIN_SETTINGS.remove(pluginId);
    }
}
