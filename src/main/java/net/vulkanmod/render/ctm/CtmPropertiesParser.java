package net.vulkanmod.render.ctm;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.vulkanmod.render.vertex.TerrainRenderType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public final class CtmPropertiesParser {
    private CtmPropertiesParser() {}

    public static CtmProperties parse(Properties p, ResourceLocation file) {
        CtmMethod method = CtmMethod.fromString(p.getProperty("method", "ctm"));
        if (method == CtmMethod.UNSUPPORTED) return null;

        String ns = file.getNamespace();
        String path = file.getPath();
        int slash = path.lastIndexOf('/');
        String dir = slash >= 0 ? path.substring(0, slash) : path;

        List<ResourceLocation> tileIds = new ArrayList<>();
        for (String tok : expandTiles(p.getProperty("tiles", ""))) {
            tileIds.add(ResourceLocation.parse(TilePath.resolve(tok, dir, ns)));
        }
        if (tileIds.isEmpty()) return null;

        Set<ResourceLocation> matchTiles = new HashSet<>();
        for (String t : split(p.getProperty("matchTiles"))) {
            matchTiles.add(resolveMatchTileId(t, ns));
        }
        Set<Block> matchBlocks = new HashSet<>();
        for (String b : split(p.getProperty("matchBlocks"))) {
            ResourceLocation id = ResourceLocation.parse(b.contains(":") ? b : "minecraft:" + b);
            Block blk = BuiltInRegistries.BLOCK.get(id);
            if (blk != null && blk != Blocks.AIR) matchBlocks.add(blk);
        }

        int[] weights = parseWeights(p.getProperty("weights"), tileIds.size());
        EnumSet<Direction> faces = parseFaces(p.getProperty("faces"));
        BiomeMatcher biomes = BiomeMatcher.parse(p.getProperty("biomes"));
        int minH = parseInt(p.getProperty("minHeight"), Integer.MIN_VALUE);
        int maxH = parseInt(p.getProperty("maxHeight"), Integer.MAX_VALUE);
        int tint = parseInt(p.getProperty("tintIndex"), -1);
        TerrainRenderType layer = TerrainRenderType.CUTOUT_MIPPED;

        return new CtmProperties(method, matchTiles, matchBlocks, tileIds, weights, faces,
                biomes, minH, maxH, tint, layer, dir);
    }

    private static List<String> expandTiles(String spec) {
        List<String> out = new ArrayList<>();
        for (String tok : split(spec)) {
            int dash = tok.indexOf('-');
            if (dash > 0 && tok.substring(0, dash).matches("\\d+") && tok.substring(dash + 1).matches("\\d+")) {
                int a = Integer.parseInt(tok.substring(0, dash));
                int b = Integer.parseInt(tok.substring(dash + 1));
                for (int i = a; i <= b; i++) out.add(Integer.toString(i));
            } else {
                out.add(tok);
            }
        }
        return out;
    }

    private static String[] split(String s) {
        if (s == null || s.isBlank()) return new String[0];
        return s.trim().split("\\s+");
    }

    private static int[] parseWeights(String s, int n) {
        String[] parts = split(s == null ? null : s.replace(":", " "));
        int[] w = new int[n];
        Arrays.fill(w, 1);
        for (int i = 0; i < Math.min(parts.length, n); i++) {
            w[i] = parseInt(parts[i], 1);
        }
        return w;
    }

    private static EnumSet<Direction> parseFaces(String s) {
        if (s == null || s.isBlank()) return EnumSet.allOf(Direction.class);
        EnumSet<Direction> set = EnumSet.noneOf(Direction.class);
        for (String f : split(s)) {
            switch (f.toLowerCase()) {
                case "all" -> set.addAll(EnumSet.allOf(Direction.class));
                case "sides" -> { set.add(Direction.NORTH); set.add(Direction.SOUTH); set.add(Direction.EAST); set.add(Direction.WEST); }
                case "top" -> set.add(Direction.UP);
                case "bottom" -> set.add(Direction.DOWN);
                case "north" -> set.add(Direction.NORTH);
                case "south" -> set.add(Direction.SOUTH);
                case "east" -> set.add(Direction.EAST);
                case "west" -> set.add(Direction.WEST);
                default -> {}
            }
        }
        return set.isEmpty() ? EnumSet.allOf(Direction.class) : set;
    }

    private static int parseInt(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }

    private static ResourceLocation resolveMatchTileId(String token, String ns) {
        String t = token.trim();
        if (t.endsWith(".png")) t = t.substring(0, t.length() - 4);
        if (t.contains(":")) return ResourceLocation.parse(t);
        if (t.contains("/")) return ResourceLocation.fromNamespaceAndPath(ns, t);
        return ResourceLocation.fromNamespaceAndPath(ns, "block/" + t);
    }
}
