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
}
