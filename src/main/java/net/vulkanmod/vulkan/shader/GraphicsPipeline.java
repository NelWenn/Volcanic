package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.vulkanmod.interfaces.VertexFormatMixed;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.function.ToLongFunction;

import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class GraphicsPipeline extends Pipeline {
    private final Object2LongMap<PipelineState> graphicsPipelines = new Object2LongOpenHashMap<>();
    private static final java.util.Set<String> loggedPipelineFailures = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final VertexFormat vertexFormat;
    private final VertexInputDescription vertexInputDescription;

    private long vertShaderModule = 0;
    private long fragShaderModule = 0;

    GraphicsPipeline(Builder builder) {
        super(builder.shaderPath);
        this.buffers = builder.UBOs;
        this.manualUBO = builder.manualUBO;
        this.imageDescriptors = builder.imageDescriptors;
        this.pushConstants = builder.pushConstants;
        this.vertexFormat = builder.vertexFormat;

        this.vertexInputDescription = new VertexInputDescription(this.vertexFormat);

        createDescriptorSetLayout();
        createPipelineLayout();
        createShaderModules(builder.vertShaderSPIRV, builder.fragShaderSPIRV);

        if (builder.renderPass != null)
            graphicsPipelines.computeIfAbsent(PipelineState.DEFAULT,
                    (ToLongFunction<PipelineState>) this::createGraphicsPipeline);

        createDescriptorSets(Renderer.getFramesNum());

        PIPELINES.add(this);
    }

    public int getVariantCount() {
        return graphicsPipelines.size();
    }

    public long getHandle(PipelineState state) {
        if (net.vulkanmod.compat.observer.CompatProfiler.ENABLED) {
            if (graphicsPipelines.containsKey(state)) {
                net.vulkanmod.compat.observer.CompatProfiler.shaderCacheHits++;
            } else {
                net.vulkanmod.compat.observer.CompatProfiler.shaderCacheMisses++;
            }
        }
        return graphicsPipelines.computeIfAbsent(state, (ToLongFunction<PipelineState>) this::createGraphicsPipeline);
    }

    private long createGraphicsPipeline(PipelineState state) {
        try (MemoryStack stack = stackPush()) {
            ByteBuffer entryPoint = stack.UTF8("main");

            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);

            VkPipelineShaderStageCreateInfo vertShaderStageInfo = shaderStages.get(0);

            vertShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            vertShaderStageInfo.stage(VK_SHADER_STAGE_VERTEX_BIT);
            vertShaderStageInfo.module(vertShaderModule);
            vertShaderStageInfo.pName(entryPoint);

            VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(1);

            fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            fragShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
            fragShaderStageInfo.module(fragShaderModule);
            fragShaderStageInfo.pName(entryPoint);

            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            vertexInputInfo.pVertexBindingDescriptions(vertexInputDescription.bindingDescriptions);
            vertexInputInfo.pVertexAttributeDescriptions(vertexInputDescription.attributeDescriptions);

            final int topology = PipelineState.AssemblyRasterState.decodeTopology(state.assemblyRasterState);

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(topology);
            inputAssembly.primitiveRestartEnable(false);

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);

            viewportState.viewportCount(1);
            viewportState.scissorCount(1);

            final int polygonMode = PipelineState.AssemblyRasterState.decodePolygonMode(state.assemblyRasterState);
            final int cullMode = PipelineState.AssemblyRasterState.decodeCullMode(state.assemblyRasterState);
            final int frontFace = PipelineState.AssemblyRasterState.decodeFrontFace(state.assemblyRasterState);

            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            rasterizer.depthClampEnable(false);
            rasterizer.rasterizerDiscardEnable(false);
            rasterizer.polygonMode(polygonMode);
            rasterizer.lineWidth(1.0f);
            rasterizer.cullMode(cullMode);
            rasterizer.frontFace(frontFace);
            rasterizer.depthBiasEnable(true);

            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            multisampling.sampleShadingEnable(false);
            multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack);
            depthStencil.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
            depthStencil.depthTestEnable(PipelineState.DepthState.depthTest(state.depthState_i));
            depthStencil.depthWriteEnable(PipelineState.DepthState.depthMask(state.depthState_i));
            depthStencil.depthCompareOp(PipelineState.DepthState.decodeDepthFun(state.depthState_i));
            depthStencil.depthBoundsTestEnable(false);
            depthStencil.minDepthBounds(0.0f);
            depthStencil.maxDepthBounds(1.0f);
            boolean stencilEnabled = PipelineState.StencilState.stencilTest(state.stencilState_i)
                    && VulkanImage.hasStencilComponent(state.renderPass.getFramebuffer().getDepthFormat());
            depthStencil.stencilTestEnable(stencilEnabled);
            if (stencilEnabled) {
                depthStencil.front()
                        .failOp(PipelineState.StencilState.decodeFailOp(state.stencilState_i))
                        .passOp(PipelineState.StencilState.decodePassOp(state.stencilState_i))
                        .depthFailOp(PipelineState.StencilState.decodeDepthFailOp(state.stencilState_i))
                        .compareOp(PipelineState.StencilState.decodeCompareOp(state.stencilState_i))
                        .compareMask(0)
                        .writeMask(0)
                        .reference(0);
                depthStencil.back(depthStencil.front());
            }

            int colorAttachmentCount = 1;
            if (!Vulkan.DYNAMIC_RENDERING && state.renderPass != null && state.renderPass.getFramebuffer() != null) {
                colorAttachmentCount = Math.max(1, state.renderPass.getFramebuffer().getColorAttachmentCount());
            }

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(colorAttachmentCount, stack);
            colorBlendAttachment.get(0).colorWriteMask(state.colorMask_i);

            if (PipelineState.BlendState.enable(state.blendState_i)) {
                colorBlendAttachment.get(0).blendEnable(true);
                colorBlendAttachment.get(0).srcColorBlendFactor(PipelineState.BlendState.getSrcRgbFactor(state.blendState_i));
                colorBlendAttachment.get(0).dstColorBlendFactor(PipelineState.BlendState.getDstRgbFactor(state.blendState_i));
                colorBlendAttachment.get(0).colorBlendOp(PipelineState.BlendState.getColorBlendOp(state.blendState_i));
                colorBlendAttachment.get(0).srcAlphaBlendFactor(PipelineState.BlendState.getSrcAlphaFactor(state.blendState_i));
                colorBlendAttachment.get(0).dstAlphaBlendFactor(PipelineState.BlendState.getDstAlphaFactor(state.blendState_i));
                colorBlendAttachment.get(0).alphaBlendOp(PipelineState.BlendState.getAlphaBlendOp(state.blendState_i));
            }
            else {
                colorBlendAttachment.get(0).blendEnable(false);
            }

            for (int a = 1; a < colorAttachmentCount; a++) {
                colorBlendAttachment.get(a)
                        .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT)
                        .blendEnable(false);
            }

            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(PipelineState.LogicOpState.enable(state.logicOp_i));
            colorBlending.logicOp(PipelineState.LogicOpState.decodeFun(state.logicOp_i));
            colorBlending.pAttachments(colorBlendAttachment);

            VkPipelineDynamicStateCreateInfo dynamicStates = VkPipelineDynamicStateCreateInfo.calloc(stack);
            dynamicStates.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);

            if (topology == VK_PRIMITIVE_TOPOLOGY_LINE_LIST || polygonMode == VK_POLYGON_MODE_LINE) {
                if (stencilEnabled)
                    dynamicStates.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_DEPTH_BIAS, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR,
                            VK_DYNAMIC_STATE_LINE_WIDTH, VK_DYNAMIC_STATE_STENCIL_COMPARE_MASK, VK_DYNAMIC_STATE_STENCIL_WRITE_MASK,
                            VK_DYNAMIC_STATE_STENCIL_REFERENCE, VK_DYNAMIC_STATE_BLEND_CONSTANTS));
                else
                    dynamicStates.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_DEPTH_BIAS, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR,
                            VK_DYNAMIC_STATE_LINE_WIDTH, VK_DYNAMIC_STATE_BLEND_CONSTANTS));
            } else {
                if (stencilEnabled)
                    dynamicStates.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_DEPTH_BIAS, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR,
                            VK_DYNAMIC_STATE_STENCIL_COMPARE_MASK, VK_DYNAMIC_STATE_STENCIL_WRITE_MASK,
                            VK_DYNAMIC_STATE_STENCIL_REFERENCE, VK_DYNAMIC_STATE_BLEND_CONSTANTS));
                else
                    dynamicStates.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_DEPTH_BIAS, VK_DYNAMIC_STATE_VIEWPORT,
                            VK_DYNAMIC_STATE_SCISSOR, VK_DYNAMIC_STATE_BLEND_CONSTANTS));
            }

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            pipelineInfo.pStages(shaderStages);
            pipelineInfo.pVertexInputState(vertexInputInfo);
            pipelineInfo.pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState);
            pipelineInfo.pRasterizationState(rasterizer);
            pipelineInfo.pMultisampleState(multisampling);
            pipelineInfo.pDepthStencilState(depthStencil);
            pipelineInfo.pColorBlendState(colorBlending);
            pipelineInfo.pDynamicState(dynamicStates);
            pipelineInfo.layout(pipelineLayout);
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);

            if (!Vulkan.DYNAMIC_RENDERING) {
                pipelineInfo.renderPass(state.renderPass.getId());
                pipelineInfo.subpass(0);
            }
            else {

                VkPipelineRenderingCreateInfoKHR renderingInfo = VkPipelineRenderingCreateInfoKHR.calloc(stack);
                renderingInfo.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO_KHR);
                renderingInfo.pColorAttachmentFormats(stack.ints(state.renderPass.getFramebuffer().getFormat()));
                renderingInfo.depthAttachmentFormat(state.renderPass.getFramebuffer().getDepthFormat());
                renderingInfo.stencilAttachmentFormat(VulkanImage.hasStencilComponent(state.renderPass.getFramebuffer().getDepthFormat())
                        ? state.renderPass.getFramebuffer().getDepthFormat() : VK_FORMAT_UNDEFINED);
                pipelineInfo.pNext(renderingInfo);
            }

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);

            int pipelineResult = vkCreateGraphicsPipelines(DeviceManager.vkDevice, PIPELINE_CACHE, pipelineInfo, null, pGraphicsPipeline);
            if (pipelineResult != VK_SUCCESS) {
                if (loggedPipelineFailures.add(this.name)) {
                    net.vulkanmod.Initializer.LOGGER.error("Failed to create graphics pipeline '{}' (vertexFormat={}, colorFormat={}, VkResult={}); skipping this render type instead of crashing",
                            this.name, this.vertexFormat, state.renderPass.getFramebuffer().getFormat(), pipelineResult);
                }
                return VK_NULL_HANDLE;
            }

            return pGraphicsPipeline.get(0);
        }
    }

    private void createShaderModules(SPIRVUtils.SPIRV vertSpirv, SPIRVUtils.SPIRV fragSpirv) {
        this.vertShaderModule = createShaderModule(vertSpirv.bytecode());
        this.fragShaderModule = createShaderModule(fragSpirv.bytecode());
    }

    public void cleanUp() {
        vkDestroyShaderModule(DeviceManager.vkDevice, vertShaderModule, null);
        vkDestroyShaderModule(DeviceManager.vkDevice, fragShaderModule, null);

        vertexInputDescription.cleanUp();

        destroyDescriptorSets();

        graphicsPipelines.forEach((state, pipeline) -> {
            vkDestroyPipeline(DeviceManager.vkDevice, pipeline, null);
        });
        graphicsPipelines.clear();

        vkDestroyDescriptorSetLayout(DeviceManager.vkDevice, descriptorSetLayout, null);
        vkDestroyPipelineLayout(DeviceManager.vkDevice, pipelineLayout, null);

        PIPELINES.remove(this);
        Renderer.getInstance().removeUsedPipeline(this);
    }

    static class VertexInputDescription {
        final VkVertexInputAttributeDescription.Buffer attributeDescriptions;
        final VkVertexInputBindingDescription.Buffer bindingDescriptions;

        VertexInputDescription(VertexFormat vertexFormat) {
            this.bindingDescriptions = getBindingDescription(vertexFormat);
            this.attributeDescriptions = getAttributeDescriptions(vertexFormat);
        }

        void cleanUp() {
            MemoryUtil.memFree(this.bindingDescriptions);
            MemoryUtil.memFree(this.attributeDescriptions);
        }
    }

    private static VkVertexInputBindingDescription.Buffer getBindingDescription(VertexFormat vertexFormat) {
        VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.calloc(1);

        bindingDescription.binding(0);
        bindingDescription.stride(vertexFormat.getVertexSize());
        bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

        return bindingDescription;
    }

    private static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions(VertexFormat vertexFormat) {
        List<VertexFormatElement> elements = vertexFormat.getElements();

        int size = elements.size();

        VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(size);

        int offset = 0;

        for (int i = 0; i < size; ++i) {
            VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(i);
            posDescription.binding(0);
            posDescription.location(i);

            VertexFormatElement formatElement = elements.get(i);
            VertexFormatElement.Usage usage = formatElement.usage();
            VertexFormatElement.Type type = formatElement.type();
            int elementCount = formatElement.count();

            switch (usage) {
                case POSITION -> {
                    switch (type) {
                        case FLOAT -> {
                            posDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
                            posDescription.offset(offset);

                            offset += 12;
                        }
                        case SHORT -> {
                            posDescription.format(VK_FORMAT_R16G16B16A16_SINT);
                            posDescription.offset(offset);

                            offset += 8;
                        }
                        case USHORT -> {
                            posDescription.format(VK_FORMAT_R16G16B16A16_UINT);
                            posDescription.offset(offset);

                            offset += 8;
                        }
                        case BYTE -> {
                            posDescription.format(VK_FORMAT_R8G8B8A8_SINT);
                            posDescription.offset(offset);

                            offset += 4;
                        }
                        default -> throw new RuntimeException(String.format("Unknown type %s for POSITION", type));
                    }

                }

                case COLOR -> {
                    posDescription.format(VK_FORMAT_R8G8B8A8_UNORM);
                    posDescription.offset(offset);

                    offset += 4;
                }

                case UV -> {
                    switch (type) {
                        case FLOAT -> {
                            posDescription.format(VK_FORMAT_R32G32_SFLOAT);
                            posDescription.offset(offset);

                            offset += 8;
                        }
                        case SHORT -> {
                            posDescription.format(VK_FORMAT_R16G16_SINT);
                            posDescription.offset(offset);

                            offset += 4;
                        }
                        case USHORT -> {
                            posDescription.format(VK_FORMAT_R16G16_UINT);
                            posDescription.offset(offset);

                            offset += 4;
                        }
                        default -> throw new RuntimeException(String.format("Unknown type %s for UV", type));
                    }
                }

                case NORMAL -> {
                    posDescription.format(VK_FORMAT_R8G8B8A8_SNORM);
                    posDescription.offset(offset);

                    offset += 4;
                }

                case GENERIC -> {
                    if (type == VertexFormatElement.Type.SHORT && elementCount == 1) {
                        posDescription.format(VK_FORMAT_R16_SINT);
                        posDescription.offset(offset);

                        offset += 2;
                    }
                    else if (type == VertexFormatElement.Type.INT && elementCount == 1) {
                        posDescription.format(VK_FORMAT_R32_SINT);
                        posDescription.offset(offset);

                        offset += 4;
                    }
                    else {
                        throw new RuntimeException(String.format("Unknown format: %s", usage));
                    }
                }

                default -> throw new RuntimeException(String.format("Unknown format: %s", usage));
            }

            posDescription.offset(((VertexFormatMixed) (vertexFormat)).getOffset(i));
        }

        return attributeDescriptions.rewind();
    }
}
