package net.vulkanmod.vulkan.pass;

import net.vulkanmod.vulkan.texture.VulkanImage;
import org.joml.Matrix4f;

public interface MaterialProvider extends PipelineFeature {

    void prepareMaterialBuffer(double camX, double camY, double camZ,
                               Matrix4f modelView, Matrix4f projection);

    void renderMaterialBuffer();

    VulkanImage getMaterialImage();

    VulkanImage getMaterialDepthImage();
}
