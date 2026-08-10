package net.vulkanmod.config.ui.shell;

import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.vulkanmod.config.ui.core.ColorToken;
import net.vulkanmod.config.ui.core.Glide;
import net.vulkanmod.config.ui.core.Motion;
import net.vulkanmod.config.ui.core.Rect;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingRowLayout;
import net.vulkanmod.config.ui.core.SliderGeometry;
import net.vulkanmod.config.ui.core.Theme;
import net.vulkanmod.config.ui.render.SurfacePainter;
import net.vulkanmod.config.ui.settings.SettingBinding;

public final class SettingRowRenderer {
    private static final int ARROW_GAP = 6;
    private static final int GLYPH_INSET = 4;

    private static final int TEXT_HEIGHT = 9;
    private static final int GLYPH_HEIGHT = 8;

    private static final int PILL_WIDTH = 22;
    private static final int PILL_HEIGHT = 12;
    private static final int KNOB_INSET = 2;
    private final net.vulkanmod.config.ui.core.HoverState pills =
            new net.vulkanmod.config.ui.core.HoverState(Motion.SELECTION_MS, true);
    private final java.util.Map<String, Glide> fills = new java.util.HashMap<>();
    private final java.util.Set<String> seen = new java.util.HashSet<>();
    private final net.vulkanmod.config.ui.core.HoverState stars =
            new net.vulkanmod.config.ui.core.HoverState(Motion.SELECTION_MS, true);
    private final java.util.Map<String, String> shown = new java.util.HashMap<>();
    private final java.util.Map<String, Long> struck = new java.util.HashMap<>();

    private static final int TRACK_HEIGHT = 3;
    private static final int TRACK_GAP = 8;
    private static final int KNOB_RADIUS = 2;

    private static final String ARROW_LEFT = "\u2039";
    private static final String ARROW_RIGHT = "\u203A";
    private static final String ITALIC = "\u00A7o";

    static final String[] FLASK = {
            "..###..",
            "...#...",
            "...#...",
            "..###..",
            ".#####.",
            "#######",
            "#######"};

    static final String[] CHECK = {
            "......#",
            ".....##",
            "#...##.",
            "##.##..",
            ".###...",
            "..#....",
            "......."};

    private static final int STAR_INSET = 3;
    static final String[] STAR = {
            "....#....",
            "...###...",
            "...###...",
            "#########",
            ".#######.",
            "..#####..",
            "..#####..",
            ".##...##.",
            "##.....##"};

    private final Theme theme;

    public SettingRowRenderer(Theme theme) {
        if (theme == null) {
            throw new IllegalArgumentException("theme must not be null");
        }
        this.theme = theme;
    }

    public void render(SurfacePainter painter, Font font, Rect box, SettingMeta meta, SettingBinding binding,
                       Object value, boolean enabled, float hovered, boolean resettable, boolean resetHovered,
                       boolean favorite, boolean starHovered, boolean prevHovered, boolean nextHovered) {
        render(painter, font, box, meta, binding, value, enabled, hovered, resettable, resetHovered,
                favorite, starHovered, prevHovered, nextHovered, 0L);
    }

    public void endFrame() {
        this.pills.endFrame();
        this.stars.endFrame();
        this.fills.keySet().retainAll(seen);
        this.shown.keySet().retainAll(seen);
        this.struck.keySet().retainAll(seen);
        this.seen.clear();
    }

    private float strike(String key, String display, long deltaMs) {
        seen.add(key);
        String previous = shown.put(key, display);
        if (previous != null && !previous.equals(display)) {
            struck.put(key, (long) Motion.SELECTION_MS);
        }
        long left = struck.getOrDefault(key, 0L) - deltaMs;
        if (left <= 0L) {
            struck.remove(key);
            return 0.0f;
        }
        struck.put(key, left);
        return Motion.ease(left, Motion.SELECTION_MS);
    }

