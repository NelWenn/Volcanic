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
    private static final int CARD_RADIUS = 6;
    private static final int GLOW_RADIUS = 8;
    private static final float HOVER_GLOW_ALPHA = 0.60f;
    private static final float CHANGED_GLOW_ALPHA = 0.45f;

    private static final int PAD_X = 12;
    private static final int TEXT_HEIGHT = 9;

    private static final int PILL_WIDTH = 22;
    private static final int PILL_HEIGHT = 12;
    private static final int KNOB_INSET = 2;

    private static final int TRACK_WIDTH = 56;
    private static final int TRACK_HEIGHT = 3;
    private static final int TRACK_GAP = 8;

    private final Theme theme;

    public SettingRowRenderer(Theme theme) {
        if (theme == null) {
            throw new IllegalArgumentException("theme must not be null");
        }
        this.theme = theme;
    }

    public void render(SurfacePainter painter, Font font, Rect box, SettingMeta meta, Object value,
                       boolean hovered, boolean changed) {
        render(painter, font, box, meta, value, hovered, changed, 0, 0);
    }

    public void render(SurfacePainter painter, Font font, Rect box, SettingMeta meta, Object value,
                       boolean hovered, boolean changed, int min, int max) {
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

        painter.surface(box, CARD_RADIUS, cardArgb(hovered), borderArgb(hovered, changed),
                glowArgb(hovered, changed), hovered || changed ? GLOW_RADIUS : 0);

        painter.text(box.x() + PAD_X, textTop(box), I18n.get(meta.titleKey()),
                theme.color(hovered ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_DEFAULT), false);

        switch (meta.type()) {
            case BOOL -> paintPill(painter, box, booleanValue(meta, value));
            case INT -> paintTrack(painter, font, box, intValue(meta, value), min, max);
            case ENUM -> paintValue(painter, font, box, I18n.get(value.toString()));
        }
    }

    private void paintPill(SurfacePainter painter, Rect box, boolean on) {
        Rect pill = new Rect(box.right() - PAD_X - PILL_WIDTH, box.y() + (box.height() - PILL_HEIGHT) / 2,
                PILL_WIDTH, PILL_HEIGHT);
        ShellRenderer.paintRoundedFill(painter, pill, PILL_HEIGHT / 2,
                theme.color(on ? ColorToken.SUCCESS : ColorToken.BORDER_DEFAULT));

        int knob = PILL_HEIGHT - KNOB_INSET * 2;
        int knobX = on ? pill.right() - KNOB_INSET - knob : pill.x() + KNOB_INSET;
        ShellRenderer.paintRoundedFill(painter, new Rect(knobX, pill.y() + KNOB_INSET, knob, knob), knob / 2,
                theme.color(on ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_MUTED));
    }

    private void paintTrack(SurfacePainter painter, Font font, Rect box, int value, int min, int max) {
        int valueX = paintValue(painter, font, box, String.valueOf(value));
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

    private int paintValue(SurfacePainter painter, Font font, Rect box, String text) {
        int x = box.right() - PAD_X - font.width(text);
        painter.text(x, textTop(box), text, theme.color(ColorToken.TEXT_SECONDARY), false);
        return x;
    }

    private int cardArgb(boolean hovered) {
        return theme.color(hovered ? ColorToken.SURFACE_CARD_HOVER : ColorToken.SURFACE_CARD);
    }

    private int borderArgb(boolean hovered, boolean changed) {
        if (changed) {
            return theme.color(ColorToken.BORDER_ACCENT);
        }
        return theme.color(hovered ? ColorToken.BORDER_STRONG : ColorToken.BORDER_SUBTLE);
    }

    private int glowArgb(boolean hovered, boolean changed) {
        if (hovered) {
            return theme.color(ColorToken.ACCENT, HOVER_GLOW_ALPHA);
        }
        return changed ? theme.color(ColorToken.WARNING, CHANGED_GLOW_ALPHA) : 0;
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
