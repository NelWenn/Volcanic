package net.vulkanmod.rendergraph.radiance.settings;

import net.vulkanmod.plugin.hooks.annotations.SettingsCategory;
import net.vulkanmod.plugin.hooks.annotations.SettingsField;
import org.jetbrains.annotations.TestOnly;

@TestOnly
@SettingsCategory(id = "render", nameKey = "Radiance Rendering", descriptionKey = "Graphics settings for radiance rendering")
public class RadianceRenderSettings {

    // TODO: remplace hardcoded names by translation keys
    @SettingsField(nameKey = "Shadow Texture Size", descriptionKey = "Quality of the shadow texture")
    public int shadowQuality = 4096;

    @SettingsField(nameKey = "Reflection Quality", descriptionKey = "Quality of the reflection rendering")
    public int reflectionQuality = 2;
}
