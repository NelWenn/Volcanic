package net.vulkanmod.config.ui.settings;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
    void aScaledBindingReadsTheBackendFloatAsAStepGridInteger() {
        AtomicReference<Object> backend = new AtomicReference<>(0.67f);
        SettingBinding binding = SettingBinding.scaled(backend::get, backend::set, 0, 100, 5, 100);

        assertEquals(67, binding.get());
        assertEquals(0, binding.min());
        assertEquals(100, binding.max());
        assertEquals(5, binding.step());
    }

    @Test
    void aScaledBindingWritesTheGridIntegerBackAsTheBackendFloat() {
        AtomicReference<Object> backend = new AtomicReference<>(0.0f);
        SettingBinding binding = SettingBinding.scaled(backend::get, backend::set, 0, 100, 5, 100);

        binding.set(65);
        assertEquals(0.65f, ((Number) backend.get()).floatValue(), 1.0e-6f);
    }

    @Test
    void aScaledValueReadsAsTheFloatItStandsFor() {
        AtomicReference<Object> backend = new AtomicReference<>(0.67f);
        SettingBinding binding = SettingBinding.scaled(backend::get, backend::set, 0, 100, 5, 100);

        assertEquals("0.67", binding.display(67));
        assertEquals("1.0", binding.display(100));
        assertEquals("0.0", binding.display(0));
        assertEquals("2.0", SettingBinding.scaled(backend::get, backend::set, 0, 200, 10, 100).display(200));
    }

    @Test
    void everyStepOfAScaledRangeSurvivesTheRoundTripToTheBackendAndBack() {
        AtomicReference<Object> backend = new AtomicReference<>(0.0f);
        SettingBinding binding = SettingBinding.scaled(
                () -> ((Number) backend.get()).floatValue(),
                value -> backend.set(((Number) value).floatValue()),
                0, 200, 10, 100);

        for (int value = 0; value <= 200; value += 10) {
            binding.set(value);
            assertEquals(value, binding.get(), "round trip failed for " + value);
        }
    }

    @Test
    void aFormatterOnAScaledBindingSeesTheGridValueTheSliderWorksIn() {
        AtomicReference<Object> backend = new AtomicReference<>(0.67f);
        SettingBinding binding = SettingBinding.scaled(backend::get, backend::set, 0, 100, 5, 100)
                .withFormatter(value -> value + "%");

        assertEquals("67%", binding.display(binding.get()));
    }

    @Test
    void aCopyOfAScaledBindingKeepsItsScale() {
        AtomicReference<Object> backend = new AtomicReference<>(0.67f);
        SettingBinding binding = SettingBinding.scaled(backend::get, backend::set, 0, 100, 5, 100)
                .withDefault(() -> 0);

        assertEquals(67, binding.get());
        assertEquals("0.67", binding.display(67));
        assertEquals(0, binding.defaultValue());

        binding.set(binding.defaultValue());
        assertEquals(0.0f, ((Number) backend.get()).floatValue(), 1.0e-6f);
        assertEquals(0, binding.get());
    }

    @Test
    void aComputedMaxIsReReadEveryTimeTheRangeIsAsked() {
        AtomicInteger max = new AtomicInteger(3);
        AtomicReference<Object> cell = new AtomicReference<>(0);
        SettingBinding binding = SettingBinding.ranged(cell::get, cell::set, 0, max::get, 1);

        assertEquals(0, binding.min());
        assertEquals(3, binding.max());

        max.set(7);
        assertEquals(7, binding.max());
    }

    @Test
    void aComputedMaxRangeStillRejectsAnInvalidStep() {
        AtomicReference<Object> cell = new AtomicReference<>(0);
        assertThrows(IllegalArgumentException.class,
                () -> SettingBinding.ranged(cell::get, cell::set, 0, () -> 3, 0));
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> plain().withFormatter(null));
        assertThrows(IllegalArgumentException.class, () -> plain().display(null));
    }

    @Test
    void aScaledBindingRejectsInvalidInput() {
        AtomicReference<Object> cell = new AtomicReference<>(0.5f);
        assertThrows(IllegalArgumentException.class,
                () -> SettingBinding.scaled(cell::get, cell::set, 0, 100, 5, 0));
        assertThrows(IllegalArgumentException.class,
                () -> SettingBinding.scaled(cell::get, cell::set, 100, 0, 5, 100));
        assertThrows(IllegalArgumentException.class,
                () -> SettingBinding.scaled(cell::get, cell::set, 0, 100, 0, 100));

        SettingBinding binding = SettingBinding.scaled(cell::get, cell::set, 0, 100, 5, 100);
        assertThrows(IllegalArgumentException.class, () -> binding.set("half"));
        assertThrows(IllegalArgumentException.class, () -> binding.display("half"));

        AtomicReference<Object> text = new AtomicReference<>("half");
        assertThrows(IllegalArgumentException.class,
                () -> SettingBinding.scaled(text::get, text::set, 0, 100, 5, 100).get());
    }
}
