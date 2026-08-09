package net.vulkanmod.config.ui.settings;

import net.vulkanmod.config.ui.core.BoundVerdict;
import net.vulkanmod.config.ui.core.Recommendation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OverviewKeysTest {
    private static final Path LANG = Path.of("src/main/resources/assets/vulkanmod/lang/en_us.json");

    @Test
    void everyBoundVerdictHasASentence() throws IOException {
        List<String> lang = Files.readAllLines(LANG);
        List<String> missing = new ArrayList<>();
        for (BoundVerdict verdict : BoundVerdict.values()) {
            if (!declares(lang, verdict.messageKey())) {
                missing.add(verdict.messageKey());
            }
        }
        assertEquals(List.of(), missing);
    }

    @Test
    void everyRecommendationKeyResolves() throws IOException {
        List<String> lang = Files.readAllLines(LANG);
        List<String> missing = new ArrayList<>();
        for (String key : Recommendation.keys()) {
            if (!declares(lang, key)) {
                missing.add(key);
            }
        }
        assertEquals(List.of(), missing);
    }

    @Test
    void everyPresetHasANameAndACardBlurb() throws IOException {
        List<String> lang = Files.readAllLines(LANG);
        List<String> missing = new ArrayList<>();
        for (String preset : List.of("performance", "balanced", "quality", "ultra", "custom")) {
            String key = "vulkanmod.options.performancePreset." + preset;
            if (!declares(lang, key)) {
                missing.add(key);
            }
            if (!declares(lang, key + ".card")) {
                missing.add(key + ".card");
            }
        }
        assertEquals(List.of(), missing);
    }

    private static boolean declares(List<String> lang, String key) {
        return lang.stream().anyMatch(line -> line.trim().startsWith("\"" + key + "\":"));
    }
}
