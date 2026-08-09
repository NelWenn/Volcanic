package net.vulkanmod.config.ui.settings;

import net.vulkanmod.config.ui.core.ApplyScope;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingSource;
import net.vulkanmod.config.ui.core.SettingType;

import java.util.List;

public final class ShaderParameters {
    public static final RouteId ROUTE = RouteId.parse("shaders.settings");

    private ShaderParameters() {
    }

    // TODO(revo) — le menu sait déjà afficher tout ça, il manque juste la source.
    //
    // Renvoie ici les paramètres du plugin actif. Un SettingMeta par paramètre :
    //   new SettingMeta.Builder(
    //           SettingId.of(pluginId, "sun.intensity"),  // namespace = id() du plugin
    //           ROUTE,                                    // placement : à nous, pas à toi
    //           "shader.radiance.sun_intensity",          // clé de trad, jamais un littéral
    //           SettingType.FLOAT,                        // BOOL / INT / FLOAT / ENUM
    //           SettingSource.SHADERS)
    //       .scope(ApplyScope.INSTANT)   // ou CHUNK_REBUILD / RESTART si ça recompile
    //       .advanced(true)
    //       .build()
    //
    // Ne mets pas de chemin d'UI dans ton annotation : un `group` logique suffit, on mappe.
    // Si l'id encode le placement, déplacer un paramètre casse les favoris et les liens.
    //
    // Les annotations doivent être en RetentionPolicy.RUNTIME — on les lit par réflexion au
    // chargement du plugin. En CLASS (le défaut) on ne voit rien, et l'échec est silencieux.
    //
    // Il faut aussi un SettingBinding par paramètre : SettingBinding.ranged(getter, setter,
    // min, max, step) pour un curseur, .choosing(...) pour un enum, .of(...) pour un booléen.
    public static List<SettingMeta> of(String pluginId) {
        if (pluginId == null || pluginId.isBlank()) {
            throw new IllegalArgumentException("pluginId must not be blank");
        }
        return List.of();
    }
}
