package net.vulkanmod.config.ui.shell;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.vulkanmod.config.ui.core.ApplyBarModel;
import net.vulkanmod.config.ui.core.ApplyScope;
import net.vulkanmod.config.ui.core.BreadcrumbModel;
import net.vulkanmod.config.ui.core.ColorToken;
import net.vulkanmod.config.ui.core.Gradient;
import net.vulkanmod.config.ui.core.NavNode;
import net.vulkanmod.config.ui.core.OverviewModel;
import net.vulkanmod.config.ui.core.Rect;
import net.vulkanmod.config.ui.core.RoundedScanline;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.ScrollIndicator;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingRowLayout;
import net.vulkanmod.config.ui.core.ShellLayout;
import net.vulkanmod.config.ui.core.SidebarModel;
import net.vulkanmod.config.ui.core.SidebarViewport;
import net.vulkanmod.config.ui.core.SliderGeometry;
import net.vulkanmod.config.ui.core.TabStripModel;
import net.vulkanmod.config.ui.core.Theme;
import net.vulkanmod.config.ui.render.SurfacePainter;
import net.vulkanmod.config.ui.settings.SettingBinding;
import net.vulkanmod.config.ui.settings.SettingsCatalog;

import java.util.List;
import java.util.Locale;

public final class ShellRenderer {
    private static final int NAV_RADIUS = 8;
    private static final int PILL_RADIUS = 11;
    private static final int TEXT_HEIGHT = 9;

    private static final int SECTION_TEXT_X = 9;
    private static final int ROW_TEXT_X = 17;
    private static final int ROW_INSET_X = 4;
    private static final int ROW_INSET_Y = 1;
    private static final int ACCENT_BAR_WIDTH = 2;

    static final int CARD_PAD_X = 12;
    static final int SLIDER_TRACK_WIDTH = 56;

    private static final int CONTENT_PAD_X = 14;
    private static final int BREADCRUMB_Y = 12;
    private static final int TITLE_Y = 30;
    private static final int TAB_STRIP_Y = 48;

    private static final int BRAND_X = 12;
    private static final int BRAND_Y = 12;
    private static final int BRAND_GAP = 8;
    private static final float SCRIM_ALPHA = 0.72f;
    private static final int BAR_BUTTON_RADIUS = 5;
    private static final String KEY_APPLY = "vulkanmod.applybar.apply";
    private static final String KEY_DISCARD = "vulkanmod.applybar.discard";
    private static final String BRAND = "VOLCANIC";
    private static final String BREADCRUMB_SEPARATOR = "›";
    private static final RouteId OVERVIEW = RouteId.parse("overview");

    private final Theme theme;
    private final SettingRowRenderer rowRenderer;

    public ShellRenderer(Theme theme) {
        if (theme == null) {
            throw new IllegalArgumentException("theme must not be null");
        }
        this.theme = theme;
        this.rowRenderer = new SettingRowRenderer(theme);
    }

    public void render(GuiGraphics graphics, SurfacePainter painter, Font font, ShellLayout layout,
                       NavPresenter presenter, int scroll, int contentScroll, int mouseX, int mouseY,
                       SettingId dragged, boolean drawerOpen) {
        if (graphics == null) {
            throw new IllegalArgumentException("graphics must not be null");
        }
        if (painter == null) {
            throw new IllegalArgumentException("painter must not be null");
        }
        requireInputs(font, layout, presenter);
        if (scroll < 0) {
            throw new IllegalArgumentException("scroll must not be negative: " + scroll);
        }
        if (contentScroll < 0) {
            throw new IllegalArgumentException("contentScroll must not be negative: " + contentScroll);
        }

        paintChrome(painter, layout, drawerOpen);
        paintApplyBar(painter, font, layout, presenter, mouseX, mouseY);
        painter.flush();

        Rect nav = layout.sidebarOrDrawer(drawerOpen);
        if (!layout.hasDrawer()) {
            paintNav(graphics, painter, nav, presenter, scroll, mouseX, mouseY);
        }

        Rect content = layout.content();
        if (!content.isEmpty()) {
            graphics.enableScissor(content.x(), content.y(), content.right(), content.bottom());
            try {
                paintContent(painter, font, layout, presenter, contentScroll, mouseX, mouseY, dragged);
                painter.flush();
            } finally {
                graphics.disableScissor();
            }
        }

        if (layout.hasDrawer() && !nav.isEmpty()) {
            painter.fill(layout.content(), theme.color(ColorToken.SURFACE_SUNKEN, SCRIM_ALPHA));
            painter.flush();
            paintNav(graphics, painter, nav, presenter, scroll, mouseX, mouseY);
            painter.fill(new Rect(nav.right(), nav.y(), 1, nav.height()),
                    theme.color(ColorToken.BORDER_ACCENT));
            painter.flush();
        }
    }

