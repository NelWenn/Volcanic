package net.vulkanmod.config.ui.settings;

import net.vulkanmod.config.ui.core.ApplyScope;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingSource;
import net.vulkanmod.config.ui.core.SettingType;

import java.util.List;

public final class PluginParameters {
    public static final RouteId SHADER_ROUTE = RouteId.parse("shaders.settings");

    private PluginParameters() {
    }

    // TODO(revo) — le menu sait déjà tout afficher, il manque la source.
    //
    // À accrocher sur RenderPipelinePlugin, PAS sur un type propre aux shaders : un plugin
    // n'est pas forcément un shader (Caldera, un overlay de debug…) et devrait pouvoir
    // exposer ses réglages pareil. Un seul mécanisme pour tous les plugins.
    //
    // Renvoie un SettingMeta par paramètre :
    //   new SettingMeta.Builder(
    //           SettingId.of(pluginId, "sun.intensity"),  // namespace = id() du plugin
    //           SHADER_ROUTE,                             // placement : à nous, pas à toi
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
