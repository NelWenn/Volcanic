package net.vulkanmod.render.sodium;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineFactory;
import net.vulkanmod.vulkan.shader.pipeline.definitions.TerrainPipeline;
import net.vulkanmod.vulkan.shader.pipeline.definitions.core.CloudsPipeline;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SodiumTerrainCompiler {

    private static final String SHIM_VSH_PATH = "/assets/vulkanmod/shaders/sodium/block_shim.vsh";
    private static final String SHIM_INCLUDE_ROOT = "/assets/vulkanmod/shaders/sodium/";
    private static final String CLOUDS_VSH_PATH = "/assets/vulkanmod/shaders/minecraft/core/rendertype_clouds/rendertype_clouds.vsh";
    private static final int MAX_IMPORT_DEPTH = 16;

    private static String shimVsh;
    private static String cloudsVsh;

    private SodiumTerrainCompiler() {
    }

    public static GraphicsPipeline compileClouds(String packFragment, VertexFormat format) {
        String fragmentSource = translateClouds(packFragment);
        return PipelineFactory.buildFromSources(CloudsPipeline.class, format, "sodium_clouds", cloudsVsh(), fragmentSource);
    }

    private static final boolean DEBUG_CLOUDS = false;

    private static String translateClouds(String packFragment) {
        if (DEBUG_CLOUDS) {
            return """
                    #version 450
                    #include "fog.glsl"
                    layout(binding = 1) uniform DbgUbo {
                        vec4 ColorModulator;
                        vec4 FogColor;
                        float FogStart;
                        float FogEnd;
                    };
                    layout(location = 0) in vec4 vertexColor;
                    layout(location = 1) in vec2 texCoord0;
                    layout(location = 2) in float vertexDistance;
                    layout(location = 0) out vec4 fragColor;
                    void main() {
                        vec4 color = vertexColor * ColorModulator;
                        float width = FogEnd - FogStart;
                        float fade = linear_fog_fade(vertexDistance, FogStart, FogStart + width * 4.0) * FogColor.a;
                        if (color.a < 0.1) {
                            fragColor = vec4(1.0, 0.0, 0.0, 1.0);
                        } else if (fade < 0.05) {
                            fragColor = vec4(0.0, 0.0, 1.0, 1.0);
                        } else {
                            fragColor = vec4(0.0, 1.0, 0.0, 1.0);
                        }
                    }
                    """;
        }

        StringBuilder body = new StringBuilder();
        for (String line : packFragment.split("\n", -1)) {
            String code = stripLineComment(line).strip();
            if (code.startsWith("#version") || code.startsWith("#moj_import") || code.startsWith("#import")) {
                continue;
            }
            if (isTopLevelDecl(code, "in ") || isTopLevelDecl(code, "out ") || isTopLevelDecl(code, "uniform ")) {
                continue;
            }
            body.append(line).append("\n");
        }
        return CLOUDS_HEADER + body;
    }

    private static final String CLOUDS_HEADER = """
            #version 450
            #include "fog.glsl"

            layout(binding = 2) uniform sampler2D Sampler0;

            layout(binding = 1) uniform SodiumCloudsUbo {
                vec4 ColorModulator;
                vec4 FogColor;
                float FogStart;
                float FogEnd;
            };

            layout(location = 0) in vec4 v_cloudColor;
            layout(location = 1) in vec2 texCoord0;
            layout(location = 2) in float vertexDistance;

            layout(location = 0) out vec4 fragColor;

            #define vertexColor (texture(Sampler0, texCoord0) * v_cloudColor)

            """;

    public static GraphicsPipeline compile(TerrainRenderType renderType, String packFragment, ResourceProvider resourceProvider) {
        List<String> macros = macrosFor(renderType);

        String vertexSource = injectDefines(shimVsh(), macros);
        String fragmentSource = translateFragment(packFragment, macros, resourceProvider);

        String name = "sodium_block_" + renderType.name().toLowerCase();
        return PipelineFactory.buildFromSources(TerrainPipeline.class, CustomVertexFormat.COMPRESSED_TERRAIN, name, vertexSource, fragmentSource);
    }

    private static List<String> macrosFor(TerrainRenderType renderType) {
        String renderPass = switch (renderType) {
            case SOLID -> "RENDER_PASS_SOLID";
            case TRANSLUCENT -> "RENDER_PASS_TRANSLUCENT";
            default -> "RENDER_PASS_CUTOUT";
        };
        boolean discard = renderType != TerrainRenderType.SOLID && renderType != TerrainRenderType.TRANSLUCENT;
        float mipBias = renderType == TerrainRenderType.CUTOUT ? -4.0f : 0.0f;
        float alphaCutoff = renderType.alphaCutout;

        List<String> macros = new ArrayList<>();
        macros.add("#define " + renderPass);
        macros.add("#define USE_FOG");
        macros.add("#define USE_VERTEX_COMPRESSION");
        macros.add("#define SODIUM_CORE_SHADER_SUPPORT");
        if (discard) {
            macros.add("#define USE_FRAGMENT_DISCARD");
        }
        macros.add("#define SODIUM_MIP_BIAS " + glslFloat(mipBias));
        macros.add("#define SODIUM_ALPHA_CUTOFF " + glslFloat(alphaCutoff));
        return macros;
    }

    private static String translateFragment(String packFragment, List<String> macros, ResourceProvider resourceProvider) {
        String inlined = inlineImports(packFragment, resourceProvider, 0);

        StringBuilder body = new StringBuilder();
        for (String line : inlined.split("\n", -1)) {
            String code = stripLineComment(line).strip();
            if (code.startsWith("#version")) {
                continue;
            }
            if (isTopLevelDecl(code, "in ") || isTopLevelDecl(code, "out ") || isTopLevelDecl(code, "uniform ")) {
                continue;
            }
            body.append(line).append("\n");
        }

        String renamedBody = body.toString().replaceFirst("\\bvoid\\s+main\\s*\\(\\s*(?:void)?\\s*\\)", "void sodium_main()");

        StringBuilder source = new StringBuilder();
        source.append("#version 450\n");
        for (String macro : macros) {
            source.append(macro).append("\n");
        }
        source.append(FRAGMENT_HEADER);
        source.append(renamedBody);
        source.append(MAIN_WRAPPER);
        return source.toString();
    }

    private static final String FRAGMENT_HEADER = """
            layout(binding = 2) uniform sampler2D Sampler0;

            layout(binding = 1) uniform SodiumFragUbo {
                vec4 FogColor;
                float FogStart;
                float FogEnd;
                float AlphaCutout;
                float PbrDebug;
                float CamilleActive;
                float GameTime;
                float SunAngle;
                vec3 FogSunDir;
            };

            #define u_BlockTex Sampler0
            #define u_FogColor FogColor
            #define u_FogStart FogStart
            #define u_FogEnd FogEnd
            #define u_GameTime GameTime
            #define u_SunAngle SunAngle

            layout(location = 0) in vec4 v_Color;
            layout(location = 1) in vec2 v_TexCoord;
            layout(location = 2) in float v_FragDistance;
            layout(location = 3) in float v_MaterialMipBias;
            layout(location = 4) in float v_MaterialAlphaCutoff;
            layout(location = 5) in vec3 worldPos;

            layout(location = 0) out vec4 fragColor;
            layout(location = 1) out vec4 outNormal;

            """;

    private static final String MAIN_WRAPPER = """

            void main() {
                sodium_main();
                vec3 dp1 = dFdx(worldPos);
                vec3 dp2 = dFdy(worldPos);
                vec3 geoN = normalize(cross(dp1, dp2));
                if (dot(geoN, worldPos) > 0.0) {
                    geoN = -geoN;
                }
                outNormal = vec4(geoN, 1.0);
            }
            """;

    private static boolean isTopLevelDecl(String code, String keyword) {
        return code.startsWith(keyword) && code.endsWith(";") && !code.contains("(");
    }

    private static String stripLineComment(String line) {
        int idx = line.indexOf("//");
        return idx >= 0 ? line.substring(0, idx) : line;
    }

    private static String inlineImports(String source, ResourceProvider resourceProvider, int depth) {
        if (depth > MAX_IMPORT_DEPTH) {
            throw new IllegalStateException("Sodium shader import depth exceeded");
        }

        StringBuilder out = new StringBuilder();
        for (String line : source.split("\n", -1)) {
            String ref = parseImport(line.strip());
            if (ref != null) {
                out.append(inlineImports(resolveInclude(ref, resourceProvider), resourceProvider, depth + 1)).append("\n");
            } else {
                out.append(line).append("\n");
            }
        }
        return out.toString();
    }

    private static String parseImport(String trimmed) {
        if (!trimmed.startsWith("#import") && !trimmed.startsWith("#moj_import")) {
            return null;
        }
        int open = trimmed.indexOf('<');
        int close = trimmed.lastIndexOf('>');
        if (open >= 0 && close > open) {
            return trimmed.substring(open + 1, close).strip();
        }
        int firstQuote = trimmed.indexOf('"');
        int lastQuote = trimmed.lastIndexOf('"');
        if (firstQuote >= 0 && lastQuote > firstQuote) {
            return trimmed.substring(firstQuote + 1, lastQuote).strip();
        }
        return null;
    }

    private static String resolveInclude(String ref, ResourceProvider resourceProvider) {
        int separator = ref.indexOf(':');
        String namespace = separator > 0 ? ref.substring(0, separator) : "sodium";
        String path = separator > 0 ? ref.substring(separator + 1) : ref;

        Optional<Resource> resource = resourceProvider.getResource(ResourceLocation.fromNamespaceAndPath(namespace, "shaders/" + path));
        if (resource.isPresent()) {
            return readResource(resource.get());
        }

        try (InputStream stream = SodiumTerrainCompiler.class.getResourceAsStream(SHIM_INCLUDE_ROOT + path)) {
            if (stream != null) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read Sodium include shim: " + ref, e);
        }

        throw new IllegalStateException("Unresolved Sodium shader include: " + ref);
    }

    private static String injectDefines(String source, List<String> macros) {
        StringBuilder out = new StringBuilder();
        boolean injected = false;
        for (String line : source.split("\n", -1)) {
            out.append(line).append("\n");
            if (!injected && line.strip().startsWith("#version")) {
                for (String macro : macros) {
                    out.append(macro).append("\n");
                }
                injected = true;
            }
        }
        if (!injected) {
            StringBuilder prefix = new StringBuilder();
            for (String macro : macros) {
                prefix.append(macro).append("\n");
            }
            return prefix.append(out).toString();
        }
        return out.toString();
    }

    private static String shimVsh() {
        if (shimVsh == null) {
            shimVsh = readClasspath(SHIM_VSH_PATH);
        }
        return shimVsh;
    }

    private static String cloudsVsh() {
        if (cloudsVsh == null) {
            cloudsVsh = readClasspath(CLOUDS_VSH_PATH);
        }
        return cloudsVsh;
    }

    private static String readClasspath(String path) {
        try (InputStream stream = SodiumTerrainCompiler.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing classpath shader resource: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read classpath shader resource: " + path, e);
        }
    }

    private static String readResource(Resource resource) {
        try (InputStream stream = resource.open()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read Sodium shader resource", e);
        }
    }

    private static String glslFloat(float value) {
        if (value == Math.rint(value)) {
            return String.format(java.util.Locale.ROOT, "%.1f", value);
        }
        return String.format(java.util.Locale.ROOT, "%s", value);
    }
}
