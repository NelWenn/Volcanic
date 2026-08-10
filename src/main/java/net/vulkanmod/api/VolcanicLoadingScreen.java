package net.vulkanmod.api;

import net.vulkanmod.Initializer;

import java.util.LinkedHashSet;
import java.util.Set;

public final class VolcanicLoadingScreen {
    private static final Set<String> claims = new LinkedHashSet<>();

    private VolcanicLoadingScreen() {
    }

    public static void claim(String modId) {
        if (modId == null || modId.isBlank()) {
            throw new IllegalArgumentException("modId must not be blank");
        }
        if (claims.add(modId)) {
            Initializer.LOGGER.info("Loading screen handed to {}", modId);
        }
    }

    public static void release(String modId) {
        if (modId != null && claims.remove(modId)) {
            Initializer.LOGGER.info("Loading screen released by {}", modId);
        }
    }

    public static Set<String> claimants() {
        return Set.copyOf(claims);
    }

    public static boolean painting() {
        if (!claims.isEmpty()) {
            return false;
        }
        try {
            return Initializer.CONFIG == null || Initializer.CONFIG.loadingScreen;
        } catch (Throwable unavailable) {
            return true;
        }
    }
}
