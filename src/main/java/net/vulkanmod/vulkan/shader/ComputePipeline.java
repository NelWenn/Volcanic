package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.memory.MemoryManager;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkComputePipelineCreateInfo;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class ComputePipeline {
    private static final int MIN_POOL_CAPACITY = 32;

    public final String name;

    private final int[] descriptorTypes;
    private final int pushConstantSize;

    private long descriptorSetLayout;
    private long pipelineLayout;
    private long shaderModule;
    private long handle;

    private long[] descriptorPools = new long[0];
    private int[] poolCapacities = new int[0];
    private int[] poolUsed = new int[0];
    private int lastPreparedFrame = -1;

    public sealed interface Resource permits BufferResource, SamplerResource, StorageImageResource {
    }

    public record BufferResource(long buffer, long offset, long range) implements Resource {
    }

    public record SamplerResource(long imageView, long samplerHandle, int imageLayout) implements Resource {
    }

    public record StorageImageResource(long imageView) implements Resource {
    }

    public ComputePipeline(String shaderPath, int[] descriptorTypes, int pushConstantSize) {
        this.name = shaderPath;
        this.descriptorTypes = descriptorTypes.clone();
        this.pushConstantSize = pushConstantSize;

        SPIRVUtils.SPIRV spirv = SPIRVUtils.compileShaderAbsoluteFile(
                String.format("/assets/vulkanmod/shaders/%s.csh", shaderPath), SPIRVUtils.ShaderKind.COMPUTE_SHADER);
        Validate.notNull(spirv, "Failed to compile compute shader: %s", shaderPath);

        this.shaderModule = Pipeline.createShaderModule(spirv.bytecode());

        createDescriptorSetLayout();
        createPipelineLayout();
        createPipeline();
    }

    private void createDescriptorSetLayout() {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(this.descriptorTypes.length, stack);

            for (int i = 0; i < this.descriptorTypes.length; ++i) {
                bindings.get(i)
                        .binding(i)
                        .descriptorCount(1)
                        .descriptorType(this.descriptorTypes[i])
                        .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
            }

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
            layoutInfo.sType$Default();
            layoutInfo.pBindings(bindings);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);
            if (vkCreateDescriptorSetLayout(DeviceManager.vkDevice, layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create compute descriptor set layout");
            }

            this.descriptorSetLayout = pDescriptorSetLayout.get(0);
        }
    }

    private void createPipelineLayout() {
        try (MemoryStack stack = stackPush()) {
            VkPipelineLayoutCreateInfo layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            layoutInfo.sType$Default();
            layoutInfo.pSetLayouts(stack.longs(this.descriptorSetLayout));

            if (this.pushConstantSize > 0) {
                VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.calloc(1, stack);
                pushConstantRange.size(this.pushConstantSize);
                pushConstantRange.offset(0);
                pushConstantRange.stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);

                layoutInfo.pPushConstantRanges(pushConstantRange);
            }

            LongBuffer pPipelineLayout = stack.mallocLong(1);
            if (vkCreatePipelineLayout(DeviceManager.vkDevice, layoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create compute pipeline layout");
            }

            this.pipelineLayout = pPipelineLayout.get(0);
        }
    }

    private void createPipeline() {
        try (MemoryStack stack = stackPush()) {
            VkPipelineShaderStageCreateInfo stage = VkPipelineShaderStageCreateInfo.calloc(stack);
            stage.sType$Default();
            stage.stage(VK_SHADER_STAGE_COMPUTE_BIT);
            stage.module(this.shaderModule);
            stage.pName(stack.UTF8("main"));

            VkComputePipelineCreateInfo.Buffer pipelineInfo = VkComputePipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType$Default();
            pipelineInfo.stage(stage);
            pipelineInfo.layout(this.pipelineLayout);
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);

            LongBuffer pPipeline = stack.mallocLong(1);
            int result = vkCreateComputePipelines(DeviceManager.vkDevice, Pipeline.PIPELINE_CACHE, pipelineInfo, null, pPipeline);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create compute pipeline '%s': %d".formatted(this.name, result));
            }

            this.handle = pPipeline.get(0);
        }
    }

    public void bind(VkCommandBuffer commandBuffer) {
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, this.handle);
    }

    public void bindResources(VkCommandBuffer commandBuffer, Resource... resources) {
        Validate.isTrue(resources.length == this.descriptorTypes.length,
                "Expected %d resources, got %d", this.descriptorTypes.length, resources.length);

        long set = allocateSet(Renderer.getCurrentFrame());

        try (MemoryStack stack = stackPush()) {
            VkWriteDescriptorSet.Buffer writes = VkWriteDescriptorSet.calloc(resources.length, stack);

            for (int i = 0; i < resources.length; ++i) {
                VkWriteDescriptorSet write = writes.get(i);
                write.sType$Default();
                write.dstSet(set);
                write.dstBinding(i);
                write.dstArrayElement(0);
                write.descriptorType(this.descriptorTypes[i]);
                write.descriptorCount(1);

                switch (resources[i]) {
                    case BufferResource buffer -> {
                        VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
                        bufferInfo.buffer(buffer.buffer());
                        bufferInfo.offset(buffer.offset());
                        bufferInfo.range(buffer.range());
                        write.pBufferInfo(bufferInfo);
                    }
                    case SamplerResource sampled -> {
                        VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
                        imageInfo.imageView(sampled.imageView());
                        imageInfo.sampler(sampled.samplerHandle());
                        imageInfo.imageLayout(sampled.imageLayout());
                        write.pImageInfo(imageInfo);
                    }
                    case StorageImageResource storage -> {
                        VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
                        imageInfo.imageView(storage.imageView());
                        imageInfo.imageLayout(VK_IMAGE_LAYOUT_GENERAL);
                        write.pImageInfo(imageInfo);
                    }
                }
            }

            vkUpdateDescriptorSets(DeviceManager.vkDevice, writes, null);
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, this.pipelineLayout,
                    0, stack.longs(set), null);
        }
    }

    public void pushConstants(VkCommandBuffer commandBuffer, ByteBuffer data) {
        vkCmdPushConstants(commandBuffer, this.pipelineLayout, VK_SHADER_STAGE_COMPUTE_BIT, 0, data);
    }

    public void dispatch(VkCommandBuffer commandBuffer, int groupCountX, int groupCountY, int groupCountZ) {
        vkCmdDispatch(commandBuffer, groupCountX, groupCountY, groupCountZ);
    }

    public long getLayout() {
        return this.pipelineLayout;
    }

    private long allocateSet(int frame) {
        int framesNum = Renderer.getFramesNum();
        if (this.descriptorPools.length != framesNum) {
            releasePools();
            this.descriptorPools = new long[framesNum];
            this.poolCapacities = new int[framesNum];
            this.poolUsed = new int[framesNum];
            this.lastPreparedFrame = -1;
        }

        if (frame != this.lastPreparedFrame) {
            if (this.descriptorPools[frame] != VK_NULL_HANDLE) {
                vkResetDescriptorPool(DeviceManager.vkDevice, this.descriptorPools[frame], 0);
            }
            this.poolUsed[frame] = 0;
            this.lastPreparedFrame = frame;
        }

        if (this.descriptorPools[frame] == VK_NULL_HANDLE || this.poolUsed[frame] >= this.poolCapacities[frame]) {
            growPool(frame);
        }

        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
            allocInfo.sType$Default();
            allocInfo.descriptorPool(this.descriptorPools[frame]);
            allocInfo.pSetLayouts(stack.longs(this.descriptorSetLayout));

            LongBuffer pSet = stack.mallocLong(1);
            int result = vkAllocateDescriptorSets(DeviceManager.vkDevice, allocInfo, pSet);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate compute descriptor set: %d".formatted(result));
            }

            this.poolUsed[frame]++;
            return pSet.get(0);
        }
    }

    private void growPool(int frame) {
        int newCapacity = Math.max(MIN_POOL_CAPACITY, this.poolCapacities[frame] * 2);

        try (MemoryStack stack = stackPush()) {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(this.descriptorTypes.length, stack);
            for (int i = 0; i < this.descriptorTypes.length; ++i) {
                poolSizes.get(i)
                        .type(this.descriptorTypes[i])
                        .descriptorCount(newCapacity);
            }

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType$Default();
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(newCapacity);

            LongBuffer pDescriptorPool = stack.mallocLong(1);
            if (vkCreateDescriptorPool(DeviceManager.vkDevice, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create compute descriptor pool");
            }

            if (this.descriptorPools[frame] != VK_NULL_HANDLE) {
                final long oldPool = this.descriptorPools[frame];
                MemoryManager.getInstance().addFrameOp(() -> vkDestroyDescriptorPool(DeviceManager.vkDevice, oldPool, null));
            }

            this.descriptorPools[frame] = pDescriptorPool.get(0);
            this.poolCapacities[frame] = newCapacity;
            this.poolUsed[frame] = 0;
        }
    }

    private void releasePools() {
        for (long pool : this.descriptorPools) {
            if (pool != VK_NULL_HANDLE) {
                final long oldPool = pool;
                MemoryManager.getInstance().addFrameOp(() -> vkDestroyDescriptorPool(DeviceManager.vkDevice, oldPool, null));
            }
        }
    }

    public void cleanUp() {
        releasePools();
        this.descriptorPools = new long[0];
        this.poolCapacities = new int[0];
        this.poolUsed = new int[0];

        vkDestroyPipeline(DeviceManager.vkDevice, this.handle, null);
        vkDestroyPipelineLayout(DeviceManager.vkDevice, this.pipelineLayout, null);
        vkDestroyDescriptorSetLayout(DeviceManager.vkDevice, this.descriptorSetLayout, null);
        vkDestroyShaderModule(DeviceManager.vkDevice, this.shaderModule, null);

        this.handle = VK_NULL_HANDLE;
        this.pipelineLayout = VK_NULL_HANDLE;
        this.descriptorSetLayout = VK_NULL_HANDLE;
        this.shaderModule = VK_NULL_HANDLE;
    }
}