    public void render(SurfacePainter painter, Font font, Rect box, SettingMeta meta, SettingBinding binding,
                       Object value, boolean enabled, float hovered, boolean resettable, boolean resetHovered,
                       boolean favorite, boolean starHovered, boolean prevHovered, boolean nextHovered,
                       long deltaMs) {
        if (painter == null) {
            throw new IllegalArgumentException("painter must not be null");
        }
        if (font == null) {
            throw new IllegalArgumentException("font must not be null");
        }
        if (box == null) {
            throw new IllegalArgumentException("box must not be null");
        }
        if (meta == null) {
            throw new IllegalArgumentException("meta must not be null");
        }
        if (binding == null) {
            throw new IllegalArgumentException("binding must not be null for setting " + meta.id());
        }
        if (value == null) {
            throw new IllegalArgumentException("value must not be null for setting " + meta.id());
        }
        if (hovered < 0.0f || hovered > 1.0f) {
            throw new IllegalArgumentException("hovered must be within 0..1: " + hovered);
        }
        if (box.isEmpty()) {
            return;
        }

        Rect card = SettingRowLayout.cardBox(box);
        if (card.isEmpty()) {
            return;
        }

        float highlighted = enabled ? hovered : 0.0f;
        boolean reset = resettable && enabled;

        String key = meta.id().toString();
        float struckNow = strike(key, binding.display(value), deltaMs);
        ShellRenderer.paintRoundedFill(painter, card, SettingRowLayout.CARD_RADIUS, cardArgb(highlighted));
        if (struckNow > 0.0f) {
            ShellRenderer.paintRoundedFill(painter, card, SettingRowLayout.CARD_RADIUS,
                    theme.color(ColorToken.ACCENT, struckNow * 0.16f));
        }
        ShellRenderer.paintRoundedOutline(painter, card, SettingRowLayout.CARD_RADIUS,
                struckNow > 0.0f
                        ? Motion.blend(borderArgb(highlighted), theme.color(ColorToken.ACCENT), struckNow)
                        : borderArgb(highlighted));

        String title = title(meta, reset);
        painter.text(card.x() + ShellRenderer.CARD_PAD_X, textTop(card), title,
                titleArgb(enabled, highlighted), false);
        paintBadges(painter, box, meta, font.width(title), enabled);

        if (reset) {
            paintReset(painter, SettingRowLayout.resetBox(box), resetHovered);
        }
        paintStar(painter, SettingRowLayout.starBox(box), favorite, starHovered,
                stars.advance(key, favorite, deltaMs));

        int right = card.right() - ShellRenderer.CARD_PAD_X;
        switch (meta.type()) {
            case BOOL -> paintPill(painter, card, right, booleanValue(meta, value),
                    meta.id().toString(), deltaMs);
            case INT -> paintSlider(painter, font, box, card, right, binding.display(value),
                    intValue(meta, value), binding.min(), binding.max(), enabled, highlighted,
                    meta.id().toString(), deltaMs);
            case ENUM -> paintCycler(painter, font, box, I18n.get(binding.display(value)),
                    enabled, highlighted, prevHovered, nextHovered);
        }
    }

    private static String title(SettingMeta meta, boolean modified) {
        String text = I18n.get(meta.titleKey());
        return modified ? ITALIC + text : text;
    }

    private void paintBadges(SurfacePainter painter, Rect row, SettingMeta meta, int titleWidth, boolean enabled) {
        int offset = titleWidth;
        if (meta.experimental()) {
            paintGlyph(painter, SettingRowLayout.badgeBox(row, offset), FLASK,
                    badgeArgb(ColorToken.WARNING, enabled), true);
            offset += SettingRowLayout.BADGE_ADVANCE;
        }
        if (meta.recommended()) {
            paintGlyph(painter, SettingRowLayout.badgeBox(row, offset), CHECK,
                    badgeArgb(ColorToken.SUCCESS, enabled), true);
        }
    }

    private int badgeArgb(ColorToken token, boolean enabled) {
        return theme.color(enabled ? token : ColorToken.TEXT_FAINT);
    }

    private void paintReset(SurfacePainter painter, Rect box, boolean hovered) {
        if (box.isEmpty()) {
            return;
        }
        ShellRenderer.paintRoundedFill(painter, box, SettingRowLayout.RESET_RADIUS,
                theme.color(hovered ? ColorToken.SURFACE_NAV_ACTIVE : ColorToken.SURFACE_CARD));
        ShellRenderer.paintRoundedOutline(painter, box, SettingRowLayout.RESET_RADIUS,
                theme.color(hovered ? ColorToken.ACCENT : ColorToken.BORDER_STRONG));
        paintUndoArrow(painter, box.inset(GLYPH_INSET),
                theme.color(hovered ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_SECONDARY));
    }

    private static void paintUndoArrow(SurfacePainter painter, Rect glyph, int argb) {
        if (glyph.isEmpty()) {
            return;
        }
        int thickness = Math.max(1, glyph.height() / 3);
        int head = Math.min(Math.max(2, glyph.width() / 2), glyph.width() - 1);
        int top = glyph.y() + (glyph.height() - thickness) / 2;

        painter.fill(new Rect(glyph.x() + head, top, glyph.width() - head, thickness), argb);
        for (int step = 0; step < head; step++) {
            painter.fill(new Rect(glyph.x() + step, top - step, 1, thickness + step * 2), argb);
        }
    }

