package net.vulkanmod.render.framegraph;

import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.pipeline.PipelineDefinition;
import net.vulkanmod.vulkan.shader.pipeline.PipelineRegistry;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.DrawUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkClearColorValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkImageSubresourceRange;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.vulkan.VK10.*;

public final class FrameGraph {
    public static final String SWAPCHAIN = "swapchain";

    public interface ResourceResolver {
        VulkanImage resolve(String name);
    }

    static final class Resource {
        final String name;
        final int vkFormat;
        final float scale;
        final float clear;
        final boolean pingpong;

        final Framebuffer[] framebuffers = new Framebuffer[2];
        final RenderPass[] renderPasses = new RenderPass[2];
        int index;
        int w = -1, h = -1;
        int firstWrite = -1, lastRead = -1;

        Resource(String name, int vkFormat, float scale, float clear, boolean pingpong) {
            this.name = name;
            this.vkFormat = vkFormat;
            this.scale = scale;
            this.clear = clear;
            this.pingpong = pingpong;
        }

        int buffers() {
            return this.pingpong ? 2 : 1;
        }
    }

    static final class Node {
        final Class<? extends PipelineDefinition> pipeline;
        final Phase phase;
        final Class<? extends PassExecutor> executor;
        final Map<Integer, String> inputs;
        final String output;
        PassExecutor executorInstance;

        Node(Class<? extends PipelineDefinition> pipeline, Phase phase, Class<? extends PassExecutor> executor,
             Map<Integer, String> inputs, String output) {
            this.pipeline = pipeline;
            this.phase = phase;
            this.executor = executor;
            this.inputs = inputs;
            this.output = output;
        }

        boolean isExecutor() {
            return this.executor != PassExecutor.class;
        }
    }

    private final String id;
    private final Map<String, Resource> targets = new LinkedHashMap<>();
    private final List<Node> passes;

    private FrameGraph(String id, Map<String, Resource> targets, List<Node> passes) {
        this.id = id;
        this.targets.putAll(targets);
        this.passes = passes;
    }

    private void compile() {
        for (int i = 0; i < this.passes.size(); i++) {
            Node pass = this.passes.get(i);
            Resource out = this.targets.get(pass.output);
            if (out != null && out.firstWrite < 0) {
                out.firstWrite = i;
            }
            for (String in : pass.inputs.values()) {
                String base = in.endsWith("_history") ? in.substring(0, in.length() - "_history".length()) : in;
                Resource r = this.targets.get(base);
                if (r != null) {
                    r.lastRead = i;
                }
            }
        }
        for (Resource r : this.targets.values()) {
            if (r.firstWrite < 0) {
                Initializer.LOGGER.warn("Frame graph '{}': target '{}' is never written", this.id, r.name);
            }
        }
    }

    public boolean pipelinesReady() {
        for (Node pass : this.passes) {
            if (!pass.isExecutor() && PipelineRegistry.getOrNull(pass.pipeline) == null) {
                return false;
            }
        }
        return true;
    }