    private void paintNav(GuiGraphics graphics, SurfacePainter painter, Rect region, NavPresenter presenter,
                          int scroll, int mouseX, int mouseY) {
        if (region.isEmpty()) {
            return;
        }
        graphics.enableScissor(region.x(), region.y(), region.right(), region.bottom());
        try {
            paintSidebar(painter, region, presenter, scroll, mouseX, mouseY);
            painter.flush();
        } finally {
            graphics.disableScissor();
        }
    }

    public RouteId sidebarRouteAt(Rect nav, SidebarModel model, int scroll, int mouseX, int mouseY) {
        if (nav == null) {
            throw new IllegalArgumentException("nav must not be null");
        }
        if (model == null) {
            throw new IllegalArgumentException("model must not be null");
        }
        SidebarViewport viewport = new SidebarViewport(nav, scroll);
        return viewport.contains(mouseX, mouseY) ? model.routeAt(viewport.contentY(mouseY)) : null;
    }

    public List<Rect> breadcrumbBoxes(Font font, ShellLayout layout, NavPresenter presenter) {
        requireInputs(font, layout, presenter);
        List<RouteId> trail = presenter.stack().trail();
        if (trail.size() < 2) {
            return List.of();
        }
        int[] widths = new int[trail.size()];
        for (int i = 0; i < trail.size(); i++) {
            widths[i] = font.width(label(presenter, trail.get(i)));
        }
        return BreadcrumbModel.layout(widths,
                layout.content().x() + CONTENT_PAD_X, layout.content().y() + BREADCRUMB_Y);
    }

    public List<Rect> tabStripBoxes(Font font, ShellLayout layout, NavPresenter presenter) {
        requireInputs(font, layout, presenter);
        List<NavNode> tabs = presenter.subTabs();
        int[] widths = new int[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) {
            widths[i] = font.width(I18n.get(tabs.get(i).titleKey()));
        }

        Rect content = layout.content();
        int left = content.x() + CONTENT_PAD_X;
        int right = Math.max(left, content.right() - CONTENT_PAD_X);
        List<Rect> boxes = TabStripModel.layout(widths, left, content.y() + TAB_STRIP_Y);
        return TabStripModel.shifted(boxes,
                TabStripModel.scrollToReveal(boxes, revealIndex(presenter, tabs), left, right));
    }

    public List<Rect> settingRowBoxes(ShellLayout layout, NavPresenter presenter, int scroll) {
        if (layout == null) {
            throw new IllegalArgumentException("layout must not be null");
        }
        if (presenter == null) {
            throw new IllegalArgumentException("presenter must not be null");
        }
        return SettingRowLayout.rows(layout.content(), presenter.contentRowCount(), scroll, layout.breakpoint());
    }

    public static Rect sliderTrack(Rect row) {
        if (row == null) {
            throw new IllegalArgumentException("row must not be null");
        }
        return SliderGeometry.track(SettingRowLayout.cardBox(row), CARD_PAD_X, SLIDER_TRACK_WIDTH);
    }

    public ScrollIndicator contentScrollIndicator(ShellLayout layout, NavPresenter presenter, int scroll) {
        if (layout == null) {
            throw new IllegalArgumentException("layout must not be null");
        }
        if (presenter == null) {
            throw new IllegalArgumentException("presenter must not be null");
        }
        return ScrollIndicator.of(layout.content(),
                SettingRowLayout.contentHeight(presenter.contentRowCount(), layout.breakpoint()), scroll);
    }

