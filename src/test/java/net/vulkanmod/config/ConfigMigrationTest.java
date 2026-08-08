package net.vulkanmod.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMigrationTest {
    @Test
    void anUnversionedConfigIsTreatedAsVersionZeroAndUpgraded() {
        assertEquals(ConfigVersion.CURRENT, ConfigVersion.migrated(0));
    }

    @Test
    void aConfigAlreadyAtTheCurrentVersionStaysThere() {
        assertEquals(ConfigVersion.CURRENT, ConfigVersion.migrated(ConfigVersion.CURRENT));
    }

    @Test
    void aVersionFromTheFutureIsRefusedRatherThanSilentlyDowngraded() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> ConfigVersion.migrated(ConfigVersion.CURRENT + 1));
        assertTrue(error.getMessage().contains(String.valueOf(ConfigVersion.CURRENT + 1)));
    }

    @Test
    void aNegativeVersionIsRefused() {
        assertThrows(IllegalStateException.class, () -> ConfigVersion.migrated(-1));
    }

    @Test
    void theVersionFieldIsSerialisableByGson() throws Exception {
        assertFalse(Modifier.isPrivate(Config.class.getField("configVersion").getModifiers()));
    }
}
