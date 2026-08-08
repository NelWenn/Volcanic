package net.vulkanmod.config.ui.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.vulkanmod.Initializer;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;

import java.util.concurrent.atomic.AtomicInteger;

public final class GuiSurfacePipeline {
    public static final String SHADER_NAME = "gui_surface";
    private static final String VULKAN_BIND_PATH = "vulkanmod/core/gui_surface/gui_surface";
    private static final int MAX_DRAW_FAILURES = 3;

    private static final VertexFormat FORMAT = createFormat();
    private static final AtomicInteger DRAW_FAILURES = new AtomicInteger();

    private static volatile ShaderInstance shader;
    private static volatile boolean disabled;

    private GuiSurfacePipeline() {
    }

    public static void register(RegisterShadersEvent event) {
        shader = null;

        if (FORMAT == null) {
            return;
        }

        if (disabled) {
            if (DRAW_FAILURES.get() >= MAX_DRAW_FAILURES) {
                return;
            }
            disabled = false;
            Initializer.LOGGER.info("Re-enabling the {} shader for this resource reload", SHADER_NAME);
        }

        try {
            ShaderInstance instance = new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(Initializer.MOD_ID, SHADER_NAME), FORMAT);
            event.registerShader(instance, GuiSurfacePipeline::onLoaded);
        } catch (Throwable throwable) {
            Initializer.LOGGER.error("Failed to register the {} shader; the options menu falls back to flat surfaces",
                    SHADER_NAME, throwable);
        }
    }

    public static boolean isAvailable() {
        return !disabled && FORMAT != null && shader != null;
    }

    public static ShaderInstance shader() {
        return shader;
    }

    public static VertexFormat format() {
        return FORMAT;
    }

    static void markUnavailable(String reason, Throwable throwable) {
        if (disabled) {
            return;
        }
        disabled = true;
        shader = null;

        if (DRAW_FAILURES.incrementAndGet() >= MAX_DRAW_FAILURES) {
            Initializer.LOGGER.error("Disabling the {} shader for the rest of this session after {} failures: {}",
                    SHADER_NAME, MAX_DRAW_FAILURES, reason, throwable);
            return;
        }

        Initializer.LOGGER.error("Disabling the {} shader until the next resource reload: {}",
                SHADER_NAME, reason, throwable);
    }

    private static void onLoaded(ShaderInstance instance) {
        try {
            GraphicsPipeline pipeline = ((ShaderMixed) instance).getPipeline();

            if (pipeline == null || !VULKAN_BIND_PATH.equals(pipeline.name)) {
                Initializer.LOGGER.warn("Shader {} did not resolve through the Vulkan pipeline at {} (got {});"
                                + " the options menu falls back to flat surfaces",
                        SHADER_NAME, VULKAN_BIND_PATH, pipeline == null ? "no pipeline" : pipeline.name);
                return;
            }

            shader = instance;
            Initializer.LOGGER.info("Shader {} resolved through the Vulkan pipeline at {}", SHADER_NAME, VULKAN_BIND_PATH);
        } catch (Throwable throwable) {
            Initializer.LOGGER.error("Failed to inspect the {} shader pipeline", SHADER_NAME, throwable);
        }
    }

    private static VertexFormat createFormat() {
        try {
            return VertexFormat.builder()
                    .add("Position", VertexFormatElement.POSITION)
                    .add("Color", VertexFormatElement.COLOR)
                    .add("Local", VertexFormatElement.UV0)
                    .add("Extent", VertexFormatElement.UV1)
                    .add("Radii", VertexFormatElement.UV2)
                    .add("Layer", VertexFormatElement.NORMAL)
                    .padding(1)
                    .build();
        } catch (Throwable throwable) {
            Initializer.LOGGER.error("Failed to build the {} vertex format", SHADER_NAME, throwable);
            return null;
        }
    }
}
