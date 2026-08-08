package net.vulkanmod.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigMigrationTest {
    @Test
    void anUnversionedConfigIsTreatedAsVersionZeroAndUpgraded() {
        Config config = new Config();
        config.configVersion = 0;
        Config migrated = Config.migrate(config);
        assertEquals(Config.CURRENT_VERSION, migrated.configVersion);
    }

    @Test
    void aConfigAlreadyAtTheCurrentVersionIsUntouched() {
        Config config = new Config();
        config.configVersion = Config.CURRENT_VERSION;
        int frameQueue = config.frameQueueSize;
        assertSame(config, Config.migrate(config));
        assertEquals(frameQueue, config.frameQueueSize);
    }

    @Test
    void aVersionFromTheFutureIsRefusedRatherThanSilentlyDowngraded() {
        Config config = new Config();
        config.configVersion = Config.CURRENT_VERSION + 1;
        assertThrows(IllegalStateException.class, () -> Config.migrate(config));
    }

    @Test
    void theVersionFieldIsSerialisable() throws Exception {
        assertFalse(java.lang.reflect.Modifier.isPrivate(
                Config.class.getField("configVersion").getModifiers()));
    }
}
