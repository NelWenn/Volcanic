package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SettingIdTest {
    @Test
    void roundTripsThroughString() {
        SettingId id = SettingId.of("vulkanmod", "culling.occlusion");
        assertEquals("vulkanmod:culling.occlusion", id.toString());
        assertEquals(id, SettingId.parse("vulkanmod:culling.occlusion"));
    }

    @Test
    void parsesNamespaceAndPath() {
        SettingId id = SettingId.parse("create:common/kinetics.max_rotation_speed");
        assertEquals("create", id.namespace());
        assertEquals("common/kinetics.max_rotation_speed", id.path());
    }

    @Test
    void rejectsMissingSeparator() {
        assertThrows(IllegalArgumentException.class, () -> SettingId.parse("culling.occlusion"));
    }

    @Test
    void rejectsBlankParts() {
        assertThrows(IllegalArgumentException.class, () -> SettingId.parse(":path"));
        assertThrows(IllegalArgumentException.class, () -> SettingId.parse("ns:"));
        assertThrows(IllegalArgumentException.class, () -> SettingId.of("ns", "  "));
    }

    @Test
    void equalIdsShareHashCode() {
        assertEquals(SettingId.of("a", "b").hashCode(), SettingId.parse("a:b").hashCode());
    }
}
