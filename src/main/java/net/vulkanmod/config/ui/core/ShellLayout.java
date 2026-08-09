package net.vulkanmod.config.ui.core;

public record ShellLayout(Breakpoint breakpoint, Rect topBar, Rect sidebar,
                          Rect content, Rect details, Rect bottomBar) {
    public static final int TOP_BAR_HEIGHT = 32;
    public static final int BOTTOM_BAR_HEIGHT = 34;
    public static final int SIDEBAR_WIDTH = 150;
    public static final int DETAILS_WIDTH = 196;
    public static final int MENU_BUTTON_SIZE = 14;
    public static final int SEARCH_FIELD_HEIGHT = 16;
    private static final int MENU_BUTTON_X = 10;
    private static final int SEARCH_FIELD_MARGIN = 12;
    private static final int SEARCH_FIELD_WIDTH = 150;
    private static final int SEARCH_FIELD_WIDTH_COMPACT = 96;
    private static final int SEARCH_FIELD_MIN_WIDTH = 80;
    private static final int BRAND_RESERVE = 84;
    private static final int BAR_BUTTON_WIDTH = 54;
    private static final int BAR_BUTTON_HEIGHT = 18;
    private static final int BAR_BUTTON_GAP = 6;
    private static final int BAR_BUTTON_MARGIN = 12;

    public static ShellLayout of(int guiWidth, int guiHeight) {
        int width = Math.max(0, guiWidth);
        int height = Math.max(0, guiHeight);
        Breakpoint breakpoint = Breakpoint.forWidth(width);

        Rect topBar = new Rect(0, 0, width, Math.min(TOP_BAR_HEIGHT, height));
        int bottomHeight = Math.min(BOTTOM_BAR_HEIGHT, Math.max(0, height - topBar.height()));
        Rect bottomBar = new Rect(0, height - bottomHeight, width, bottomHeight);

        int bodyTop = topBar.height();
        int bodyHeight = Math.max(0, height - topBar.height() - bottomHeight);

        int sidebarWidth = breakpoint == Breakpoint.COMPACT ? 0 : Math.min(SIDEBAR_WIDTH, width);
        int detailsWidth = breakpoint == Breakpoint.WIDE ? Math.min(DETAILS_WIDTH, width - sidebarWidth) : 0;
        int contentWidth = Math.max(0, width - sidebarWidth - detailsWidth);

        Rect sidebar = new Rect(0, bodyTop, sidebarWidth, bodyHeight);
        Rect content = new Rect(sidebar.right(), bodyTop, contentWidth, bodyHeight);
        Rect details = detailsWidth > 0
                ? new Rect(content.right(), bodyTop, detailsWidth, bodyHeight)
                : Rect.EMPTY;

        return new ShellLayout(breakpoint, topBar, sidebar, content, details, bottomBar);
    }

    public Rect screen() {
        return new Rect(0, 0, topBar.width(), bottomBar.bottom());
    }

    public boolean hasDetailsPanel() {
        return !details.isEmpty();
    }

    public boolean hasDrawer() {
        return breakpoint == Breakpoint.COMPACT;
    }

    public Rect drawer() {
        if (!hasDrawer()) {
            return Rect.EMPTY;
        }
        return new Rect(content.x(), content.y(), Math.min(SIDEBAR_WIDTH, content.width()), content.height());
    }

    public Rect menuButton() {
        if (!hasDrawer()) {
            return Rect.EMPTY;
        }
        int size = Math.min(MENU_BUTTON_SIZE, Math.min(topBar.width(), topBar.height()));
        if (size <= 0) {
            return Rect.EMPTY;
        }
        return new Rect(topBar.x() + Math.min(MENU_BUTTON_X, topBar.width() - size),
                topBar.y() + (topBar.height() - size) / 2, size, size);
    }

    public Rect searchField() {
        if (topBar.height() < SEARCH_FIELD_HEIGHT) {
            return Rect.EMPTY;
        }
        int wanted = breakpoint == Breakpoint.COMPACT ? SEARCH_FIELD_WIDTH_COMPACT : SEARCH_FIELD_WIDTH;
        int width = Math.min(wanted, topBar.width() - SEARCH_FIELD_MARGIN * 2 - BRAND_RESERVE);
        if (width < SEARCH_FIELD_MIN_WIDTH) {
            return Rect.EMPTY;
        }
        return new Rect(topBar.right() - SEARCH_FIELD_MARGIN - width,
                topBar.y() + (topBar.height() - SEARCH_FIELD_HEIGHT) / 2, width, SEARCH_FIELD_HEIGHT);
    }

    public Rect applyButton() {
        return barButton(0);
    }

    public Rect discardButton() {
        return barButton(1);
    }

    private Rect barButton(int fromRight) {
        int needed = BAR_BUTTON_MARGIN * 2 + BAR_BUTTON_WIDTH * 2 + BAR_BUTTON_GAP;
        if (bottomBar.height() < BAR_BUTTON_HEIGHT || bottomBar.width() < needed) {
            return Rect.EMPTY;
        }
        return new Rect(bottomBar.right() - BAR_BUTTON_MARGIN
                - (fromRight + 1) * BAR_BUTTON_WIDTH - fromRight * BAR_BUTTON_GAP,
                bottomBar.y() + (bottomBar.height() - BAR_BUTTON_HEIGHT) / 2,
                BAR_BUTTON_WIDTH, BAR_BUTTON_HEIGHT);
    }

    public Rect sidebarOrDrawer(boolean drawerOpen) {
        return hasDrawer() ? (drawerOpen ? drawer() : Rect.EMPTY) : sidebar;
    }
}
