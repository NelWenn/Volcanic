package net.vulkanmod.render.cit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;

import java.util.List;

public final class Cit {
    private Cit() {}

    public static boolean isActive() {
        return CitPackLoader.ruleCount() > 0 && net.vulkanmod.Initializer.CONFIG.citEnabled;
    }

    public static BakedModel resolve(ItemStack stack) {
        try {
            List<CitRule> rules = CitPackLoader.rulesFor(stack.getItem());
            if (rules.isEmpty()) return null;

            String name = null;
            Component custom = stack.get(DataComponents.CUSTOM_NAME);
            if (custom != null) name = custom.getString();

            ArmorTrim trim = stack.get(DataComponents.TRIM);
            ResourceLocation pattern = null, material = null;
            if (trim != null) {
                pattern = trim.pattern().unwrapKey().map(ResourceKey::location).orElse(null);
                material = trim.material().unwrapKey().map(ResourceKey::location).orElse(null);
            }
            for (CitRule rule : rules) {
                if (rule.matches(stack.getItem(), name, pattern, material)) {
                    if (!CitPackLoader.isRenderable(rule.model())) return null;
                    BakedModel model = Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(rule.model()));
                    BakedModel missing = Minecraft.getInstance().getModelManager().getMissingModel();
                    return (model == null || model == missing) ? null : model;
                }
            }
        } catch (Throwable t) {
            return null;
        }
        return null;
    }
}
