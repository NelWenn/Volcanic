package net.vulkanmod.config.ui.mods;

import net.vulkanmod.config.ui.core.SettingId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModSettingsSlugTest {

    @Test
    void anInstalledStyleIdIsUnchanged() {
        assertEquals("caldera", ModSettings.slugOf("caldera"));
    }

    @Test
    void wordsAreLowercasedAndJoinedWithUnderscores() {
        assertEquals("caldera_lod", ModSettings.slugOf("Caldera LOD"));
    }

    @Test
    void digitsSurvive() {
        assertEquals("fps_2000", ModSettings.slugOf("FPS 2000"));
    }

    @Test
    void runsOfSeparatorsCollapseAndEdgesAreTrimmed() {
        assertEquals("a_b", ModSettings.slugOf("  ..A -- B!! "));
    }

    @Test
    void aNameWithoutLettersOrDigitsHasNoId() {
        assertThrows(IllegalArgumentException.class, () -> ModSettings.slugOf(" -- "));
    }

    @Test
    void nullIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ModSettings.slugOf(null));
    }

    @Test
    void theSlugIsUsableAsANamespaceAndAsARouteSegment() {
        String slug = ModSettings.slugOf("Some. Mod: Name");
        assertEquals("some_mod_name", slug);
        assertEquals("mods.some_mod_name", ModSettings.routeOf(slug).toString());
        assertDoesNotThrow(() -> SettingId.of(slug, "page/block/title"));
    }
}
