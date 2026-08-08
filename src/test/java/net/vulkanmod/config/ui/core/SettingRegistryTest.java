package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SettingRegistryTest {
    private static final RouteId GENERAL = RouteId.parse("display.general");
    private static final RouteId INTERFACE = RouteId.parse("display.interface");

    private static SettingMeta meta(String id, RouteId route) {
        return new SettingMeta.Builder(SettingId.parse(id), route, "k." + id,
                SettingType.BOOL, SettingSource.VOLCANIC).build();
    }

    @Test
    void keepsRegistrationOrderWithinARoute() {
        SettingRegistry registry = new SettingRegistry();
        registry.register(meta("vulkanmod:a", GENERAL));
        registry.register(meta("vulkanmod:b", GENERAL));
        registry.register(meta("vulkanmod:c", INTERFACE));

        assertEquals(List.of(SettingId.parse("vulkanmod:a"), SettingId.parse("vulkanmod:b")),
                registry.forRoute(GENERAL).stream().map(SettingMeta::id).toList());
        assertEquals(1, registry.forRoute(INTERFACE).size());
        assertEquals(3, registry.size());
    }

    @Test
    void aRouteWithNoSettingsIsEmptyRatherThanNull() {
        assertTrue(new SettingRegistry().forRoute(GENERAL).isEmpty());
    }

    @Test
    void registeringTheSameIdTwiceIsRejected() {
        SettingRegistry registry = new SettingRegistry();
        registry.register(meta("vulkanmod:a", GENERAL));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> registry.register(meta("vulkanmod:a", INTERFACE)));
        assertTrue(error.getMessage().contains("vulkanmod:a"));
    }

    @Test
    void anUnknownIdThrowsRatherThanReturningNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new SettingRegistry().get(SettingId.parse("vulkanmod:nope")));
        assertFalse(new SettingRegistry().contains(SettingId.parse("vulkanmod:nope")));
    }

    @Test
    void theRouteListingCannotBeMutatedByItsCaller() {
        SettingRegistry registry = new SettingRegistry();
        registry.register(meta("vulkanmod:a", GENERAL));
        List<SettingMeta> listing = registry.forRoute(GENERAL);
        assertThrows(UnsupportedOperationException.class, () -> listing.add(meta("vulkanmod:b", GENERAL)));
    }

    @Test
    void rejectsNullInput() {
        SettingRegistry registry = new SettingRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.register(null));
        assertThrows(IllegalArgumentException.class, () -> registry.forRoute(null));
        assertThrows(IllegalArgumentException.class, () -> registry.get(null));
    }
}
