package net.vulkanmod.config.ui.core;

import java.util.Collection;

public enum ApplyScope {
    INSTANT,
    CHUNK_REBUILD,
    TEXTURE_RELOAD,
    SWAPCHAIN,
    WINDOW,
    RESTART;

    public static ApplyScope heaviest(Collection<ApplyScope> scopes) {
        if (scopes == null) {
            throw new IllegalArgumentException("scopes must not be null");
        }
        ApplyScope heaviest = INSTANT;
        for (ApplyScope scope : scopes) {
            if (scope != null && scope.ordinal() > heaviest.ordinal()) {
                heaviest = scope;
            }
        }
        return heaviest;
    }
}
