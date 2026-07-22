package net.vulkanmod.render.ctm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeightedPickerTest {
    @Test
    void allWeightOnLastIndex() {
        int[] w = {0, 0, 5};
        for (long s = 0; s < 50; s++) assertEquals(2, WeightedPicker.pick(w, s));
    }
    @Test
    void deterministicForSameSeed() {
        int[] w = {1, 1, 45};
        assertEquals(WeightedPicker.pick(w, 12345L), WeightedPicker.pick(w, 12345L));
    }
    @Test
    void indexInRange() {
        int[] w = {3, 2, 1};
        for (long s = 0; s < 100; s++) {
            int i = WeightedPicker.pick(w, s);
            assertTrue(i >= 0 && i < 3);
        }
    }
}
