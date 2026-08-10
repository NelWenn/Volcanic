package net.vulkanmod.config.ui.shell;

import java.util.Map;

public final class PresetIcons {
    public static final int SIZE = 9;

    private static final String[] PERFORMANCE = {
            "....###..",
            "...###...",
            "..###....",
            ".####....",
            "########.",
            "...####..",
            "...###...",
            "..###....",
            ".###....."};

    private static final String[] BALANCED = {
            "....#....",
            "#########",
            ".#..#..#.",
            ".#..#..#.",
            "###.#.###",
            "....#....",
            "....#....",
            "...###...",
            "..#####.."};

    private static final String[] QUALITY = {
            "..#####..",
            ".#.....#.",
            "#....##.#",
            "#.....#.#",
            "#.......#",
            ".#.....#.",
            "..#...#..",
            "...#.#...",
            "....#...."};

    private static final String[] ULTRA = {
            "....#....",
            ".#..#..#.",
            "..#.#.#..",
            "...###...",
            "#########",
            "...###...",
            "..#.#.#..",
            ".#..#..#.",
            "....#...."};

    private static final String[] CUSTOM = {
            ".#..#..#.",
            "###.#..#.",
            ".#..#..#.",
            ".#..#..#.",
            ".#.###.#.",
            ".#..#..#.",
            ".#..#..#.",
            ".#..#.###",
            ".#..#..#."};

    private static final Map<String, Integer> TONES = Map.of(
            "vulkanmod.options.performancePreset.performance", 0xFFFFB020,
            "vulkanmod.options.performancePreset.balanced", 0xFFFF7A2A,
            "vulkanmod.options.performancePreset.quality", 0xFFFF5A1F,
            "vulkanmod.options.performancePreset.ultra", 0xFFFF2E00,
            "vulkanmod.options.performancePreset.custom", 0xFFA3918A);

    private static final Map<String, String> TIERS = Map.of(
            "vulkanmod.options.performancePreset.performance", "TIER I",
            "vulkanmod.options.performancePreset.balanced", "TIER II",
            "vulkanmod.options.performancePreset.quality", "TIER III",
            "vulkanmod.options.performancePreset.ultra", "TIER IV",
            "vulkanmod.options.performancePreset.custom", "OWN MIX");

    private static final Map<String, String[]> BY_KEY = Map.of(
            "vulkanmod.options.performancePreset.performance", PERFORMANCE,
            "vulkanmod.options.performancePreset.balanced", BALANCED,
            "vulkanmod.options.performancePreset.quality", QUALITY,
            "vulkanmod.options.performancePreset.ultra", ULTRA,
            "vulkanmod.options.performancePreset.custom", CUSTOM);

    private PresetIcons() {
    }

    public static String[] of(String presetKey) {
        return presetKey == null ? null : BY_KEY.get(presetKey);
    }

    public static int tone(String presetKey) {
        Integer tone = presetKey == null ? null : TONES.get(presetKey);
        return tone == null ? 0xFFA3918A : tone;
    }

    public static String tier(String presetKey) {
        String tier = presetKey == null ? null : TIERS.get(presetKey);
        return tier == null ? "OWN MIX" : tier;
    }
}
