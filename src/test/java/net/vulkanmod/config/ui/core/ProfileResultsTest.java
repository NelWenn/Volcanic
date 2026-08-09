package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileResultsTest {
    private static ProfileResults.Result result(int fps, int frames) {
        return new ProfileResults.Result(fps, 1000.0f / fps, 1000.0f / fps * 1.4f, frames);
    }

    @Test
    void eachProfileKeepsItsOwnFigureWhenAnotherIsMeasured() {
        ProfileResults results = new ProfileResults();
        results.record("quality", result(360, 1200));
        results.record("balanced", result(410, 900));

        assertEquals(360, results.of("quality").orElseThrow().fps(),
                "measuring balanced must not move quality's number");
        assertEquals(410, results.of("balanced").orElseThrow().fps());
        assertEquals(2, results.size());
    }

    @Test
    void aProfileNeverMeasuredHasNothingRatherThanAZero() {
        ProfileResults results = new ProfileResults();
        assertTrue(results.of("ultra").isEmpty());
        assertFalse(results.has("ultra"));
        assertTrue(results.of(null).isEmpty());
    }

    @Test
    void aLongerRunReplacesAShorterOne() {
        ProfileResults results = new ProfileResults();
        results.record("quality", result(300, 400));
        results.record("quality", result(355, 1500));

        assertEquals(355, results.of("quality").orElseThrow().fps());
    }

    @Test
    void aShorterRunDoesNotDisplaceASettledOne() {
        ProfileResults results = new ProfileResults();
        results.record("quality", result(355, 1500));
        results.record("quality", result(120, 310));

        assertEquals(355, results.of("quality").orElseThrow().fps(),
                "a brief sample must not overwrite a long one");
    }

    @Test
    void forgettingOneLeavesTheOthers() {
        ProfileResults results = new ProfileResults();
        results.record("quality", result(360, 1200));
        results.record("balanced", result(410, 900));
        results.forget("quality");

        assertFalse(results.has("quality"));
        assertTrue(results.has("balanced"));

        results.clear();
        assertEquals(0, results.size());
    }

    @Test
    void rejectsInvalidInput() {
        ProfileResults results = new ProfileResults();
        assertThrows(IllegalArgumentException.class, () -> results.record(" ", result(60, 500)));
        assertThrows(IllegalArgumentException.class, () -> results.record("quality", null));
        assertThrows(IllegalArgumentException.class, () -> new ProfileResults.Result(0, 1, 1, 100));
        assertThrows(IllegalArgumentException.class, () -> new ProfileResults.Result(60, 1, 1, 0));
    }
}
