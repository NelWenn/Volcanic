package net.vulkanmod.config.ui.mods;

import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.settings.SettingBinding;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public record ModSettings(String modId, List<SettingMeta> metas, Map<SettingId, SettingBinding> bindings) {

    public ModSettings {
        if (modId == null || modId.isBlank()) {
            throw new IllegalArgumentException("modId must not be blank");
        }
        if (metas == null || bindings == null) {
            throw new IllegalArgumentException("metas and bindings must not be null");
        }
        metas = List.copyOf(metas);
        bindings = Map.copyOf(bindings);
        if (metas.size() != bindings.size()) {
            throw new IllegalArgumentException("every setting of " + modId + " needs one binding, got "
                    + metas.size() + " settings and " + bindings.size() + " bindings");
        }
    }

    public static RouteId routeOf(String modId) {
        if (modId == null || modId.isBlank()) {
            throw new IllegalArgumentException("modId must not be blank");
        }
        return RouteId.parse("mods").child(modId);
    }

    public RouteId route() {
        return routeOf(modId);
    }

    public static String slugOf(String modName) {
        if (modName == null) {
            throw new IllegalArgumentException("modName must not be null");
        }
        StringBuilder slug = new StringBuilder();
        for (char character : modName.toLowerCase(Locale.ROOT).toCharArray()) {
            if (character >= 'a' && character <= 'z' || character >= '0' && character <= '9') {
                slug.append(character);
            } else if (slug.length() > 0 && slug.charAt(slug.length() - 1) != '_') {
                slug.append('_');
            }
        }
        while (slug.length() > 0 && slug.charAt(slug.length() - 1) == '_') {
            slug.setLength(slug.length() - 1);
        }
        if (slug.length() == 0) {
            throw new IllegalArgumentException("no mod id can be derived from " + modName);
        }
        return slug.toString();
    }
}
