package net.vulkanmod.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ConfigSerializationTest {
    private static final Path CONFIG = Path.of("src/main/java/net/vulkanmod/config/Config.java");
    private static final Pattern EXCLUSION =
            Pattern.compile("excludeFieldsWithModifiers\\(([^)]*)\\)");
    private static final Pattern PUBLIC_STATIC_FIELD =
            Pattern.compile("^\\s*public static (?!final class)[\\w.<>\\[\\]]+ \\w+\\s*[=;]");

    @Test
    void gsonSkipsStaticFieldsOrOneConstantTakesTheWholeConfigFileDownWithIt() throws IOException {
        Matcher matcher = EXCLUSION.matcher(source());
        assertTrue(matcher.find(), "Config no longer configures a field exclusion policy at all");

        String modifiers = matcher.group(1);
        assertTrue(modifiers.contains("Modifier.STATIC"),
                "Gson's own default exclusion is STATIC | TRANSIENT, and naming any modifier"
                        + " REPLACES that default rather than adding to it. Without Modifier.STATIC"
                        + " here, a single public static constant in Config makes Gson throw"
                        + " 'Cannot set value of static final field' on load, which sets the"
                        + " player's whole settings file aside and starts from defaults."
                        + " Found: " + modifiers);
        assertTrue(modifiers.contains("Modifier.TRANSIENT"),
                "restoring STATIC without TRANSIENT still drops half of Gson's default. Found: "
                        + modifiers);
    }

    @Test
    void everyPublicStaticFieldInConfigIsAConstantSoItCanNeverBeWrittenBack() throws IOException {
        List<String> offenders = source().lines()
                .map(String::stripTrailing)
                .filter(line -> PUBLIC_STATIC_FIELD.matcher(line).find())
                .filter(line -> !line.contains(" final "))
                .toList();
        if (!offenders.isEmpty()) {
            fail("a mutable public static field in Config is state that no config file owns: "
                    + offenders);
        }
    }

    private static String source() throws IOException {
        assertTrue(Files.isRegularFile(CONFIG), "config missing at " + CONFIG.toAbsolutePath());
        return Files.readString(CONFIG);
    }
}
