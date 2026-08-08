package net.vulkanmod.config.ui.settings;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class OverviewLabelKeysTest {
    private static final Path CATALOG =
            Path.of("src/main/java/net/vulkanmod/config/ui/settings/SettingsCatalog.java");
    private static final Path LANG = Path.of("src/main/resources/assets/vulkanmod/lang/en_us.json");
    private static final Pattern LABEL_KEY = Pattern.compile("\"(vulkanmod\\.overview\\.[a-z_]+)\"");
    private static final Pattern ENTRY = Pattern.compile("^\\s*\"([^\"]+)\"\\s*:\\s*\"(.*)\"\\s*,?\\s*$");

    @Test
    void theOverviewRowsAreTheOnesTheSettingsMapLists() throws IOException {
        assertEquals(List.of(
                "vulkanmod.overview.profile",
                "vulkanmod.overview.gpu",
                "vulkanmod.overview.resolution",
                "vulkanmod.overview.refresh_rate",
                "vulkanmod.overview.internal_resolution",
                "vulkanmod.overview.effective_resolution",
                "vulkanmod.overview.upscaler",
                "vulkanmod.overview.vsync",
                "vulkanmod.overview.render_distance",
                "vulkanmod.overview.occlusion_culling",
                "vulkanmod.overview.shader_pack",
                "vulkanmod.overview.gpu_frame_time"), labelKeys());
    }

    @Test
    void everyOverviewLabelKeyResolvesInEnUs() throws IOException {
        Map<String, String> lang = readLang();
        List<String> keys = labelKeys();
        assertFalse(keys.isEmpty(), "no overview label key found in " + CATALOG.toAbsolutePath());

        List<String> unresolved = new ArrayList<>();
        for (String key : keys) {
            String value = lang.get(key);
            if (value == null || value.isBlank()) {
                unresolved.add(key);
            }
        }
        assertEquals(List.of(), unresolved);
    }

    private static List<String> labelKeys() throws IOException {
        assertTrue(Files.isRegularFile(CATALOG), "catalogue missing at " + CATALOG.toAbsolutePath());
        List<String> keys = new ArrayList<>();
        for (String line : Files.readAllLines(CATALOG)) {
            Matcher matcher = LABEL_KEY.matcher(line);
            while (matcher.find()) {
                keys.add(matcher.group(1));
            }
        }
        return keys;
    }

    private static Map<String, String> readLang() throws IOException {
        assertTrue(Files.isRegularFile(LANG), "lang file missing at " + LANG.toAbsolutePath());
        Map<String, String> entries = new LinkedHashMap<>();
        for (String line : Files.readAllLines(LANG)) {
            Matcher matcher = ENTRY.matcher(line);
            if (matcher.matches()) {
                entries.put(matcher.group(1), matcher.group(2));
            }
        }
        assertFalse(entries.isEmpty(), "no entries parsed from " + LANG.toAbsolutePath());
        return entries;
    }
}
