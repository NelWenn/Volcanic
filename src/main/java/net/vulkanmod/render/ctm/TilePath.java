package net.vulkanmod.render.ctm;

public final class TilePath {
    private TilePath() {}

    public static String resolve(String token, String propertiesDirPath, String namespace) {
        String t = token.trim();
        if (t.endsWith(".png")) t = t.substring(0, t.length() - 4);
        int colon = t.indexOf(':');
        if (colon > 0) return t;
        if (t.startsWith("~/")) return namespace + ":optifine/" + t.substring(2);
        if (t.matches("\\d+")) return namespace + ":" + propertiesDirPath + "/" + t;
        if (t.startsWith("./")) t = t.substring(2);
        return namespace + ":" + propertiesDirPath + "/" + t;
    }
}
