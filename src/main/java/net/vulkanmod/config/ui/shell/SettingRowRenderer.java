package net.vulkanmod.config.ui.shell;

import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.vulkanmod.config.ui.core.ColorToken;
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

    private static final int PILL_WIDTH = 22;
    private static final int PILL_HEIGHT = 12;
    private static final int KNOB_INSET = 2;

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
    private static final String[] STAR = {
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
                       boolean favorite, boolean starHovered) {
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

        ShellRenderer.paintRoundedFill(painter, card, SettingRowLayout.CARD_RADIUS, cardArgb(highlighted));
        ShellRenderer.paintRoundedOutline(painter, card, SettingRowLayout.CARD_RADIUS, borderArgb(highlighted));

        String title = title(meta, reset);
        painter.text(card.x() + ShellRenderer.CARD_PAD_X, textTop(card), title,
                titleArgb(enabled, highlighted), false);
        paintBadges(painter, box, meta, font.width(title), enabled);

        if (reset) {
            paintReset(painter, SettingRowLayout.resetBox(box), resetHovered);
        }
        paintStar(painter, SettingRowLayout.starBox(box), favorite, starHovered);

        int right = card.right() - ShellRenderer.CARD_PAD_X;
        switch (meta.type()) {
            case BOOL -> paintPill(painter, card, right, booleanValue(meta, value));
            case INT -> paintSlider(painter, font, box, card, right, binding.display(value),
                    intValue(meta, value), binding.min(), binding.max(), enabled, highlighted);
            case ENUM -> paintCycler(painter, font, card, right, I18n.get(binding.display(value)),
                    enabled, highlighted);
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

    private void paintStar(SurfacePainter painter, Rect box, boolean favorite, boolean hovered) {
        if (box.isEmpty()) {
            return;
        }
        if (hovered) {
            ShellRenderer.paintRoundedFill(painter, box, SettingRowLayout.RESET_RADIUS,
                    theme.color(ColorToken.SURFACE_NAV_ACTIVE));
        }
        paintGlyph(painter, box.inset(STAR_INSET), STAR, theme.color(starToken(favorite, hovered)), favorite);
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

    private void paintPill(SurfacePainter painter, Rect box, int right, boolean on) {
        Rect pill = new Rect(right - PILL_WIDTH, box.y() + (box.height() - PILL_HEIGHT) / 2,
                PILL_WIDTH, PILL_HEIGHT);
        ShellRenderer.paintRoundedFill(painter, pill, PILL_HEIGHT / 2,
                theme.color(on ? ColorToken.SUCCESS : ColorToken.BORDER_DEFAULT));

        int knob = PILL_HEIGHT - KNOB_INSET * 2;
        int knobX = on ? pill.right() - KNOB_INSET - knob : pill.x() + KNOB_INSET;
        ShellRenderer.paintRoundedFill(painter, new Rect(knobX, pill.y() + KNOB_INSET, knob, knob), knob / 2,
                theme.color(on ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_MUTED));
    }

    private void paintSlider(SurfacePainter painter, Font font, Rect row, Rect card, int right, String text,
                             int value, int min, int max, boolean enabled, float active) {
        Rect zone = max > min ? ShellRenderer.sliderTrack(row) : Rect.EMPTY;
        paintValue(painter, font, card, zone.isEmpty() ? right : zone.x() - TRACK_GAP, text, valueArgb(enabled));
        if (zone.isEmpty()) {
            return;
        }

        Rect track = new Rect(zone.x(), zone.y() + (zone.height() - TRACK_HEIGHT) / 2,
                zone.width(), TRACK_HEIGHT);
        painter.fill(track, theme.color(ColorToken.BORDER_DEFAULT));
        int filled = SettingRowLayout.trackFill(track.width(), value, min, max);
        if (filled > 0) {
            painter.fill(new Rect(track.x(), track.y(), filled, TRACK_HEIGHT), theme.color(ColorToken.ACCENT));
        }

        ShellRenderer.paintRoundedFill(painter,
                SliderGeometry.knob(zone, value, min, max, SliderGeometry.KNOB_WIDTH),
                KNOB_RADIUS, knobArgb(enabled, active));
    }

    private int knobArgb(boolean enabled, float active) {
        if (!enabled) {
            return theme.color(ColorToken.TEXT_FAINT);
        }
        return blend(ColorToken.TEXT_MUTED, ColorToken.ACCENT, active);
    }

    private void paintCycler(SurfacePainter painter, Font font, Rect box, int right, String text,
                             boolean enabled, float hovered) {
        int argb = arrowArgb(enabled, hovered);
        painter.text(right - font.width(ARROW_RIGHT), textTop(box), ARROW_RIGHT, argb, false);

        int valueRight = right - font.width(ARROW_RIGHT) - ARROW_GAP;
        painter.text(valueRight - font.width(text), textTop(box), text, valueArgb(enabled), false);

        int leftArrowX = valueRight - font.width(text) - ARROW_GAP - font.width(ARROW_LEFT);
        painter.text(leftArrowX, textTop(box), ARROW_LEFT, argb, false);
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
