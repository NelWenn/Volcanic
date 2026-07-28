package net.vulkanmod.render.cit;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Set;
import java.util.regex.Pattern;

public record CitRule(Set<Item> items, Pattern namePattern, ResourceLocation trimPattern, ResourceLocation trimMaterial, ResourceLocation model) {
    public boolean matches(Item item, String name, ResourceLocation pattern, ResourceLocation material) {
        if (!items.isEmpty() && !items.contains(item)) return false;
        if (namePattern != null && (name == null || !namePattern.matcher(name).matches())) return false;
        if (trimPattern != null && !trimPattern.equals(pattern)) return false;
        if (trimMaterial != null && !trimMaterial.equals(material)) return false;
        return true;
    }
}
