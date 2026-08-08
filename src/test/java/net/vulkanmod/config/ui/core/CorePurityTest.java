package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CorePurityTest {
    private static final List<String> FORBIDDEN =
            List.of("net.minecraft", "com.mojang", "net.neoforged", "net.vulkanmod.vulkan", "org.lwjgl",
                    "net.vulkanmod.config.ui.render", "net.vulkanmod.config.ui.shell");

    private static final List<String> PROBES = List.of(
            "net.minecraft.client.gui.GuiGraphics",
            "com.mojang.blaze3d.systems.RenderSystem",
            "net.neoforged.fml.ModList",
            "net.vulkanmod.vulkan.Renderer",
            "org.lwjgl.glfw.GLFW",
            "net.vulkanmod.config.ui.render.SurfacePainter",
            "net.vulkanmod.config.ui.shell.NavPresenter");

    @Test
    void coreHasNoRenderingOrGameImports() throws IOException {
        Path root = Path.of("src/main/java/net/vulkanmod/config/ui/core");
        assertTrue(Files.isDirectory(root), "core package missing at " + root.toAbsolutePath());
        assertEquals(List.of(), scan(root));
    }

    @Test
    void everyProbeImportIsReportedAsAViolation(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("Probe.java");
        for (String probe : PROBES) {
            Files.writeString(file, "package net.vulkanmod.config.ui.core;\n\nimport " + probe + ";\n");
            assertEquals(List.of("Probe.java -> import " + probe + ";"), scan(directory), probe);
        }
    }

    @Test
    void aFileImportingOnlyJavaUtilIsClean(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve("Clean.java"),
                "package net.vulkanmod.config.ui.core;\n\nimport java.util.List;\nimport java.util.Map;\n");
        assertEquals(List.of(), scan(directory));
    }

    private static List<String> scan(Path root) throws IOException {
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
        return violations;
    }
}
