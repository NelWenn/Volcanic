package net.vulkanmod.vulkan.pass;

import net.vulkanmod.vulkan.texture.VulkanImage;

import java.util.function.Supplier;

/**
 * Registry that maps named engine resources to VulkanImages.
 * The engine registers core resources (scene, gnormal), plugins
 * register their own (shadowtex, material) via lifecycle hooks.
 */
public interface EngineResourceRegistry {

    void register(String name, Supplier<VulkanImage> supplier);

    VulkanImage resolve(String name);
}
