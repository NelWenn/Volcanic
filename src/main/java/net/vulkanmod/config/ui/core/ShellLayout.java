package net.vulkanmod.config.ui.core;

public record ShellLayout(Breakpoint breakpoint, Rect topBar, Rect sidebar,
                          Rect content, Rect details, Rect bottomBar) {
    public static final int TOP_BAR_HEIGHT = 32;
    public static final int BOTTOM_BAR_HEIGHT = 34;
    public static final int SIDEBAR_WIDTH = 150;
    public static final int SIDEBAR_RAIL_WIDTH = 28;
    public static final int DETAILS_WIDTH = 196;

    public static ShellLayout of(int guiWidth, int guiHeight) {
        int width = Math.max(0, guiWidth);
        int height = Math.max(0, guiHeight);
        Breakpoint breakpoint = Breakpoint.forWidth(width);

        Rect topBar = new Rect(0, 0, width, Math.min(TOP_BAR_HEIGHT, height));
        int bottomHeight = Math.min(BOTTOM_BAR_HEIGHT, Math.max(0, height - topBar.height()));
        Rect bottomBar = new Rect(0, height - bottomHeight, width, bottomHeight);

        int bodyTop = topBar.height();
        int bodyHeight = Math.max(0, height - topBar.height() - bottomHeight);

        int sidebarWidth = breakpoint == Breakpoint.COMPACT ? SIDEBAR_RAIL_WIDTH : SIDEBAR_WIDTH;
        sidebarWidth = Math.min(sidebarWidth, width);
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
}
