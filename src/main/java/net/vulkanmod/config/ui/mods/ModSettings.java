package net.vulkanmod.config.ui.mods;

import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.settings.SettingBinding;

import java.util.List;
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
}