    public void resize(VkCommandBuffer commandBuffer, MemoryStack stack, int mainWidth, int mainHeight) {
        if (mainWidth <= 0 || mainHeight <= 0) {
            return;
        }
        for (Resource target : this.targets.values()) {
            int w = Math.max(1, (int) (mainWidth * target.scale));
            int h = Math.max(1, (int) (mainHeight * target.scale));
            if (target.framebuffers[0] != null && target.w == w && target.h == h) {
                continue;
            }
            dispose(target);
            for (int i = 0; i < target.buffers(); i++) {
                VulkanImage image = VulkanImage.builder(w, h)
                        .setFormat(target.vkFormat)
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
                clearColor.float32(0, target.clear);
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

    public boolean targetsReady() {
        for (Resource target : this.targets.values()) {
            if (target.framebuffers[0] == null) {
                return false;
            }
        }
        return true;
    }

    public boolean execute(Phase phase, VkCommandBuffer commandBuffer, MemoryStack stack, ResourceResolver resolver, Runnable presentBegin) {
        List<Resource> swapped = new ArrayList<>();
        boolean presented = false;

        for (Node pass : this.passes) {
            if (pass.phase != phase) {
                continue;
            }
            if (pass.isExecutor()) {
                runExecutor(pass, commandBuffer, stack);
                continue;
            }
            GraphicsPipeline pipeline = PipelineRegistry.getOrNull(pass.pipeline);
            if (pipeline == null) {
                return false;
            }

            if (SWAPCHAIN.equals(pass.output)) {
                presentBegin.run();
                bindInputs(pass, resolver);
                DrawUtil.blit(pipeline);
                presented = true;
                continue;
            }

            Resource out = this.targets.get(pass.output);
            if (out == null || out.framebuffers[0] == null) {
                return false;
            }
            Framebuffer fb = out.framebuffers[out.index];
            fb.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            Renderer.clearViewportScale();
            fb.beginRenderPass(commandBuffer, out.renderPasses[out.index], stack);
            bindInputs(pass, resolver);
            DrawUtil.blit(pipeline);
            Renderer.getInstance().endRenderPass(commandBuffer);
            fb.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            if (out.pingpong) {
                swapped.add(out);
            }
        }

        for (Resource r : swapped) {
            r.index ^= 1;
        }
        return presented;
    }

    private void runExecutor(Node pass, VkCommandBuffer commandBuffer, MemoryStack stack) {
        if (pass.executorInstance == null) {
            try {
                pass.executorInstance = pass.executor.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot instantiate pass executor " + pass.executor.getName(), e);
            }
        }
        pass.executorInstance.execute(commandBuffer, stack);
    }

    private void bindInputs(Node pass, ResourceResolver resolver) {
        for (Map.Entry<Integer, String> in : pass.inputs.entrySet()) {
            VulkanImage img = resolveInput(in.getValue(), resolver);
            if (img != null) {
                VTextureSelector.bindTexture(in.getKey(), img);
            }
        }
    }

    private VulkanImage resolveInput(String name, ResourceResolver resolver) {
        boolean history = name.endsWith("_history");
        String base = history ? name.substring(0, name.length() - "_history".length()) : name;
        Resource target = this.targets.get(base);
        if (target != null && target.framebuffers[0] != null) {
            int idx = history && target.pingpong ? target.index ^ 1 : target.index;
            return target.framebuffers[idx].getColorAttachment();
        }
        return resolver.resolve(name);
    }

    public void dispose() {
        for (Resource target : this.targets.values()) {
            dispose(target);
        }
    }

    private static void dispose(Resource target) {
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

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static FrameGraph fromPasses(String id, Class<?>... passClasses) {
        Builder builder = new Builder(id);
        List<PassSpec> specs = new ArrayList<>();
        for (Class<?> passClass : passClasses) {
            Pass ann = passClass.getAnnotation(Pass.class);
            if (ann == null) {
                throw new IllegalStateException(passClass.getName() + " has no @Pass annotation");
            }
            boolean executorPass = ann.executor() != PassExecutor.class;
            Map<Integer, String> inputs = new LinkedHashMap<>();
            List<String> outputs = new ArrayList<>();
            int slot = 0;
            for (Field field : passClass.getDeclaredFields()) {
                Input in = field.getAnnotation(Input.class);
                if (in != null) {
                    inputs.put(slot++, in.value());
                    continue;
                }
                Output out = field.getAnnotation(Output.class);
                if (out != null) {
                    outputs.add(out.value());
                    if (!SWAPCHAIN.equals(out.value()) && !executorPass) {
                        builder.target(out.value(), out.format().vk, out.scale(), out.clear(), out.pingpong());
                    }
                }
            }
            if (outputs.isEmpty()) {
                throw new IllegalStateException(passClass.getName() + " has no @Output field");
            }
            specs.add(new PassSpec(ann.pipeline(), ann.phase(), ann.executor(), inputs, outputs));
        }
        for (PassSpec spec : topoSort(id, specs)) {
            PassBuilder passBuilder = builder.pass(spec.pipeline()).phase(spec.phase()).executor(spec.executor());
            for (Map.Entry<Integer, String> input : spec.inputs().entrySet()) {
                passBuilder.in(input.getKey(), input.getValue());
            }
            passBuilder.out(spec.outputs().get(0));
        }
        return builder.build();
    }

    private record PassSpec(Class<? extends PipelineDefinition> pipeline, Phase phase,
                            Class<? extends PassExecutor> executor, Map<Integer, String> inputs, List<String> outputs) {
    }

    private static List<PassSpec> topoSort(String id, List<PassSpec> specs) {
        Map<String, PassSpec> producer = new HashMap<>();
        for (PassSpec spec : specs) {
            for (String out : spec.outputs()) {
                producer.put(out, spec);
            }
        }
        Map<PassSpec, Set<PassSpec>> dependencies = new LinkedHashMap<>();
        Map<PassSpec, Integer> indegree = new LinkedHashMap<>();
        for (PassSpec spec : specs) {
            dependencies.put(spec, new LinkedHashSet<>());
            indegree.put(spec, 0);
        }
        for (PassSpec spec : specs) {
            for (String input : spec.inputs().values()) {
                if (input.endsWith("_history")) {
                    continue;
                }
                PassSpec upstream = producer.get(input);
                if (upstream != null && upstream != spec && dependencies.get(spec).add(upstream)) {
                    indegree.merge(spec, 1, Integer::sum);
                }
            }
        }
        Deque<PassSpec> ready = new ArrayDeque<>();
        for (PassSpec spec : specs) {
            if (indegree.get(spec) == 0) {
                ready.add(spec);
            }
        }
        List<PassSpec> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            PassSpec spec = ready.poll();
            ordered.add(spec);
            for (PassSpec other : specs) {
                if (dependencies.get(other).remove(spec) && indegree.merge(other, -1, Integer::sum) == 0) {
                    ready.add(other);
                }
            }
        }
        if (ordered.size() != specs.size()) {
            throw new IllegalStateException("Frame graph '" + id + "' has a cyclic pass dependency");
        }
        return ordered;
    }

    public static final class Builder {
        private final String id;
        private final Map<String, Resource> targets = new LinkedHashMap<>();
        private final List<Node> passes = new ArrayList<>();

        private Builder(String id) {
            this.id = id;
        }

        public Builder target(String name, int vkFormat, float scale, float clear, boolean pingpong) {
            this.targets.put(name, new Resource(name, vkFormat, scale, clear, pingpong));
            return this;
        }

        public PassBuilder pass(Class<? extends PipelineDefinition> pipeline) {
            return new PassBuilder(this, pipeline);
        }

        public FrameGraph build() {
            FrameGraph graph = new FrameGraph(this.id, this.targets, this.passes);
            graph.compile();
            return graph;
        }
    }

    public static final class PassBuilder {
        private final Builder parent;
        private final Class<? extends PipelineDefinition> pipeline;
        private Phase phase = Phase.POST_PROCESS;
        private Class<? extends PassExecutor> executor = PassExecutor.class;
        private final Map<Integer, String> inputs = new LinkedHashMap<>();

        private PassBuilder(Builder parent, Class<? extends PipelineDefinition> pipeline) {
            this.parent = parent;
            this.pipeline = pipeline;
        }

        public PassBuilder phase(Phase phase) {
            this.phase = phase;
            return this;
        }

        public PassBuilder executor(Class<? extends PassExecutor> executor) {
            this.executor = executor;
            return this;
        }

        public PassBuilder in(int binding, String resource) {
            this.inputs.put(binding, resource);
            return this;
        }

        public Builder out(String output) {
            this.parent.passes.add(new Node(this.pipeline, this.phase, this.executor, this.inputs, output));
            return this.parent;
        }
    }
}
