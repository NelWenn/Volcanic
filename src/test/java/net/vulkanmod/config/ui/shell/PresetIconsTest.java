package net.vulkanmod.config.ui.shell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PresetIconsTest {
    private static final String P = "vulkanmod.options.performancePreset.";

    @Test
    void everyPresetHasAnIconOfTheDeclaredSize() {
        for (String preset : new String[] {"performance", "balanced", "quality", "ultra", "custom"}) {
            String[] icon = PresetIcons.of(P + preset);
            assertNotNull(icon, preset + " has no icon");
            assertEquals(PresetIcons.SIZE, icon.length, preset + " is not " + PresetIcons.SIZE + " rows");
            for (String row : icon) {
                assertEquals(PresetIcons.SIZE, row.length(),
                        preset + " has a row of " + row.length() + ": " + row);
                assertTrue(row.matches("[.#]+"), preset + " row has a stray character: " + row);
            }
        }
    }

    @Test
    void noIconIsBlankOrCompletelyFilled() {
        for (String preset : new String[] {"performance", "balanced", "quality", "ultra", "custom"}) {
            int lit = 0;
            for (String row : PresetIcons.of(P + preset)) {
                lit += row.replace(".", "").length();
            }
            int total = PresetIcons.SIZE * PresetIcons.SIZE;
            assertTrue(lit > 4, preset + " is nearly empty");
            assertTrue(lit < total, preset + " is a solid block");
        }
    }

    @Test
    void anUnknownKeyHasNoIconRatherThanACrash() {
        assertNull(PresetIcons.of("vulkanmod.options.performancePreset.nope"));
        assertNull(PresetIcons.of(null));
    }
}
