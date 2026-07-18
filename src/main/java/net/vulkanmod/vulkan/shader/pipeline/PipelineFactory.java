package net.vulkanmod.vulkan.shader.pipeline;

import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.vulkanmod.vulkan.shader.SamplerTextureSlot;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.shader.layout.AlignedStruct;
import net.vulkanmod.vulkan.shader.layout.PushConstants;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

import static net.vulkanmod.vulkan.shader.SPIRVUtils.ShaderKind.FRAGMENT_SHADER;
import static net.vulkanmod.vulkan.shader.SPIRVUtils.ShaderKind.VERTEX_SHADER;
import static net.vulkanmod.vulkan.shader.SPIRVUtils.compileShaderAbsoluteFile;

/**
 * Reflects over a {@link PipelineDefinition} class annotations to build a {@link GraphicsPipeline},
 */
public final class PipelineFactory {
    private static final String SHADER_ROOT = "/assets/vulkanmod/shaders/";

    private PipelineFactory() {
    }

    public static GraphicsPipeline build(Class<? extends PipelineDefinition> definition) {
        GfxPipeline meta = definition.getAnnotation(GfxPipeline.class);
        if (meta == null)
            throw new IllegalStateException(definition.getName() + " has no @GfxPipeline annotation");

        Pipeline.Builder builder = new Pipeline.Builder(meta.vertexFormat().resolve(), meta.basePath());
        builder.setUniforms(collectUbos(definition), collectSamplers(definition));
        builder.setPushConstants(collectPushConstants(definition));

        String vertPath = String.format("basic/%s/%s", meta.basePath(), meta.vertex());
        String fragPath = String.format("basic/%s/%s", meta.basePath(), meta.fragment());
        SPIRVUtils.SPIRV vertSpirv = compileShaderAbsoluteFile(SHADER_ROOT + vertPath + ".vsh", VERTEX_SHADER);
        SPIRVUtils.SPIRV fragSpirv = compileShaderAbsoluteFile(SHADER_ROOT + fragPath + ".fsh", FRAGMENT_SHADER);
        builder.setSPIRVs(vertSpirv, fragSpirv);

        return builder.createGraphicsPipeline();
    }

    private static List<UBO> collectUbos(Class<?> definition) {
        List<UBO> ubos = new ArrayList<>();
        for (Class<?> nested : nestedClassesOf(definition)) {
            Ubo ubo = nested.getAnnotation(Ubo.class);
            if (ubo == null)
                continue;

            AlignedStruct.Builder structBuilder = new AlignedStruct.Builder();
            addUniformFields(structBuilder, nested);
            ubos.add(structBuilder.buildUBO(ubo.binding(), ubo.stage().bits()));
        }
        // vkCmdBindDescriptorSets' pDynamicOffsets match to dynamic UBOs in ascending binding
        ubos.sort(Comparator.comparingInt(UBO::getBinding));
        return ubos;
    }

    private static List<ImageDescriptor> collectSamplers(Class<?> definition) {
        List<ImageDescriptor> samplers = new ArrayList<>();
        for (Field field : fieldsOf(definition)) {
            Sampler sampler = field.getAnnotation(Sampler.class);
            if (sampler == null)
                continue;

            String name = field.getName();
            samplers.add(new ImageDescriptor(sampler.binding(), "sampler2D", name, SamplerTextureSlot.getTextureIdx(name)));
        }
        return samplers;
    }

    private static PushConstants collectPushConstants(Class<?> definition) {
        for (Class<?> nested : nestedClassesOf(definition)) {
            if (!nested.isAnnotationPresent(PushConstantBlock.class))
                continue;

            AlignedStruct.Builder structBuilder = new AlignedStruct.Builder();
            addUniformFields(structBuilder, nested);
            return structBuilder.buildPushConstant();
        }
        return null;
    }

    // std140 offsets accumulate in field declaration order, so it must mirror the GLSL struct exactly (Or Konssékensses).
    private static void addUniformFields(AlignedStruct.Builder builder, Class<?> struct) {
        for (Field field : fieldsOf(struct)) {
            if (field.getType() == float[].class) {
                Uniform uniform = field.getAnnotation(Uniform.class);
                if (uniform == null || uniform.count() <= 0)
                    throw new IllegalStateException("float[] field " + field + " requires @Uniform(count = N)");
                builder.addUniformInfo("float", field.getName(), uniform.count());
            } else {
                builder.addUniformInfo(uniformType(field), field.getName());
            }
        }
    }

    private static String uniformType(Field field) {
        Class<?> type = field.getType();
        if (type == float.class) return "float";
        if (type == int.class) return "int";
        if (type == Matrix4f.class) return "mat4";
        if (type == Matrix3f.class) return "mat3";
        if (type == Vector4f.class) return "vec4";
        if (type == Vector3f.class) return "vec3";
        if (type == Vector2f.class) return "vec2";
        throw new IllegalStateException("Unsupported uniform field type: " + type + " (" + field + ")");
    }

    private static List<Field> fieldsOf(Class<?> leaf) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> clazz : hierarchyOf(leaf))
            for (Field field : clazz.getDeclaredFields())
                if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers()))
                    fields.add(field);
        return fields;
    }

    private static List<Class<?>> nestedClassesOf(Class<?> leaf) {
        List<Class<?>> nested = new ArrayList<>();
        for (Class<?> clazz : hierarchyOf(leaf))
            nested.addAll(List.of(clazz.getDeclaredClasses()));
        return nested;
    }

    // root sup first
    private static List<Class<?>> hierarchyOf(Class<?> leaf) {
        Deque<Class<?>> chain = new ArrayDeque<>();
        for (Class<?> cl = leaf;
             cl != null && cl != Object.class;
             cl = cl.getSuperclass())
            
            chain.push(cl);
        return new ArrayList<>(chain);
    }
}
