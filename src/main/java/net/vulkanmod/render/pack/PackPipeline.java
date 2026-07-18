package net.vulkanmod.render.pack;

import net.vulkanmod.Initializer;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.DrawUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkClearColorValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkImageSubresourceRange;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.vulkan.VK10.*;

public class PackPipeline {
    private static final Map<String, ShaderPack> CACHE = new HashMap<>();
    private static final Map<String, Map<String, Target>> TARGETS = new HashMap<>();

    public interface ResourceResolver {
        VulkanImage resolve(String name);
    }

    private static class Target {
        final PackTarget spec;
        final Framebuffer[] framebuffers = new Framebuffer[2];
        final RenderPass[] renderPasses = new RenderPass[2];
        int index;
        int w = -1, h = -1;

        Target(PackTarget spec) {
            this.spec = spec;
        }

        int buffers() {
            return this.spec.pingpong ? 2 : 1;
        }
    }

    public static ShaderPack get(String id) {
        if (CACHE.containsKey(id)) {
            return CACHE.get(id);
        }
        ShaderPack pack = PackLoader.load(id);
        CACHE.put(id, pack);
        if (pack != null) {
            Initializer.LOGGER.info("Loaded shader pack '{}' ({} passes, {} targets)", id, pack.passes.size(), pack.targets.size());
        }
        return pack;
    }

    private static GraphicsPipeline pipeline(ShaderPack pack, PackPass pass) {
        GraphicsPipeline pipeline = PackShaderCompiler.get(pack.id, pass.program);
        return pipeline != null ? pipeline : PipelineManager.getPostShaderPipeline(pass.program);
    }

    public static boolean pipelinesReady(ShaderPack pack) {
        for (PackPass pass : pack.passes) {
            if (pipeline(pack, pass) == null) {
                return false;
            }
        }
        return true;
    }

    private static final Set<String> invalidWarned = new HashSet<>();

    public static boolean structureValid(ShaderPack pack) {
        boolean valid = !pack.passes.isEmpty();
        if (valid) {
            Set<String> declared = new HashSet<>();
            for (PackTarget target : pack.targets) {
                declared.add(target.name);
            }
            for (int i = 0; i < pack.passes.size(); i++) {
                PackPass pass = pack.passes.get(i);
                boolean last = i == pack.passes.size() - 1;
                if ("swapchain".equals(pass.output) ? !last : !declared.contains(pass.output)) {
                    valid = false;
                    break;
                }
            }
            if (valid && !"swapchain".equals(pack.passes.get(pack.passes.size() - 1).output)) {
                valid = false;
            }
        }
        if (!valid && invalidWarned.add(pack.id)) {
            Initializer.LOGGER.error("Shader pack '{}' has an invalid pass layout (need declared outputs and a single final swapchain pass), falling back", pack.id);
        }
        return valid;
    }

