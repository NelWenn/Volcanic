package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CorePurityTest {
    private static final List<String> FORBIDDEN =
            List.of("net.minecraft", "com.mojang", "net.neoforged", "net.vulkanmod.vulkan", "org.lwjgl");

    @Test
    void coreHasNoRenderingOrGameImports() throws IOException {
        Path root = Path.of("src/main/java/net/vulkanmod/config/ui/core");
        assertTrue(Files.isDirectory(root), "core package missing at " + root.toAbsolutePath());

        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                for (String line : Files.readAllLines(file)) {
                    for (String forbidden : FORBIDDEN) {
                        if (line.contains(forbidden)) {
                            violations.add(file.getFileName() + " -> " + line.trim());
                        }
                    }
                }
            }
        }
        assertEquals(List.of(), violations);
    }
}
