package net.vulkanmod.config.ui.settings;

import net.vulkanmod.config.ui.core.ProfileResults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BenchmarkStoreTest {
    private static final List<String> KEYS = List.of("quality", "balanced");

    private static ProfileResults with(String key, int fps, int frames) {
        ProfileResults results = new ProfileResults();
        results.record(key, new ProfileResults.Result(fps, 1000.0f / fps, 1000.0f / fps * 1.4f, frames));
        return results;
    }

    @Test
    void aSavedResultComesBackInTheSameContext(@TempDir Path dir) {
        Path file = dir.resolve("b.json");
        BenchmarkStore.save(file, 42, with("quality", 360, 1200), KEYS, null);

        ProfileResults loaded = new ProfileResults();
        BenchmarkStore.load(file, 42, loaded, null);

        assertEquals(360, loaded.of("quality").orElseThrow().fps());
        assertEquals(1200, loaded.of("quality").orElseThrow().frames());
    }

    @Test
    void aDifferentContextThrowsTheFiguresAwayRatherThanLying(@TempDir Path dir) {
        Path file = dir.resolve("b.json");
        BenchmarkStore.save(file, 42, with("quality", 360, 1200), KEYS, null);

        ProfileResults loaded = new ProfileResults();
        BenchmarkStore.load(file, 99, loaded, null);

        assertEquals(0, loaded.size(), "a changed world or resolution invalidates every stored figure");
    }

    @Test
    void anAbsentFileIsNotAnError(@TempDir Path dir) {
        ProfileResults loaded = new ProfileResults();
        BenchmarkStore.load(dir.resolve("nothing.json"), 1, loaded, null);
        assertEquals(0, loaded.size());
    }

    @Test
    void acorruptFileCostsOnlyTheFigures(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("b.json");
        Files.writeString(file, "garbage\nmore garbage");

        ProfileResults loaded = new ProfileResults();
        assertDoesNotThrow(() -> BenchmarkStore.load(file, 1, loaded, null));
        assertEquals(0, loaded.size());
    }

    @Test
    void anEmptyOrTruncatedFileIsIgnored(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("b.json");
        Files.writeString(file, "");

        ProfileResults loaded = new ProfileResults();
        assertDoesNotThrow(() -> BenchmarkStore.load(file, 1, loaded, null));
        assertEquals(0, loaded.size());
    }

    @Test
    void onlyTheNamedProfilesAreWritten(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("b.json");
        ProfileResults results = with("quality", 360, 1200);
        results.record("secret", new ProfileResults.Result(10, 100f, 140f, 400));
        BenchmarkStore.save(file, 7, results, KEYS, null);

        assertFalse(Files.readString(file).contains("secret"));

        ProfileResults loaded = new ProfileResults();
        BenchmarkStore.load(file, 7, loaded, null);
        assertEquals(1, loaded.size());
    }

    @Test
    void rejectsInvalidInput(@TempDir Path dir) {
        assertThrows(IllegalArgumentException.class,
                () -> BenchmarkStore.load(null, 1, new ProfileResults(), null));
        assertThrows(IllegalArgumentException.class,
                () -> BenchmarkStore.load(dir.resolve("x"), 1, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> BenchmarkStore.save(null, 1, new ProfileResults(), KEYS, null));
    }
}
