package net.vulkanmod.config.ui.shell;

import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.vulkanmod.config.ui.core.ColorToken;
import net.vulkanmod.config.ui.core.Rect;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingRowLayout;
import net.vulkanmod.config.ui.core.Theme;
import net.vulkanmod.config.ui.render.SurfacePainter;

public final class SettingRowRenderer {
    private static final int ARROW_GAP = 6;
    private static final int GLYPH_INSET = 4;

    private static final int PAD_X = 12;
    private static final int TEXT_HEIGHT = 9;

    private static final int PILL_WIDTH = 22;
    private static final int PILL_HEIGHT = 12;
    private static final int KNOB_INSET = 2;

    private static final int TRACK_WIDTH = 56;
    private static final int TRACK_HEIGHT = 3;
    private static final int TRACK_GAP = 8;

    private static final String ARROW_LEFT = "\u2039";
    private static final String ARROW_RIGHT = "\u203A";

    private final Theme theme;

    public SettingRowRenderer(Theme theme) {
        if (theme == null) {
            throw new IllegalArgumentException("theme must not be null");
        }
        this.theme = theme;
    }

    public void render(SurfacePainter painter, Font font, Rect box, SettingMeta meta, Object value,
                       boolean hovered, boolean resettable, boolean resetHovered, int min, int max) {
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
        if (value == null) {
            throw new IllegalArgumentException("value must not be null for setting " + meta.id());
        }
        if (box.isEmpty()) {
            return;
        }

        Rect card = SettingRowLayout.cardBox(box);
        if (card.isEmpty()) {
            return;
        }

        ShellRenderer.paintRoundedFill(painter, card, SettingRowLayout.CARD_RADIUS, cardArgb(hovered));
        ShellRenderer.paintRoundedOutline(painter, card, SettingRowLayout.CARD_RADIUS, borderArgb(hovered));

        painter.text(card.x() + PAD_X, textTop(card), I18n.get(meta.titleKey()),
                theme.color(hovered ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_DEFAULT), false);

        if (resettable) {
            paintReset(painter, SettingRowLayout.resetBox(box), resetHovered);
        }

        int right = card.right() - PAD_X;
        switch (meta.type()) {
            case BOOL -> paintPill(painter, card, right, booleanValue(meta, value));
            case INT -> paintTrack(painter, font, card, right, intValue(meta, value), min, max);
            case ENUM -> paintCycler(painter, font, card, right, I18n.get(value.toString()), hovered);
        }
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

    private void paintTrack(SurfacePainter painter, Font font, Rect box, int right, int value, int min, int max) {
        int valueX = paintValue(painter, font, box, right, String.valueOf(value));
        if (max <= min) {
            return;
        }

        Rect track = new Rect(valueX - TRACK_GAP - TRACK_WIDTH, box.y() + (box.height() - TRACK_HEIGHT) / 2,
                TRACK_WIDTH, TRACK_HEIGHT);
        if (track.x() <= box.x() + PAD_X) {
            return;
        }

        painter.fill(track, theme.color(ColorToken.BORDER_DEFAULT));
        int filled = SettingRowLayout.trackFill(TRACK_WIDTH, value, min, max);
        if (filled > 0) {
            painter.fill(new Rect(track.x(), track.y(), filled, TRACK_HEIGHT), theme.color(ColorToken.ACCENT));
        }
    }

    private void paintCycler(SurfacePainter painter, Font font, Rect box, int right, String text, boolean hovered) {
        int argb = theme.color(hovered ? ColorToken.TEXT_SECONDARY : ColorToken.TEXT_MUTED);
        painter.text(right - font.width(ARROW_RIGHT), textTop(box), ARROW_RIGHT, argb, false);

        int valueRight = right - font.width(ARROW_RIGHT) - ARROW_GAP;
        painter.text(valueRight - font.width(text), textTop(box), text,
                theme.color(ColorToken.TEXT_SECONDARY), false);

        int leftArrowX = valueRight - font.width(text) - ARROW_GAP - font.width(ARROW_LEFT);
        painter.text(leftArrowX, textTop(box), ARROW_LEFT, argb, false);
    }

    private int paintValue(SurfacePainter painter, Font font, Rect box, int right, String text) {
        int x = right - font.width(text);
        painter.text(x, textTop(box), text, theme.color(ColorToken.TEXT_SECONDARY), false);
        return x;
    }

    private int cardArgb(boolean hovered) {
        return theme.color(hovered ? ColorToken.SURFACE_CARD_HOVER : ColorToken.SURFACE_CARD);
    }

    private int borderArgb(boolean hovered) {
        return theme.color(hovered ? ColorToken.BORDER_STRONG : ColorToken.BORDER_SUBTLE);
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
            throw new IllegalArgumentException("setting " + meta.id() + " is INT but its value is " + value);
        }
        return number.intValue();
    }
}
