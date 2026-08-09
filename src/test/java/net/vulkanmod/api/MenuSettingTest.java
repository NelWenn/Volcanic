package net.vulkanmod.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MenuSettingTest {

    @Test
    void aToggleRoundTripsThroughItsOwnAccessors() {
        AtomicBoolean held = new AtomicBoolean(false);
        MenuSetting setting = MenuSetting.toggle("lod", "terrain", "caldera.option.lod",
                held::get, held::set);

        assertEquals(MenuSetting.Kind.TOGGLE, setting.kind());
        assertEquals(Boolean.FALSE, setting.get());
        setting.set(Boolean.TRUE);
        assertTrue(held.get());
        assertEquals(Boolean.TRUE, setting.get());
    }

    @Test
    void aSliderCarriesItsRangeAndWritesThrough() {
        AtomicInteger held = new AtomicInteger(16);
        MenuSetting setting = MenuSetting.slider("rd", "terrain", "caldera.option.rd",
                4, 128, 4, held::get, held::set);

        assertEquals(4, setting.min());
        assertEquals(128, setting.max());
        assertEquals(4, setting.step());
        setting.set(64);
        assertEquals(64, held.get());
    }

    @Test
    void aChoiceKeepsItsOptionsInOrder() {
        AtomicReference<String> held = new AtomicReference<>("off");
        MenuSetting setting = MenuSetting.choice("mode", "terrain", "caldera.option.mode",
                List.of("off", "safe", "full"), held::get, held::set);

        assertEquals(List.of("off", "safe", "full"), setting.choices());
        setting.set("full");
        assertEquals("full", held.get());
    }

    @Test
    void anAbsentGroupFallsBackRatherThanProducingABlankTab() {
        assertEquals("general",
                MenuSetting.toggle("k", null, "t", () -> true, v -> { }).group());
        assertEquals("general",
                MenuSetting.toggle("k", "  ", "t", () -> true, v -> { }).group());
    }

    @Test
    void restartIsOptInAndDoesNotDisturbTheRest() {
        MenuSetting plain = MenuSetting.toggle("k", "g", "t", () -> true, v -> { });
        MenuSetting restart = plain.requiringRestart();

        assertFalse(plain.restartRequired());
        assertTrue(restart.restartRequired());
        assertEquals(plain.key(), restart.key());
        assertEquals(plain.group(), restart.group());
        assertEquals(plain.kind(), restart.kind());
    }

    @Test
    void aPluginThatDeclaresNothingIsStillValid() {
        MenuPlugin bare = new MenuPlugin() {
            @Override
            public String id() {
                return "bare";
            }

            @Override
            public String displayName() {
                return "Bare";
            }
        };

        assertTrue(bare.settings().isEmpty());
        assertTrue(bare.enabled(), "a plugin is enabled unless it says otherwise");
        assertDoesNotThrow(bare::onApply);
    }

    @Test
    void aSettingRemembersItsStartingValueSoResetHasSomethingToGoBackTo() {
        AtomicInteger held = new AtomicInteger(16);
        MenuSetting setting = MenuSetting.slider("rd", "terrain", "t", 4, 128, 4, held::get, held::set);

        assertEquals(16, setting.defaultValue());
        setting.set(64);
        assertEquals(16, setting.defaultValue(), "the default must not drift with the value");
    }

    @Test
    void anExplicitDefaultAndDescriptionSurviveTheOtherModifiers() {
        MenuSetting setting = MenuSetting.toggle("k", "g", "t", () -> true, v -> { })
                .withDefault(Boolean.FALSE)
                .describedBy("caldera.option.k.tooltip")
                .requiringRestart();

        assertEquals(Boolean.FALSE, setting.defaultValue());
        assertEquals("caldera.option.k.tooltip", setting.descriptionKey());
        assertTrue(setting.restartRequired());
        assertEquals("k", setting.key());
    }

    @Test
    void aSettingWithoutADescriptionSaysSoRatherThanInventingOne() {
        assertNull(MenuSetting.toggle("k", "g", "t", () -> true, v -> { }).descriptionKey());
    }

    @Test
    void rejectsDeclarationsTheMenuCouldNotRender() {
        assertThrows(IllegalArgumentException.class,
                () -> MenuSetting.toggle(" ", "g", "t", () -> true, v -> { }));
        assertThrows(IllegalArgumentException.class,
                () -> MenuSetting.toggle("k", "g", " ", () -> true, v -> { }));
        assertThrows(IllegalArgumentException.class,
                () -> MenuSetting.slider("k", "g", "t", 10, 4, 1, () -> 5, v -> { }));
        assertThrows(IllegalArgumentException.class,
                () -> MenuSetting.slider("k", "g", "t", 0, 10, 0, () -> 5, v -> { }));
        assertThrows(IllegalArgumentException.class,
                () -> MenuSetting.choice("k", "g", "t", List.of(), () -> "a", v -> { }));
        assertThrows(IllegalArgumentException.class,
                () -> MenuSetting.toggle("k", "g", "t", () -> true, v -> { }).withDefault(null));
        assertThrows(IllegalArgumentException.class,
                () -> MenuSetting.toggle("k", "g", "t", () -> true, v -> { }).describedBy(" "));
    }
}
