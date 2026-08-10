package net.vulkanmod.config.ui.core;

public final class PageHeader {
    public static final int PAD_X = 14;
    public static final int BAR_W = 3;
    public static final int BAR_GAP = 9;
    public static final int TEXT_X = PAD_X + BAR_W + BAR_GAP;
    public static final int CRUMB_H = BreadcrumbModel.HEIGHT;
    public static final int TITLE_H = TextScale.LINE_HEIGHT;
    public static final int SUB_LINE = PresetCardLayout.SMALL_LINE;
    public static final int TABS_H = TabStripModel.HEIGHT;
    public static final int RULE_H = 1;
    public static final int CRUMB_TEXT_DY = 1;

    private static final int TOP_PAD = 10;
    private static final int TOP_PAD_COMPACT = 6;
    private static final int CRUMB_GAP = 4;
    private static final int CRUMB_GAP_COMPACT = 3;
    private static final int SUB_GAP = 4;
    private static final int SUB_GAP_COMPACT = 3;
    private static final int TABS_GAP = 8;
    private static final int TABS_GAP_COMPACT = 6;
    private static final int RULE_GAP = 5;
    private static final int RULE_GAP_COMPACT = 4;
    private static final int BODY_GAP = 7;
    private static final int BODY_GAP_COMPACT = 5;

    public record Band(Rect bounds, Rect bar, Rect crumbs, Rect title, Rect subtitle, Rect tabs,
                       Rect rule, int height) {
    }

    private static final Band NO_BAND = new Band(Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, Rect.EMPTY,
            Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, 0);

    private PageHeader() {
    }

    public static Band of(Rect content, boolean crumbs, boolean tabs, boolean subtitle,
                          Breakpoint breakpoint) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        int usable = content.width() - PAD_X * 2;
        if (content.isEmpty() || usable <= 0) {
            return NO_BAND;
        }

        boolean compact = requireBreakpoint(breakpoint) == Breakpoint.COMPACT;
        int height = height(crumbs, tabs, subtitle, breakpoint);
        int textWidth = content.width() - TEXT_X - PAD_X;
        int left = content.x() + PAD_X;
        int textLeft = content.x() + TEXT_X;

        int cursor = content.y() + (compact ? TOP_PAD_COMPACT : TOP_PAD);
        Rect crumbBox = Rect.EMPTY;
        if (crumbs) {
            crumbBox = band(textLeft, cursor, textWidth, CRUMB_H);
            cursor += CRUMB_H + (compact ? CRUMB_GAP_COMPACT : CRUMB_GAP);
        }
        Rect titleBox = band(textLeft, cursor, textWidth, TITLE_H);
        int inkBottom = cursor + TITLE_H;
        cursor = inkBottom;

        Rect subBox = Rect.EMPTY;
        if (subtitle) {
            cursor += compact ? SUB_GAP_COMPACT : SUB_GAP;
            subBox = band(textLeft, cursor, textWidth, SUB_LINE * subLines(breakpoint));
            cursor += SUB_LINE * subLines(breakpoint);
            inkBottom = cursor;
        }

        Rect tabBox = Rect.EMPTY;
        if (tabs) {
            cursor += compact ? TABS_GAP_COMPACT : TABS_GAP;
            tabBox = new Rect(left, cursor, usable, TABS_H);
            cursor += TABS_H;
        }
        cursor += compact ? RULE_GAP_COMPACT : RULE_GAP;
        Rect rule = new Rect(left, cursor, usable, RULE_H);

        int barTop = crumbs ? crumbBox.y() : titleBox.y();
        Rect bar = new Rect(content.x() + PAD_X, barTop, BAR_W, inkBottom - barTop);
        return new Band(new Rect(content.x(), content.y(), content.width(), height), bar,
                crumbBox, titleBox, subBox, tabBox, rule, height);
    }

    public static int height(boolean crumbs, boolean tabs, boolean subtitle, Breakpoint breakpoint) {
        boolean compact = requireBreakpoint(breakpoint) == Breakpoint.COMPACT;
        int height = compact ? TOP_PAD_COMPACT : TOP_PAD;
        if (crumbs) {
            height += CRUMB_H + (compact ? CRUMB_GAP_COMPACT : CRUMB_GAP);
        }
        height += TITLE_H;
        if (subtitle) {
            height += (compact ? SUB_GAP_COMPACT : SUB_GAP) + SUB_LINE * subLines(breakpoint);
        }
        if (tabs) {
            height += (compact ? TABS_GAP_COMPACT : TABS_GAP) + TABS_H;
        }
        return height + (compact ? RULE_GAP_COMPACT : RULE_GAP) + RULE_H
                + (compact ? BODY_GAP_COMPACT : BODY_GAP);
    }

    public static int subLines(Breakpoint breakpoint) {
        return requireBreakpoint(breakpoint) == Breakpoint.COMPACT ? 1 : 2;
    }

    private static Rect band(int x, int y, int width, int height) {
        return width <= 0 ? Rect.EMPTY : new Rect(x, y, width, height);
    }

    private static Breakpoint requireBreakpoint(Breakpoint breakpoint) {
        if (breakpoint == null) {
            throw new IllegalArgumentException("breakpoint must not be null");
        }
        return breakpoint;
    }
}
