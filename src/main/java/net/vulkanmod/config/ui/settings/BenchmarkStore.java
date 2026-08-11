package net.vulkanmod.config.ui.settings;

import net.vulkanmod.config.ui.core.ProfileResults;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.Locale;

public final class BenchmarkStore {
    public static final Path PATH = Path.of("config", "volcanic_benchmarks.json");

    private static final String HEADER = "volcanic-benchmarks 1";
    private static final String CONTEXT = "context ";

    private BenchmarkStore() {
    }

    public static void load(Path path, int context, ProfileResults into, Consumer<String> log) {
        require(path, into);
        if (!Files.isRegularFile(path)) {
            return;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (IOException | RuntimeException failure) {
            note(log, "Ignoring unreadable benchmarks at " + path + ": " + failure);
            return;
        }
        if (lines.isEmpty() || !HEADER.equals(lines.get(0).trim())) {
            note(log, "Ignoring benchmarks at " + path + ": unknown format");
            return;
        }
        if (lines.size() < 2 || !lines.get(1).trim().startsWith(CONTEXT)) {
            return;
        }
        Integer stored = parseInt(lines.get(1).trim().substring(CONTEXT.length()));
        if (stored == null || stored != context) {
            note(log, "Dropping stored benchmarks: the machine, world or resolution changed");
            return;
        }
        for (int i = 2; i < lines.size(); i++) {
            record(lines.get(i).trim(), into);
        }
    }

    public static void save(Path path, int context, ProfileResults results, Iterable<String> keys, Consumer<String> log) {
        require(path, results);
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        lines.add(CONTEXT + context);
        for (String key : keys) {
            results.of(key).ifPresent(r -> lines.add(String.format(Locale.ROOT, "%s %d %.3f %.3f %d",
                    key, r.fps(), r.medianMs(), r.lowMs(), r.frames())));
        }
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(path, lines);
        } catch (IOException | RuntimeException failure) {
            note(log, "Failed to write benchmarks to " + path + ": " + failure);
        }
    }

    private static void record(String line, ProfileResults into) {
        if (line.isEmpty()) {
            return;
        }
        String[] parts = line.split(" ");
        if (parts.length != 5) {
            return;
        }
        Integer fps = parseInt(parts[1]);
        Float median = parseFloat(parts[2]);
        Float low = parseFloat(parts[3]);
        Integer frames = parseInt(parts[4]);
        if (fps == null || median == null || low == null || frames == null || fps <= 0 || frames <= 0) {
            return;
        }
        into.record(parts[0], new ProfileResults.Result(fps, median, low, frames));
    }

    private static Integer parseInt(String text) {
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    private static Float parseFloat(String text) {
        try {
            return Float.valueOf(text.trim());
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    private static void note(Consumer<String> log, String message) {
        if (log != null) {
            log.accept(message);
        }
    }

    private static void require(Path path, ProfileResults results) {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        if (results == null) {
            throw new IllegalArgumentException("results must not be null");
        }
    }
}
