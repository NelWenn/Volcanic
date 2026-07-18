package net.vulkanmod.render.pack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PackShaderCompiler {
    private static final Map<String, GraphicsPipeline> CACHE = new HashMap<>();

    public static GraphicsPipeline get(String packId, String program) {
        String key = packId + "/" + program;
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        GraphicsPipeline pipeline = compile(packId, program);
        CACHE.put(key, pipeline);
        return pipeline;
    }

    private static GraphicsPipeline compile(String packId, String program) {
        Path dir = ShaderPacks.dir().resolve(packId);
        Path vsh = dir.resolve(program + ".vsh");
        Path fsh = dir.resolve(program + ".fsh");
        Path json = dir.resolve(program + ".json");
        if (!Files.exists(vsh) || !Files.exists(fsh) || !Files.exists(json)) {
            return null;
        }
        try {
            String vshSrc = Files.readString(vsh, StandardCharsets.UTF_8);
            String fshSrc = Files.readString(fsh, StandardCharsets.UTF_8);
            JsonObject bindings;
            try (Reader r = Files.newBufferedReader(json, StandardCharsets.UTF_8)) {
                bindings = JsonParser.parseReader(r).getAsJsonObject();
            }
            Pipeline.Builder b = new Pipeline.Builder(CustomVertexFormat.NONE);
            b.compileShaders(program, vshSrc, fshSrc);
            b.parseBindings(bindings);
            GraphicsPipeline pipeline = b.createGraphicsPipeline();
            Initializer.LOGGER.info("Compiled pack shader {}/{} from disk", packId, program);
            return pipeline;
        } catch (Exception e) {
            Initializer.LOGGER.error("Failed to compile pack shader {}/{} from disk", packId, program, e);
            return null;
        }
    }
}