    private static int revealIndex(NavPresenter presenter, List<NavNode> tabs) {
        String focusedId = focusedIn(presenter, NavPresenter.REGION_CONTENT);
        RouteId current = presenter.stack().current();
        int active = -1;
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).route().toString().equals(focusedId)) {
                return i;
            }
            if (tabs.get(i).route().equals(current)) {
                active = i;
            }
        }
        return active;
    }

    private void paintChrome(SurfacePainter painter, ShellLayout layout, boolean drawerOpen) {
        Rect screen = new Rect(0, 0, layout.topBar().width(), layout.bottomBar().bottom());
        painter.fill(screen, theme.color(ColorToken.SURFACE_BASE));
        painter.fill(layout.topBar(), theme.color(ColorToken.SURFACE_CHROME));
        painter.fill(layout.bottomBar(), theme.color(ColorToken.SURFACE_CHROME));

        if (layout.hasDetailsPanel()) {
            painter.fill(layout.details(), theme.color(ColorToken.SURFACE_CHROME));
        }

        int border = theme.color(ColorToken.BORDER_DEFAULT);
        if (!layout.sidebar().isEmpty()) {
            painter.fill(new Rect(layout.sidebar().right(), layout.sidebar().y(), 1, layout.sidebar().height()),
                    border);
        }
        painter.fill(new Rect(layout.topBar().x(), layout.topBar().bottom() - 1, layout.topBar().width(), 1), border);
        painter.fill(new Rect(layout.bottomBar().x(), layout.bottomBar().y(), layout.bottomBar().width(), 1), border);

        Rect menu = layout.menuButton();
        int brandX = menu.isEmpty() ? BRAND_X : menu.right() + BRAND_GAP;
        painter.text(layout.topBar().x() + brandX, layout.topBar().y() + BRAND_Y, BRAND,
                theme.color(ColorToken.TEXT_PRIMARY), false);

        if (!menu.isEmpty()) {
            paintMenuIcon(painter, menu, theme.color(drawerOpen ? ColorToken.ACCENT : ColorToken.TEXT_SECONDARY));
        }
    }

    public Rect applyButton(ShellLayout layout, NavPresenter presenter) {
        requireBarInputs(layout, presenter);
        return ApplyBarModel.of(presenter.pending()).visible() ? layout.applyButton() : Rect.EMPTY;
    }

    public Rect discardButton(ShellLayout layout, NavPresenter presenter) {
        requireBarInputs(layout, presenter);
        return ApplyBarModel.of(presenter.pending()).visible() ? layout.discardButton() : Rect.EMPTY;
    }

    private void paintApplyBar(SurfacePainter painter, Font font, ShellLayout layout, NavPresenter presenter,
                               int mouseX, int mouseY) {
        ApplyBarModel bar = ApplyBarModel.of(presenter.pending());
        Rect region = layout.bottomBar();
        if (!bar.visible() || region.isEmpty()) {
            return;
        }
        ColorToken token = bar.scope() == ApplyScope.RESTART ? ColorToken.WARNING : ColorToken.TEXT_SECONDARY;
        painter.text(region.x() + CONTENT_PAD_X, region.y() + (region.height() - TEXT_HEIGHT) / 2,
                I18n.get(bar.messageKey(), bar.count()), theme.color(token), false);

        paintBarButton(painter, font, applyButton(layout, presenter), I18n.get(KEY_APPLY),
                ColorToken.ACCENT, mouseX, mouseY);
        paintBarButton(painter, font, discardButton(layout, presenter), I18n.get(KEY_DISCARD),
                ColorToken.BORDER_STRONG, mouseX, mouseY);
    }

    private void paintBarButton(SurfacePainter painter, Font font, Rect box, String text, ColorToken border,
                                int mouseX, int mouseY) {
        if (box.isEmpty()) {
            return;
        }
        boolean hovered = box.contains(mouseX, mouseY);
        paintRoundedFill(painter, box, BAR_BUTTON_RADIUS,
                theme.color(hovered ? ColorToken.SURFACE_CARD_HOVER : ColorToken.SURFACE_CARD));
        paintRoundedOutline(painter, box, BAR_BUTTON_RADIUS, theme.color(hovered ? ColorToken.ACCENT : border));
        painter.text(box.x() + (box.width() - font.width(text)) / 2, box.y() + (box.height() - TEXT_HEIGHT) / 2,
                text, theme.color(hovered ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_DEFAULT), false);
    }

    private static void requireBarInputs(ShellLayout layout, NavPresenter presenter) {
        if (layout == null) {
            throw new IllegalArgumentException("layout must not be null");
        }
        if (presenter == null) {
            throw new IllegalArgumentException("presenter must not be null");
        }
    }

    private static void paintMenuIcon(SurfacePainter painter, Rect button, int argb) {
        int barHeight = Math.max(1, button.height() / 7);
        int gap = Math.max(1, (button.height() - barHeight * 3) / 2);
        int top = button.y() + Math.max(0, (button.height() - barHeight * 3 - gap * 2) / 2);
        for (int bar = 0; bar < 3; bar++) {
            painter.fill(new Rect(button.x(), top + bar * (barHeight + gap), button.width(), barHeight), argb);
        }
    }

    private void paintSidebar(SurfacePainter painter, Rect sidebar, NavPresenter presenter,
                              int scroll, int mouseX, int mouseY) {
        Gradient background = theme.gradient(ColorToken.SURFACE_BASE, ColorToken.SURFACE_SIDEBAR_BOTTOM);
        painter.gradient(sidebar, background.topArgb(), background.bottomArgb());

        SidebarModel model = presenter.sidebar();
        SidebarViewport viewport = new SidebarViewport(sidebar, scroll);
        RouteId activeRoute = presenter.activeSidebarRoute();
        RouteId hoveredRoute = sidebarRouteAt(sidebar, model, scroll, mouseX, mouseY);
        String focusedId = focusedIn(presenter, NavPresenter.REGION_SIDEBAR);

        int first = model.firstVisible(scroll);
        int last = model.lastVisible(scroll, sidebar.height());
        for (int index = first; first >= 0 && index <= last; index++) {
            int height = model.heightOf(index);
            int top = viewport.screenTop(model.offsetOf(index));

            switch (model.entries().get(index)) {
                case SidebarModel.Section(String labelKey) -> painter.text(sidebar.x() + SECTION_TEXT_X,
                        top + (height - TEXT_HEIGHT) / 2, I18n.get(labelKey).toUpperCase(Locale.ROOT),
                        theme.color(ColorToken.TEXT_FAINT), false);
                case SidebarModel.Row(RouteId route, String titleKey) -> {
                    Rect box = rowBox(sidebar, top, height);
                    boolean active = route.equals(activeRoute);
                    paintRowSurface(painter, box, active, route.equals(hoveredRoute),
                            route.toString().equals(focusedId));
                    painter.text(sidebar.x() + ROW_TEXT_X, top + (height - TEXT_HEIGHT) / 2, I18n.get(titleKey),
                            theme.color(active ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_SECONDARY), false);
                }
            }
        }

        paintScrollIndicator(painter, ScrollIndicator.of(sidebar, model.totalHeight(), scroll));
    }

    private void paintRowSurface(SurfacePainter painter, Rect box, boolean active, boolean hovered, boolean focused) {
        if (active) {
            paintRoundedGradient(painter, box, NAV_RADIUS,
                    theme.color(ColorToken.SURFACE_NAV_ACTIVE), theme.color(ColorToken.SURFACE_SIDEBAR_BOTTOM));
            paintRoundedOutline(painter, box, NAV_RADIUS, theme.color(ColorToken.BORDER_ACCENT));
            paintLeadingEdge(painter, box, NAV_RADIUS, theme.color(ColorToken.ACCENT));
        } else if (hovered) {
            paintRoundedFill(painter, box, NAV_RADIUS, theme.color(ColorToken.SURFACE_CARD_HOVER));
        }
        if (focused) {
            paintRoundedOutline(painter, box, NAV_RADIUS, theme.color(ColorToken.ACCENT));
        }
    }

    private void paintContent(SurfacePainter painter, Font font, ShellLayout layout, NavPresenter presenter,
                              int contentScroll, int mouseX, int mouseY, SettingId dragged) {
        Rect content = layout.content();
        if (content.isEmpty()) {
            return;
        }

        paintBreadcrumbs(painter, font, layout, presenter);

        painter.text(content.x() + CONTENT_PAD_X, content.y() + TITLE_Y, I18n.get(presenter.currentTitleKey()),
                theme.color(ColorToken.TEXT_PRIMARY), false);

        paintTabStrip(painter, font, layout, presenter);
        if (OVERVIEW.equals(presenter.stack().current())) {
            paintOverview(painter, font, layout, presenter, contentScroll);
        } else {
            paintSettings(painter, font, layout, presenter, contentScroll, mouseX, mouseY, dragged);
        }
        paintScrollIndicator(painter, layout, presenter, contentScroll);
    }

    private void paintOverview(SurfacePainter painter, Font font, ShellLayout layout, NavPresenter presenter,
                               int contentScroll) {
        List<OverviewModel.Row> rows = presenter.catalog().overview().rows();
        List<Rect> boxes = settingRowBoxes(layout, presenter, contentScroll);
        int labelArgb = theme.color(ColorToken.TEXT_DEFAULT);
        int valueArgb = theme.color(ColorToken.TEXT_SECONDARY);

        for (int i = 0; i < boxes.size(); i++) {
            Rect card = SettingRowLayout.cardBox(boxes.get(i));
            if (card.isEmpty()) {
                continue;
            }
            paintRoundedFill(painter, card, SettingRowLayout.CARD_RADIUS,
                    theme.color(ColorToken.SURFACE_CARD));
            paintRoundedOutline(painter, card, SettingRowLayout.CARD_RADIUS,
                    theme.color(ColorToken.BORDER_SUBTLE));

            int top = card.y() + (card.height() - TEXT_HEIGHT) / 2;
            String value = rows.get(i).value();
            painter.text(card.x() + CARD_PAD_X, top, I18n.get(rows.get(i).labelKey()), labelArgb, false);
            painter.text(card.right() - CARD_PAD_X - font.width(value), top, value, valueArgb, false);
        }
    }

    private void paintScrollIndicator(SurfacePainter painter, ShellLayout layout, NavPresenter presenter,
                                      int contentScroll) {
        paintScrollIndicator(painter, contentScrollIndicator(layout, presenter, contentScroll));
    }

    private void paintScrollIndicator(SurfacePainter painter, ScrollIndicator indicator) {
        if (!indicator.visible()) {
            return;
        }
        int radius = indicator.track().width() / 2;
        paintRoundedFill(painter, indicator.track(), radius, theme.color(ColorToken.BORDER_SUBTLE));
        paintRoundedFill(painter, indicator.thumb(), radius, theme.color(ColorToken.BORDER_ACCENT));
    }

    private void paintSettings(SurfacePainter painter, Font font, ShellLayout layout, NavPresenter presenter,
                               int contentScroll, int mouseX, int mouseY, SettingId dragged) {
        List<SettingMeta> settings = presenter.settings();
        List<Rect> boxes = settingRowBoxes(layout, presenter, contentScroll);
        SettingsCatalog catalog = presenter.catalog();
        String focusedId = focusedIn(presenter, NavPresenter.REGION_CONTENT);
        for (int i = 0; i < boxes.size(); i++) {
            Rect box = boxes.get(i);
            SettingMeta meta = settings.get(i);
            SettingBinding binding = catalog.binding(meta.id());
            rowRenderer.render(painter, font, box, meta, binding, presenter.valueOf(meta),
                    catalog.enabled(meta.id()),
                    box.contains(mouseX, mouseY) || meta.id().equals(dragged), presenter.resettable(meta),
                    SettingRowLayout.resetBox(box).contains(mouseX, mouseY));
            if (meta.id().toString().equals(focusedId)) {
                paintRoundedOutline(painter, SettingRowLayout.cardBox(box), SettingRowLayout.CARD_RADIUS,
                        theme.color(ColorToken.ACCENT));
            }
        }
    }

    private void paintBreadcrumbs(SurfacePainter painter, Font font, ShellLayout layout, NavPresenter presenter) {
        List<RouteId> trail = presenter.stack().trail();
        List<Rect> boxes = breadcrumbBoxes(font, layout, presenter);
        int separatorWidth = font.width(BREADCRUMB_SEPARATOR);
        int faint = theme.color(ColorToken.TEXT_FAINT);

        for (int i = 0; i < boxes.size(); i++) {
            Rect box = boxes.get(i);
            boolean last = i == boxes.size() - 1;
            painter.text(box.x(), box.y(), label(presenter, trail.get(i)),
                    theme.color(last ? ColorToken.TEXT_DEFAULT : ColorToken.TEXT_FAINT), false);
            if (!last) {
                int gap = boxes.get(i + 1).x() - box.right();
                painter.text(box.right() + (gap - separatorWidth) / 2, box.y(), BREADCRUMB_SEPARATOR, faint, false);
            }
        }
    }

    private void paintTabStrip(SurfacePainter painter, Font font, ShellLayout layout, NavPresenter presenter) {
        List<NavNode> tabs = presenter.subTabs();
        if (tabs.isEmpty()) {
            return;
        }

        List<Rect> boxes = tabStripBoxes(font, layout, presenter);
        RouteId current = presenter.stack().current();
        String focusedId = focusedIn(presenter, NavPresenter.REGION_CONTENT);
        int gradientTop = theme.color(ColorToken.ACCENT_BRIGHT);
        int gradientBottom = theme.color(ColorToken.ACCENT_DEEP);

        for (int i = 0; i < boxes.size(); i++) {
            Rect box = boxes.get(i);
            NavNode tab = tabs.get(i);
            boolean active = tab.route().equals(current);

            if (active) {
                paintRoundedGradient(painter, box, PILL_RADIUS, gradientTop, gradientBottom);
            }
            if (tab.route().toString().equals(focusedId)) {
                paintRoundedOutline(painter, box, PILL_RADIUS, theme.color(ColorToken.ACCENT));
            }

            String text = I18n.get(tab.titleKey());
            painter.text(box.x() + (box.width() - font.width(text)) / 2, box.y() + (box.height() - TEXT_HEIGHT) / 2,
                    text, theme.color(active ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_SECONDARY), false);
        }
    }

    private static void paintRoundedGradient(SurfacePainter painter, Rect rect, int radius, int topArgb,
                                             int bottomArgb) {
        int height = rect.height();
        for (Rect span : RoundedScanline.fillSpans(rect, radius)) {
            float progress = height == 1 ? 0.0f : (float) (span.y() - rect.y()) / (height - 1);
            painter.fill(span, lerpArgb(topArgb, bottomArgb, progress));
        }
    }

    static void paintRoundedFill(SurfacePainter painter, Rect rect, int radius, int argb) {
        for (Rect span : RoundedScanline.fillSpans(rect, radius)) {
            painter.fill(span, argb);
        }
    }

    static void paintRoundedOutline(SurfacePainter painter, Rect rect, int radius, int argb) {
        for (Rect span : RoundedScanline.outlineSpans(rect, radius)) {
            painter.fill(span, argb);
        }
    }

    private static void paintLeadingEdge(SurfacePainter painter, Rect rect, int radius, int argb) {
        for (Rect span : RoundedScanline.fillSpans(rect, radius)) {
            painter.fill(new Rect(span.x(), span.y(), Math.min(ACCENT_BAR_WIDTH, span.width()), 1), argb);
        }
    }

    private static Rect rowBox(Rect sidebar, int top, int height) {
        return new Rect(sidebar.x() + ROW_INSET_X, top + ROW_INSET_Y,
                Math.max(0, sidebar.width() - ROW_INSET_X * 2), Math.max(0, height - ROW_INSET_Y * 2));
    }

    private static String focusedIn(NavPresenter presenter, String regionId) {
        return regionId.equals(presenter.focus().activeRegion()) ? presenter.focus().focused() : null;
    }

    private static String label(NavPresenter presenter, RouteId route) {
        return I18n.get(presenter.titleKeyOf(route));
    }

    private static int lerpArgb(int from, int to, float progress) {
        int alpha = lerpChannel(from >>> 24, to >>> 24, progress);
        int red = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, progress);
        int green = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, progress);
        int blue = lerpChannel(from & 0xFF, to & 0xFF, progress);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int lerpChannel(int from, int to, float progress) {
        return Math.round(from + (to - from) * progress);
    }

    private static void requireInputs(Font font, ShellLayout layout, NavPresenter presenter) {
        if (font == null) {
            throw new IllegalArgumentException("font must not be null");
        }
        if (layout == null) {
            throw new IllegalArgumentException("layout must not be null");
        }
        if (presenter == null) {
            throw new IllegalArgumentException("presenter must not be null");
        }
    }
}
