package net.vulkanmod.config.ui.core;

public record ShellLayout(Breakpoint breakpoint, Rect topBar, Rect sidebar,
                          Rect content, Rect details, Rect bottomBar) {
    public static final int TOP_BAR_HEIGHT = 32;
    public static final int BOTTOM_BAR_HEIGHT = 34;
    public static final int SIDEBAR_WIDTH = 150;
    public static final int DETAILS_WIDTH = 196;
    public static final int MENU_BUTTON_SIZE = 14;
    private static final int MENU_BUTTON_X = 10;

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

    public Rect sidebarOrDrawer(boolean drawerOpen) {
        return hasDrawer() ? (drawerOpen ? drawer() : Rect.EMPTY) : sidebar;
    }
}
