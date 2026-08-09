package net.vulkanmod.config.ui.mods;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class VisualFilter {
    private static final Set<String> STRONG = Set.of(
            "render", "rendering", "graphic", "visual", "shader", "texture", "sprite",
            "particle", "shadow", "culling", "cull", "fps", "framerate",
            "overlay", "hud", "gui", "color", "colour", "tint",
            "opacity", "transparency", "fog", "bloom", "blur");

    private static final Set<String> WEAK = Set.of(
            "animation", "model", "quality", "display", "light", "lighting", "scale", "distance");

    private VisualFilter() {
    }

    public static boolean isVisual(String path, String comment, String translationKey) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        if (strong(path) || strong(translationKey) || strong(comment)) {
            return true;
        }
        return weak(head(path)) || weak(head(translationKey));
    }

    public static List<String> tokens(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) {
            return tokens;
        }
        StringBuilder token = new StringBuilder();
        char previous = 0;
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            char next = index + 1 < text.length() ? text.charAt(index + 1) : 0;
            boolean boundary = !Character.isLetterOrDigit(current)
                    || (Character.isUpperCase(current) && Character.isLowerCase(previous))
                    || (Character.isUpperCase(current) && Character.isLowerCase(next))
                    || (Character.isDigit(current) != Character.isDigit(previous) && token.length() > 0);
            if (boundary && token.length() > 0) {
                tokens.add(token.toString().toLowerCase(Locale.ROOT));
                token.setLength(0);
            }
            if (Character.isLetterOrDigit(current)) {
                token.append(current);
            }
            previous = current;
        }
        if (token.length() > 0) {
            tokens.add(token.toString().toLowerCase(Locale.ROOT));
        }
        return tokens;
    }

    private static boolean strong(String text) {
        for (String token : tokens(text)) {
            if (matches(token, STRONG)) {
                return true;
            }
        }
        return false;
    }

    private static boolean weak(String token) {
        return matches(token, WEAK);
    }

    private static String head(String text) {
        if (text == null) {
            return "";
        }
        int separator = Math.max(text.lastIndexOf('.'), text.lastIndexOf('/'));
        List<String> tokens = tokens(text.substring(separator + 1));
        return tokens.isEmpty() ? "" : tokens.get(tokens.size() - 1);
    }

    private static boolean matches(String token, Set<String> keywords) {
        if (token.isEmpty()) {
            return false;
        }
        return keywords.contains(token)
                || (token.endsWith("s") && keywords.contains(token.substring(0, token.length() - 1)));
    }
}
