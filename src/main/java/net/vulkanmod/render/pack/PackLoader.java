package net.vulkanmod.render.pack;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.vulkanmod.Initializer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PackLoader {

    public static ShaderPack load(String id) {
        ShaderPacks.ensureBuiltins();
        try {
            Path external = ShaderPacks.file(id, "pack.json");
            if (external != null && Files.exists(external)) {
                try (Reader r = Files.newBufferedReader(external, StandardCharsets.UTF_8)) {
                    ShaderPack pack = parse(id, JsonParser.parseReader(r).getAsJsonObject());
                    Initializer.LOGGER.info("Loaded pack '{}' from shaderpacks/", id);
                    return pack;
                }
            }
            String path = "/assets/vulkanmod/shaders/packs/" + id + "/pack.json";
            try (InputStream in = PackLoader.class.getResourceAsStream(path)) {
                if (in == null) {
                    return null;
                }
                return parse(id, JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
            }
        } catch (Exception ex) {
            Initializer.LOGGER.error("Failed to load shader pack '{}'", id, ex);
            return null;
        }
    }

    private static ShaderPack parse(String id, JsonObject root) {
        String name = root.has("name") ? root.get("name").getAsString() : id;

        List<PackTarget> targets = new ArrayList<>();
        if (root.has("targets")) {
            for (JsonElement e : root.getAsJsonArray("targets")) {
                JsonObject t = e.getAsJsonObject();
                targets.add(new PackTarget(
                        t.get("name").getAsString(),
                        t.has("format") ? t.get("format").getAsString() : "RGBA16F",
                        t.has("scale") ? t.get("scale").getAsFloat() : 1.0f,
                        t.has("pingpong") && t.get("pingpong").getAsBoolean(),
                        t.has("clear") ? t.get("clear").getAsFloat() : 0.0f));
            }
        }

        List<PackPass> passes = new ArrayList<>();
        if (root.has("passes")) {
            for (JsonElement e : root.getAsJsonArray("passes")) {
                JsonObject p = e.getAsJsonObject();
                Map<Integer, String> inputs = new LinkedHashMap<>();
                if (p.has("inputs")) {
                    for (Map.Entry<String, JsonElement> entry : p.getAsJsonObject("inputs").entrySet()) {
                        inputs.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsString());
                    }
                }
                passes.add(new PackPass(
                        p.get("name").getAsString(),
                        p.get("program").getAsString(),
                        inputs,
                        p.has("output") ? p.get("output").getAsString() : "swapchain"));
            }
        }
        return new ShaderPack(id, name, targets, passes);
    }
}
