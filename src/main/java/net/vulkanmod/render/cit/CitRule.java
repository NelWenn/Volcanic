package net.vulkanmod.render.cit;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Set;

public record CitRule(Set<Item> items, ResourceLocation trimPattern, ResourceLocation trimMaterial, ResourceLocation model) {
    public boolean matches(Item item, ResourceLocation pattern, ResourceLocation material) {
        if (!items.isEmpty() && !items.contains(item)) return false;
        if (trimPattern != null && !trimPattern.equals(pattern)) return false;
        if (trimMaterial != null && !trimMaterial.equals(material)) return false;
        return true;
    }
}
