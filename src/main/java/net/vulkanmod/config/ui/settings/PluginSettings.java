package net.vulkanmod.config.ui.settings;

import net.vulkanmod.api.MenuPlugin;
import net.vulkanmod.api.MenuSetting;
import net.vulkanmod.config.ui.core.ApplyScope;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingSource;
import net.vulkanmod.config.ui.core.SettingType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    public static SettingId idOf(String pluginId, MenuSetting setting) {
        return SettingId.of(requireId(pluginId),
                PATH_PREFIX + setting.group().toLowerCase(Locale.ROOT) + "/" + setting.key());
    }

    public static Converted convert(String pluginId, List<MenuSetting> settings) {
        requireId(pluginId);
        if (settings == null) {
            throw new IllegalArgumentException("settings must not be null");
        }
        List<SettingMeta> metas = new ArrayList<>();
        Map<SettingId, SettingBinding> bindings = new LinkedHashMap<>();
        for (MenuSetting setting : settings) {
            SettingId id = idOf(pluginId, setting);
            if (bindings.containsKey(id)) {
                continue;
            }
            metas.add(metaOf(pluginId, id, setting));
            bindings.put(id, bindingOf(setting));
        }
        return new Converted(List.copyOf(metas), Map.copyOf(bindings));
    }

    public static Converted convert(MenuPlugin plugin, List<MenuSetting> settings) {
        return convert(plugin.id(), settings);
    }

    private static SettingMeta metaOf(String pluginId, SettingId id, MenuSetting setting) {
        SettingMeta.Builder builder = new SettingMeta.Builder(id,
                routeOf(pluginId, setting.group()), setting.titleKey(), typeOf(setting),
                SettingSource.PLUGINS)
                .scope(setting.restartRequired() ? ApplyScope.RESTART : ApplyScope.INSTANT);
        if (setting.descriptionKey() != null) {
            builder.descriptionKey(setting.descriptionKey());
        }
        return builder.build();
    }

    private static SettingType typeOf(MenuSetting setting) {
        return switch (setting.kind()) {
            case TOGGLE -> SettingType.BOOL;
            case SLIDER -> SettingType.INT;
            case CHOICE -> SettingType.ENUM;
        };
    }

    private static SettingBinding bindingOf(MenuSetting setting) {
        SettingBinding binding = switch (setting.kind()) {
            case TOGGLE -> SettingBinding.of(setting::get, setting::set);
            case SLIDER -> SettingBinding.ranged(setting::get, setting::set,
                    setting.min(), setting.max(), setting.step());
            case CHOICE -> SettingBinding.choosing(setting::get, setting::set, setting::choices);
        };
        Object fallback = setting.defaultValue();
        return fallback == null ? binding : binding.withDefault(() -> fallback);
    }

    private static String requireId(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("pluginId must not be blank");
        }
        return pluginId;
    }
}
