package net.vulkanmod.config.ui.settings;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SettingBindingTest {
    private static SettingBinding plain() {
        AtomicReference<Object> cell = new AtomicReference<>(260);
        return SettingBinding.of(cell::get, cell::set);
    }

    @Test
    void withoutAFormatterAValueReadsAsItself() {
        assertEquals("260", plain().display(260));
    }

    @Test
    void aFormatterDecidesHowTheValueReads() {
        SettingBinding binding = plain().withFormatter(value -> value.equals(260) ? "Unlimited" : value.toString());
        assertEquals("Unlimited", binding.display(260));
        assertEquals("120", binding.display(120));
    }

    @Test
    void aFormatterDoesNotLeakIntoTheBindingItWasCopiedFrom() {
        SettingBinding binding = plain();
        binding.withFormatter(value -> "Unlimited");
        assertEquals("260", binding.display(260));
    }

    @Test
    void theDefaultAndTheFormatterComposeInEitherOrder() {
        SettingBinding first = plain().withDefault(() -> 120).withFormatter(value -> "read as " + value);
        assertTrue(first.hasDefault());
        assertEquals(120, first.defaultValue());
        assertEquals("read as 260", first.display(260));

        SettingBinding second = plain().withFormatter(value -> "read as " + value).withDefault(() -> 120);
        assertTrue(second.hasDefault());
        assertEquals(120, second.defaultValue());
        assertEquals("read as 260", second.display(260));
    }

    @Test
    void aCopyKeepsTheRangeAndTheChoices() {
        AtomicReference<Object> cell = new AtomicReference<>(30);
        SettingBinding ranged = SettingBinding.ranged(cell::get, cell::set, 10, 260, 10)
                .withFormatter(String::valueOf);
        assertEquals(10, ranged.min());
        assertEquals(260, ranged.max());
        assertEquals(10, ranged.step());

        SettingBinding choosing = SettingBinding.choosing(cell::get, cell::set, () -> List.of("a", "b"))
                .withFormatter(String::valueOf);
        assertEquals(List.of("a", "b"), choosing.choices());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> plain().withFormatter(null));
        assertThrows(IllegalArgumentException.class, () -> plain().display(null));
    }
}