    private void paintStar(SurfacePainter painter, Rect box, boolean favorite, boolean hovered, float lit) {
        if (box.isEmpty()) {
            return;
        }
        if (hovered) {
            ShellRenderer.paintRoundedFill(painter, box, SettingRowLayout.RESET_RADIUS,
                    theme.color(ColorToken.SURFACE_NAV_ACTIVE));
        }
        int inset = STAR_INSET - Math.round(lit * (1.0f - lit) * 4.0f);
        int argb = Motion.blend(theme.color(starToken(false, hovered)),
                theme.color(ColorToken.ACCENT), lit);
        paintGlyph(painter, box.inset(Math.max(0, inset)), STAR, argb, favorite);
    }

    private static ColorToken starToken(boolean favorite, boolean hovered) {
        if (favorite) {
            return ColorToken.ACCENT;
        }
        return hovered ? ColorToken.TEXT_SECONDARY : ColorToken.TEXT_MUTED;
    }

    static void paintGlyph(SurfacePainter painter, Rect glyph, String[] pattern, int argb, boolean filled) {
        if (glyph.isEmpty()) {
            return;
        }
        int scale = Math.max(1, Math.min(glyph.width(), glyph.height()) / pattern.length);
        int left = glyph.x() + (glyph.width() - pattern.length * scale) / 2;
        int top = glyph.y() + (glyph.height() - pattern.length * scale) / 2;
        for (int row = 0; row < pattern.length; row++) {
            for (int column = 0; column < pattern[row].length(); column++) {
                if (lit(pattern, row, column) && (filled || !enclosed(pattern, row, column))) {
                    painter.fill(new Rect(left + column * scale, top + row * scale, scale, scale), argb);
                }
            }
        }
    }

    private static boolean lit(String[] pattern, int row, int column) {
        return row >= 0 && row < pattern.length
                && column >= 0 && column < pattern[row].length() && pattern[row].charAt(column) == '#';
    }

    private static boolean enclosed(String[] pattern, int row, int column) {
        return lit(pattern, row - 1, column) && lit(pattern, row + 1, column)
                && lit(pattern, row, column - 1) && lit(pattern, row, column + 1);
    }

    private void paintPill(SurfacePainter painter, Rect box, int right, boolean on, String key, long deltaMs) {
        Rect pill = new Rect(right - PILL_WIDTH, box.y() + (box.height() - PILL_HEIGHT) / 2,
                PILL_WIDTH, PILL_HEIGHT);
        float t = pills.advance(key, on, deltaMs);
        ShellRenderer.paintRoundedFill(painter, pill, PILL_HEIGHT / 2,
                Motion.blend(theme.color(ColorToken.BORDER_DEFAULT), theme.color(ColorToken.SUCCESS), t));

        int knob = PILL_HEIGHT - KNOB_INSET * 2;
        int travel = PILL_WIDTH - KNOB_INSET * 2 - knob;
        int knobX = pill.x() + KNOB_INSET + Math.round(travel * t);
        ShellRenderer.paintRoundedFill(painter, new Rect(knobX, pill.y() + KNOB_INSET, knob, knob), knob / 2,
                Motion.blend(theme.color(ColorToken.TEXT_MUTED), theme.color(ColorToken.TEXT_PRIMARY), t));
    }

    private void paintSlider(SurfacePainter painter, Font font, Rect row, Rect card, int right, String text,
                             int value, int min, int max, boolean enabled, float active,
                             String key, long deltaMs) {
        Rect zone = max > min ? ShellRenderer.sliderTrack(row) : Rect.EMPTY;
        paintValue(painter, font, card, zone.isEmpty() ? right : zone.x() - TRACK_GAP, text, valueArgb(enabled));
        if (zone.isEmpty()) {
            return;
        }

        Rect track = new Rect(zone.x(), zone.y() + (zone.height() - TRACK_HEIGHT) / 2,
                zone.width(), TRACK_HEIGHT);
        painter.fill(track, theme.color(ColorToken.BORDER_DEFAULT));

        Glide glide = fills.computeIfAbsent(key, name -> new Glide(55.0f));
        int target = SettingRowLayout.trackFill(track.width(), value, min, max);
        int filled = Math.round(glide.advance(target, deltaMs));
        seen.add(key);
        if (filled > 0) {
            painter.fill(new Rect(track.x(), track.y(), Math.min(filled, track.width()), TRACK_HEIGHT),
                    theme.color(ColorToken.ACCENT));
        }

        Rect knob = SliderGeometry.knob(zone, value, min, max, SliderGeometry.KNOB_WIDTH);
        ShellRenderer.paintRoundedFill(painter,
                knob.translated(filled - target, 0), KNOB_RADIUS, knobArgb(enabled, active));
    }

