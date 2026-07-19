package net.vulkanmod.render.pack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;
import net.vulkanmod.Initializer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class ShaderPacks {
    private static final Map<String, String[]> BUILTINS = Map.of();

    private static boolean ensured = false;

    public static Path dir() {
        Path d = FMLPaths.GAMEDIR.get().resolve("shaderpacks");
        try {
            Files.createDirectories(d);
        } catch (Exception e) {
            Initializer.LOGGER.error("Could not create shaderpacks dir", e);
        }
        return d;
    }

    public static Path file(String id, String name) {
        return dir().resolve(id).resolve(name);
    }

    public static void ensureBuiltins() {
        if (ensured) {
            return;
        }
        ensured = true;
        Path base = dir();
        for (Map.Entry<String, String[]> entry : BUILTINS.entrySet()) {
            String id = entry.getKey();
            int bundledVersion = bundledPackVersion(id);
            int diskVersion = diskPackVersion(id);
            boolean refresh = diskVersion < bundledVersion;
            if (refresh) {
                if (diskVersion > 0) {
                    Initializer.LOGGER.info("Updating built-in pack '{}' v{} -> v{}", id, diskVersion, bundledVersion);
                }
                backupPack(base, id);
            }
            for (String f : entry.getValue()) {
                Path dst = base.resolve(id).resolve(f);
                if (!refresh && Files.exists(dst)) {
                    continue;
                }
                try (InputStream in = ShaderPacks.class.getResourceAsStream(
                        "/assets/vulkanmod/shaders/packs/" + id + "/" + f)) {
                    if (in == null) {
                        continue;
                    }
                    Files.createDirectories(dst.getParent());
                    Files.copy(in, dst, StandardCopyOption.REPLACE_EXISTING);
                    Initializer.LOGGER.info("Exported built-in pack file {}/{}", id, f);
                } catch (Exception e) {
                    Initializer.LOGGER.error("Failed to export pack file {}/{}", id, f, e);
                }
            }
        }
    }

    private static void backupPack(Path base, String id) {
        Path src = base.resolve(id);
        if (!Files.isDirectory(src)) {
            return;
        }
        Path bak = base.resolve(id + ".bak");
        try {
            if (Files.exists(bak)) {
                try (var walk = Files.walk(bak)) {
                    walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
                }
            }
            Files.move(src, bak);
            Initializer.LOGGER.info("Backed up pack '{}' to shaderpacks/{}", id, bak.getFileName());
        } catch (Exception e) {
            Initializer.LOGGER.error("Could not back up pack '{}'", id, e);
        }
    }

    private static int bundledPackVersion(String id) {
        try (InputStream in = ShaderPacks.class.getResourceAsStream(
                "/assets/vulkanmod/shaders/packs/" + id + "/pack.json")) {
            if (in == null) {
                return 0;
            }
            return packVersion(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (Exception e) {
            return 0;
        }
    }

    private static int diskPackVersion(String id) {
        Path json = file(id, "pack.json");
        if (json == null || !Files.exists(json)) {
            return 0;
        }
        try (Reader r = Files.newBufferedReader(json, StandardCharsets.UTF_8)) {
            return packVersion(JsonParser.parseReader(r).getAsJsonObject());
        } catch (Exception e) {
            return 0;
        }
    }

    private static int packVersion(JsonObject root) {
        return root.has("version") ? root.get("version").getAsInt() : 1;
    }
}
