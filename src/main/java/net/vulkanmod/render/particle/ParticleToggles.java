package net.vulkanmod.render.particle;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.Initializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ParticleToggles {
    private static Set<ParticleType<?>> blocked = Set.of();
    private static int stamp = -1;

    private ParticleToggles() {
    }

    public static List<ResourceLocation> ids() {
        List<ResourceLocation> ids = new ArrayList<>(BuiltInRegistries.PARTICLE_TYPE.keySet());
        ids.sort(ResourceLocation::compareTo);
        return List.copyOf(ids);
    }

    public static boolean enabled(ResourceLocation id) {
        return !stored().contains(id.toString());
    }

    public static void setEnabled(ResourceLocation id, boolean enabled) {
        List<String> disabled = stored();
        boolean changed = enabled
                ? disabled.remove(id.toString())
                : !disabled.contains(id.toString()) && disabled.add(id.toString());
        if (changed) {
            Collections.sort(disabled);
            invalidate();
        }
    }

    public static boolean blocked(ParticleType<?> type) {
        return cache().contains(type);
    }

    public static void invalidate() {
        stamp = -1;
    }

    private static Set<ParticleType<?>> cache() {
        List<String> disabled = stored();
        if (stamp != disabled.hashCode()) {
            Set<ParticleType<?>> types = new HashSet<>();
            for (String id : disabled) {
                ResourceLocation key = ResourceLocation.tryParse(id);
                ParticleType<?> type = key == null ? null : BuiltInRegistries.PARTICLE_TYPE.get(key);
                if (type != null) {
                    types.add(type);
                }
            }
            blocked = types;
            stamp = disabled.hashCode();
        }
        return blocked;
    }

    private static List<String> stored() {
        if (Initializer.CONFIG.disabledParticles == null) {
            Initializer.CONFIG.disabledParticles = new ArrayList<>();
        }
        return Initializer.CONFIG.disabledParticles;
    }
}
