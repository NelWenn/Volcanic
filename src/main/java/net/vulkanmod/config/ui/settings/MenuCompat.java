package net.vulkanmod.config.ui.settings;

import net.minecraft.client.resources.language.I18n;
import net.neoforged.fml.ModList;
import net.vulkanmod.compat.CompatCategory;
import net.vulkanmod.compat.CompatDetector;
import net.vulkanmod.compat.CompatMode;
import net.vulkanmod.compat.CompatMods;
import net.vulkanmod.compat.CompatPolicyManager;
import net.vulkanmod.compat.path.RenderPath;
import net.vulkanmod.compat.path.RenderPathOwnership;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MenuCompat {
    public enum Tone {
        GOOD,
        NOTE,
        WARN
    }

    public record Entry(String name, String note, String state, Tone tone) {
    }

    public record Section(String key, List<Entry> entries) {
        public Section {
            entries = List.copyOf(entries);
        }
    }

    private static final String UNKNOWN_VERSION = "UNKNOWN";
    private static final String[][] ADJUSTMENTS = {
            {"emi", "vulkanmod.ui.compat.tweak.emi", "vulkanmod.ui.compat.state.bypassed"},
            {"flywheel", "vulkanmod.ui.compat.tweak.flywheel", "vulkanmod.ui.compat.state.bypassed"},
            {"veil", "vulkanmod.ui.compat.tweak.veil", "vulkanmod.ui.compat.state.bypassed"},
            {"polytone", "vulkanmod.ui.compat.tweak.polytone", "vulkanmod.ui.compat.state.linked"}
    };

    private static List<Section> cached;

    private MenuCompat() {
    }

    public static void invalidate() {
        cached = null;
    }

    public static List<Section> sections() {
        if (cached == null) {
            cached = build();
        }
        return cached;
    }

    public static List<Integer> counts() {
        List<Section> sections = sections();
        List<Integer> counts = new ArrayList<>(sections.size());
        for (Section section : sections) {
            counts.add(section.entries().size());
        }
        return counts;
    }

    private static List<Section> build() {
        List<Section> sections = new ArrayList<>(3);
        add(sections, "vulkanmod.ui.compat.mods", detectedMods());
        add(sections, "vulkanmod.ui.compat.paths", claimedPaths());
        add(sections, "vulkanmod.ui.compat.tweaks", adjustments());
        return List.copyOf(sections);
    }

    private static void add(List<Section> sections, String key, List<Entry> entries) {
        if (!entries.isEmpty()) {
            sections.add(new Section(key, entries));
        }
    }

    private static List<Entry> detectedMods() {
        List<Entry> entries = new ArrayList<>();
        try {
            for (String modId : CompatMods.REPORT_MOD_IDS) {
                if (!CompatDetector.isModLoaded(modId)) {
                    continue;
                }
                CompatMode mode = CompatPolicyManager.getCompatMode(modId);
                entries.add(new Entry(modName(modId),
                        categoryLabel(CompatPolicyManager.getCompatCategory(modId))
                                + versionSuffix(CompatDetector.getModVersion(modId)),
                        modeLabel(mode),
                        mode == CompatMode.INCOMPATIBLE ? Tone.WARN : Tone.GOOD));
            }
        } catch (Throwable t) {
            return List.of();
        }
        return entries;
    }

    private static List<Entry> claimedPaths() {
        List<Entry> entries = new ArrayList<>();
        try {
            for (RenderPath path : RenderPath.values()) {
                if (!RenderPathOwnership.isPathActive(path)) {
                    continue;
                }
                entries.add(new Entry(
                        I18n.get("vulkanmod.ui.compat.path." + path.name().toLowerCase(Locale.ROOT)),
                        modName(RenderPathOwnership.getPathOwner(path)),
                        modeLabel(RenderPathOwnership.getPathMode(path)),
                        Tone.NOTE));
            }
        } catch (Throwable t) {
            return List.of();
        }
        return entries;
    }

    private static List<Entry> adjustments() {
        List<Entry> entries = new ArrayList<>();
        try {
            for (String[] tweak : ADJUSTMENTS) {
                if (!CompatDetector.isModLoaded(tweak[0])) {
                    continue;
                }
                boolean linked = tweak[2].endsWith("linked");
                entries.add(new Entry(modName(tweak[0]), I18n.get(tweak[1]), I18n.get(tweak[2]),
                        linked ? Tone.GOOD : Tone.NOTE));
            }
        } catch (Throwable t) {
            return List.of();
        }
        return entries;
    }

    private static String categoryLabel(CompatCategory category) {
        return I18n.get("vulkanmod.ui.compat.category." + category.name().toLowerCase(Locale.ROOT));
    }

    private static String modeLabel(CompatMode mode) {
        return I18n.get("vulkanmod.ui.compat.mode." + mode.name().toLowerCase(Locale.ROOT));
    }

    private static String versionSuffix(String version) {
        return version == null || version.isBlank() || UNKNOWN_VERSION.equals(version)
                ? "" : " · " + version;
    }

    private static String modName(String modId) {
        try {
            return ModList.get().getModContainerById(modId)
                    .map(container -> container.getModInfo().getDisplayName())
                    .filter(name -> !name.isBlank())
                    .orElse(modId);
        } catch (Throwable t) {
            return modId;
        }
    }
}
