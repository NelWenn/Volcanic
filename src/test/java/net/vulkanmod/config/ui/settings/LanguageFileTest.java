package net.vulkanmod.config.ui.settings;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LanguageFileTest {
    private static final Path LANG = Path.of("src/main/resources/assets/vulkanmod/lang/en_us.json");

    @Test
    void theFileIsStructurallyLoadable() throws IOException {
        List<String> lines = body();

        assertFalse(lines.isEmpty(), "no entries found in " + LANG.toAbsolutePath());
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            boolean last = index == lines.size() - 1;
            if (last) {
                assertFalse(line.endsWith(","),
                        "the last entry must not end with a comma, or the whole file fails to parse: " + line);
            } else {
                assertTrue(line.endsWith(","), "entry " + (index + 1) + " is missing its comma: " + line);
            }
        }
    }

    @Test
    void bracesAreBalancedAndTheFileIsAnObject() throws IOException {
        List<String> all = Files.readAllLines(LANG);

        assertEquals("{", all.get(0).trim());
        assertEquals("}", all.get(all.size() - 1).trim());
    }

    @Test
    void everyEntryIsAQuotedKeyAndValue() throws IOException {
        for (String line : body()) {
            assertTrue(line.matches("^\"[^\"]+\"\\s*:\\s*\".*\",?$"),
                    "malformed entry: " + line);
        }
    }

    @Test
    void noKeyIsDeclaredTwice() throws IOException {
        Set<String> seen = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (String line : body()) {
            String key = line.substring(1, line.indexOf('"', 1));
            if (!seen.add(key)) {
                duplicates.add(key);
            }
        }
        assertEquals(List.of(), duplicates, "a duplicated key silently shadows the first one");
    }

    private static List<String> body() throws IOException {
        assertTrue(Files.isRegularFile(LANG), "lang file missing at " + LANG.toAbsolutePath());
        List<String> entries = new ArrayList<>();
        for (String raw : Files.readAllLines(LANG)) {
            String line = raw.trim();
            if (line.isEmpty() || line.equals("{") || line.equals("}")) {
                continue;
            }
            entries.add(line);
        }
        return entries;
    }
}
