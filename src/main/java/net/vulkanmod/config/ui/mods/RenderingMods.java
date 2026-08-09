package net.vulkanmod.config.ui.mods;

import java.util.Locale;
import java.util.Set;

public final class RenderingMods {
    private static final Set<String> KNOWN = Set.of(
            "caldera",
            "moreculling",
            "dynamicfps",
            "entityculling",
            "immediatelyfast",
            "modernfix",
            "ferritecore",
            "betterfpsdist",
            "noisium",
            "distanthorizons",
            "bobby",
            "cullleaves",
            "cullparticles",
            "enhancedblockentities",
            "animatica",
            "continuity",
            "polytone");

    private RenderingMods() {
    }

    public static boolean isRenderingMod(String modId) {
        if (modId == null || modId.isBlank()) {
            throw new IllegalArgumentException("modId must not be blank");
        }
        return KNOWN.contains(modId.toLowerCase(Locale.ROOT));
    }

    public static Set<String> known() {
        return KNOWN;
    }
}
