package net.vulkanmod.vulkan.pass;

import net.vulkanmod.vulkan.texture.VulkanImage;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class DefaultEngineResourceRegistry implements EngineResourceRegistry {
    private final Map<String, Supplier<VulkanImage>> resources = new HashMap<>();

    @Override
    public void register(String name, Supplier<VulkanImage> supplier) {
        resources.put(name, supplier);
    }

    @Override
    public VulkanImage resolve(String name) {
        Supplier<VulkanImage> supplier = resources.get(name);
        return supplier != null ? supplier.get() : null;
    }
}