    private int knobArgb(boolean enabled, float active) {
        if (!enabled) {
            return theme.color(ColorToken.TEXT_FAINT);
        }
        return blend(ColorToken.TEXT_MUTED, ColorToken.ACCENT, active);
    }

    private void paintCycler(SurfacePainter painter, Font font, Rect row, String text,
                             boolean enabled, float hovered, boolean prevHovered, boolean nextHovered) {
        Rect prev = SettingRowLayout.cyclerPrevBox(row);
        Rect value = SettingRowLayout.cyclerValueBox(row);
        Rect next = SettingRowLayout.cyclerNextBox(row);
        if (prev.isEmpty()) {
            paintValue(painter, font, row, SettingRowLayout.cardBox(row).right() - SettingRowLayout.CARD_PAD_X,
                    text, valueArgb(enabled));
            return;
        }

        paintArrow(painter, font, prev, ARROW_LEFT, enabled, hovered, prevHovered);
        paintArrow(painter, font, next, ARROW_RIGHT, enabled, hovered, nextHovered);

        String shown = trimmed(font, text, value.width() - ARROW_GAP * 2);
        painter.text(value.x() + (value.width() - font.width(shown)) / 2, textTop(value), shown,
                valueArgb(enabled), false);
    }

    private void paintArrow(SurfacePainter painter, Font font, Rect box, String glyph,
                            boolean enabled, float rowHovered, boolean pointed) {
        if (box.isEmpty()) {
            return;
        }
        if (enabled && pointed) {
            ShellRenderer.paintRoundedFill(painter, box, SettingRowLayout.ARROW_RADIUS,
                    theme.color(ColorToken.SURFACE_NAV_ACTIVE));
            ShellRenderer.paintRoundedOutline(painter, box, SettingRowLayout.ARROW_RADIUS,
                    theme.color(ColorToken.ACCENT));
        }
        int ink = Math.max(1, font.width(glyph) - 1);
        painter.text(box.x() + (box.width() - ink) / 2, box.y() + (box.height() - GLYPH_HEIGHT) / 2, glyph,
                arrowArgb(enabled, pointed ? 1.0f : rowHovered), false);
    }

    private static String trimmed(Font font, String text, int width) {
        if (width <= 0 || font.width(text) <= width) {
            return text;
        }
        String cut = text;
        while (!cut.isEmpty() && font.width(cut + "…") > width) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut.isEmpty() ? text : cut + "…";
    }

    private void paintValue(SurfacePainter painter, Font font, Rect box, int right, String text, int argb) {
        painter.text(right - font.width(text), textTop(box), text, argb, false);
    }

    private int titleArgb(boolean enabled, float hovered) {
        if (!enabled) {
            return theme.color(ColorToken.TEXT_FAINT);
        }
        return blend(ColorToken.TEXT_DEFAULT, ColorToken.TEXT_PRIMARY, hovered);
    }

    private int valueArgb(boolean enabled) {
        return theme.color(enabled ? ColorToken.TEXT_SECONDARY : ColorToken.TEXT_FAINT);
    }

    private int arrowArgb(boolean enabled, float hovered) {
        if (!enabled) {
            return theme.color(ColorToken.TEXT_FAINT);
        }
        return blend(ColorToken.TEXT_MUTED, ColorToken.TEXT_SECONDARY, hovered);
    }

    private int cardArgb(float hovered) {
        return blend(ColorToken.SURFACE_CARD, ColorToken.SURFACE_CARD_HOVER, hovered);
    }

    private int borderArgb(float hovered) {
        return blend(ColorToken.BORDER_SUBTLE, ColorToken.BORDER_STRONG, hovered);
    }

    private int blend(ColorToken idle, ColorToken active, float progress) {
        return ShellRenderer.lerpArgb(theme.color(idle), theme.color(active), progress);
    }

    private static int textTop(Rect box) {
        return box.y() + (box.height() - TEXT_HEIGHT) / 2;
    }

    private static boolean booleanValue(SettingMeta meta, Object value) {
        if (!(value instanceof Boolean flag)) {
            throw new IllegalArgumentException("setting " + meta.id() + " is BOOL but its value is " + value);
        }
        return flag;
    }

    private static int intValue(SettingMeta meta, Object value) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("setting " + meta.id() + " is " + meta.type()
                    + " but its value is " + value);
        }
        return number.intValue();
    }
}
