package net.vulkanmod.rendergraph.radiance.settings;

import net.vulkanmod.plugin.hooks.annotations.SettingsCategory;
import net.vulkanmod.plugin.hooks.annotations.SettingsField;
import org.jetbrains.annotations.TestOnly;

@TestOnly
@SettingsCategory(id = "render", nameKey = "Radiance Rendering", descriptionKey = "Graphics settings for radiance rendering")
public class RadianceRenderSettings {

    // TODO: remplace hardcoded names by translation keys
    @SettingsField(nameKey = "Shadow Quality", descriptionKey = "Quality of the shadow rendering")
    public int shadowQuality = 4096;
}