    public static void ensureTargets(ShaderPack pack, VkCommandBuffer commandBuffer, MemoryStack stack, int mainWidth, int mainHeight) {
        if (mainWidth <= 0 || mainHeight <= 0) {
            return;
        }
        Map<String, Target> targets = TARGETS.computeIfAbsent(pack.id, k -> {
            Map<String, Target> m = new LinkedHashMap<>();
            for (PackTarget t : pack.targets) {
                m.put(t.name, new Target(t));
            }
            return m;
        });
        for (Target target : targets.values()) {
            int w = Math.max(1, (int) (mainWidth * target.spec.scale));
            int h = Math.max(1, (int) (mainHeight * target.spec.scale));
            if (target.framebuffers[0] != null && target.w == w && target.h == h) {
                continue;
            }
            dispose(target);

            for (int i = 0; i < target.buffers(); i++) {
                VulkanImage image = VulkanImage.builder(w, h)
                        .setFormat(format(target.spec.format))
                        .setUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT)
                        .setLinearFiltering(true)
                        .setClamp(true)
                        .createVulkanImage();
                target.framebuffers[i] = Framebuffer.builder(image, null).build();

                RenderPass.Builder builder = RenderPass.builder(target.framebuffers[i]);
                builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);
                target.renderPasses[i] = builder.build();
                image.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
                VkClearColorValue clearColor = VkClearColorValue.calloc(stack);
                clearColor.float32(0, target.spec.clear);
                VkImageSubresourceRange.Buffer range = VkImageSubresourceRange.calloc(1, stack);
                range.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1);
                vkCmdClearColorImage(commandBuffer, image.getId(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, clearColor, range);
                image.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            }
            target.index = 0;
            target.w = w;
            target.h = h;
        }
    }

    public static boolean targetsReady(ShaderPack pack) {
        Map<String, Target> targets = TARGETS.get(pack.id);
        if (targets == null) {
            return pack.targets.isEmpty();
        }
        for (Target target : targets.values()) {
            if (target.framebuffers[0] == null) {
                return false;
            }
        }
        return true;
    }

    public static boolean runFrame(ShaderPack pack, VkCommandBuffer commandBuffer, MemoryStack stack,
                                   ResourceResolver resolver, Runnable presentBegin) {
        Map<String, Target> targets = TARGETS.get(pack.id);
        Set<Target> written = new LinkedHashSet<>();
        boolean presented = false;

        for (PackPass pass : pack.passes) {
            GraphicsPipeline pipeline = pipeline(pack, pass);
            if (pipeline == null) {
                return false;
            }

            if ("swapchain".equals(pass.output)) {
                presentBegin.run();
                bindInputs(pass, targets, resolver);
                DrawUtil.blit(pipeline);
                presented = true;
                continue;
            }

            Target out = targets != null ? targets.get(pass.output) : null;
            if (out == null || out.framebuffers[0] == null) {
                return false;
            }
            Framebuffer fb = out.framebuffers[out.index];
            fb.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            Renderer.clearViewportScale();
            fb.beginRenderPass(commandBuffer, out.renderPasses[out.index], stack);
            bindInputs(pass, targets, resolver);
            DrawUtil.blit(pipeline);
            Renderer.getInstance().endRenderPass(commandBuffer);
            fb.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            if (out.spec.pingpong) {
                written.add(out);
            }
        }

        for (Target target : written) {
            target.index ^= 1;
        }
        return presented;
    }

    public static boolean run(ShaderPack pack, ResourceResolver resolver) {
        boolean ran = false;
        for (PackPass pass : pack.passes) {
            if (!"swapchain".equals(pass.output)) {
                continue;
            }
            GraphicsPipeline pipeline = pipeline(pack, pass);
            if (pipeline == null) {
                continue;
            }
            bindInputs(pass, null, resolver);
            DrawUtil.blit(pipeline);
            ran = true;
        }
        return ran;
    }

    private static void bindInputs(PackPass pass, Map<String, Target> targets, ResourceResolver resolver) {
        for (Map.Entry<Integer, String> in : pass.inputs.entrySet()) {
            VulkanImage img = resolveInput(in.getValue(), targets, resolver);
            if (img != null) {
                VTextureSelector.bindTexture(in.getKey(), img);
            }
        }
    }

    private static VulkanImage resolveInput(String name, Map<String, Target> targets, ResourceResolver resolver) {
        if (targets != null) {
            boolean history = name.endsWith("_history");
            String base = history ? name.substring(0, name.length() - "_history".length()) : name;
            Target target = targets.get(base);
            if (target != null && target.framebuffers[0] != null) {
                int idx = history && target.spec.pingpong ? target.index ^ 1 : target.index;
                return target.framebuffers[idx].getColorAttachment();
            }
        }
        return resolver.resolve(name);
    }

    private static int format(String name) {
        return switch (name) {
            case "R16F" -> VK_FORMAT_R16_SFLOAT;
            case "RG16F" -> VK_FORMAT_R16G16_SFLOAT;
            case "RGBA8" -> VK_FORMAT_R8G8B8A8_UNORM;
            default -> VK_FORMAT_R16G16B16A16_SFLOAT;
        };
    }

    public static void releaseTargets() {
        for (Map<String, Target> targets : TARGETS.values()) {
            for (Target target : targets.values()) {
                dispose(target);
            }
        }
        TARGETS.clear();
    }

    private static void dispose(Target target) {
        for (int i = 0; i < 2; i++) {
            if (target.renderPasses[i] != null) {
                target.renderPasses[i].cleanUp();
                target.renderPasses[i] = null;
            }
            if (target.framebuffers[i] != null) {
                target.framebuffers[i].cleanUp();
                target.framebuffers[i] = null;
            }
        }
        target.w = -1;
        target.h = -1;
    }
}
