package net.vulkanmod.config.ui.settings;

import net.vulkanmod.config.ui.core.SettingMeta;

import java.util.List;

public final class ShaderParameters {
    private ShaderParameters() {
    }

    // TODO(revo): return the active shader plugin's parameters here.
    // Discovery is ServiceLoader over net.vulkanmod.render.plugin.RenderPipelinePlugin;
    // id() and name() give the shader list, but no parameter API exists yet.
    public static List<SettingMeta> of(String pluginId) {
        return List.of();
    }
}
