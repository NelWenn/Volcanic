package net.vulkanmod.render.ctm;

import java.util.HashSet;
import java.util.Set;

public final class BiomeMatcher {
    private final Set<String> names;
    private final boolean negated;
    private final boolean any;

    private BiomeMatcher(Set<String> names, boolean negated, boolean any) {
        this.names = names;
        this.negated = negated;
        this.any = any;
    }

    public static BiomeMatcher any() {
        return new BiomeMatcher(Set.of(), false, true);
    }

    public static BiomeMatcher parse(String spec) {
        if (spec == null || spec.isBlank()) return any();
        Set<String> set = new HashSet<>();
        boolean neg = false;
        for (String raw : spec.trim().split("\\s+")) {
            String s = raw;
            if (s.startsWith("!")) { neg = true; s = s.substring(1); }
            if (s.isEmpty()) continue;
            set.add(s.contains(":") ? s : "minecraft:" + s);
        }
        if (set.isEmpty()) return any();
        return new BiomeMatcher(set, neg, false);
    }

    public boolean matches(String biomeId) {
        if (any) return true;
        boolean present = names.contains(biomeId);
        return negated != present;
    }
}
