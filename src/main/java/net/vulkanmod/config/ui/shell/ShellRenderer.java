package net.vulkanmod.config.ui.shell;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.vulkanmod.config.ui.core.ApplyBarModel;
import net.vulkanmod.config.ui.core.ApplyScope;
import net.vulkanmod.config.ui.core.BreadcrumbModel;
import net.vulkanmod.config.ui.core.Breakpoint;
import net.vulkanmod.config.ui.core.ColorToken;
import net.vulkanmod.config.ui.core.DetailsContent;
import net.vulkanmod.config.ui.core.DetailsItem;
import net.vulkanmod.config.ui.core.DetailsLayout;
import net.vulkanmod.config.ui.core.FrameSamples;
import net.vulkanmod.config.ui.core.PresetCardLayout;
import net.vulkanmod.config.ui.core.PresetCardModel;
import net.vulkanmod.config.ui.core.PresetRating;
import net.vulkanmod.config.ui.core.Recommendation;
import net.vulkanmod.config.ui.settings.OverviewSignals;
import net.vulkanmod.vulkan.SessionSamples;
import net.vulkanmod.config.ui.core.Gradient;
import net.vulkanmod.config.ui.core.HoverState;
import net.vulkanmod.config.ui.core.ImpactLevel;
import net.vulkanmod.config.ui.core.Motion;
import net.vulkanmod.config.ui.core.NavNode;
import net.vulkanmod.config.ui.core.OverviewModel;
import net.vulkanmod.config.ui.core.ProfileChipRow;
import net.vulkanmod.config.ui.core.Rect;
import net.vulkanmod.config.ui.core.RoundedScanline;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.ScrollIndicator;
import net.vulkanmod.config.ui.core.SearchIndex;
import net.vulkanmod.config.ui.core.SearchResultsModel;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingRowLayout;
import net.vulkanmod.config.ui.core.SettingSource;
import net.vulkanmod.config.ui.core.ShellLayout;
import net.vulkanmod.config.ui.core.SidebarModel;
import net.vulkanmod.config.ui.core.SidebarViewport;
import net.vulkanmod.config.ui.core.SliderGeometry;
import net.vulkanmod.config.ui.core.TabStripModel;
import net.vulkanmod.config.ui.core.Theme;
import net.vulkanmod.config.ui.core.TooltipLayout;
import net.vulkanmod.config.ui.render.SurfacePainter;
import net.vulkanmod.config.ui.settings.SettingBinding;
import net.vulkanmod.config.ui.settings.SettingsCatalog;

import java.util.ArrayList;
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
    private static final int ROW_INDENT = 10;
    private static final int CHEVRON = 5;
    private static final String[] CHEVRON_RIGHT = {"#....", "##...", "###..", "##...", "#...."};
    private static final String[] CHEVRON_DOWN = {"#####", ".###.", ".###.", "..#..", "....."};

    static final int CARD_PAD_X = SettingRowLayout.CARD_PAD_X;
    static final int SLIDER_TRACK_WIDTH = 56;

    private static final int CONTENT_PAD_X = 14;
    private static final int BREADCRUMB_Y = 12;
    private static final int TITLE_Y = 30;
    private static final int TAB_STRIP_Y = 48;

    private static final float SCRIM_ALPHA = 0.72f;
    private static final int BAR_BUTTON_RADIUS = 5;
    private static final int SEARCH_RADIUS = 5;
    private static final ResourceLocation LOGO =
            ResourceLocation.fromNamespaceAndPath("vulkanmod", "textures/gui/volcanic_logo.png");
    private static final ResourceLocation TITLE =
            ResourceLocation.fromNamespaceAndPath("vulkanmod", "textures/gui/volcanic_title.png");
    private static final String KEY_APPLY = "vulkanmod.applybar.apply";
    private static final String KEY_DISCARD = "vulkanmod.applybar.discard";
    private static final String BREADCRUMB_SEPARATOR = "›";
    private static final String KEY_FAVORITES_EMPTY = "vulkanmod.ui.favorites.empty";
    private static final String KEY_FAVORITES = "vulkanmod.ui.page.favorites";
    private static final String KEY_MODS_EMPTY = "vulkanmod.ui.mods.empty";
    private static final String KEY_MODS_OPEN = "vulkanmod.ui.mods.open";
    private static final String KEY_MODS_PASSTHROUGH = "vulkanmod.ui.mods.passthrough";
    private static final int MOD_BUTTON_PAD_X = 10;
    private static final int MOD_NOTE_GAP = 10;
    private static final int PLUGIN_LIST_TOP = 62;
    private static final String KEY_SEARCH_PROMPT = "vulkanmod.ui.search.prompt";
    private static final String KEY_SEARCH_EMPTY = "vulkanmod.ui.search.empty";
    private static final String KEY_SEARCH_SOURCE = "vulkanmod.ui.search.source.";
    private static final int OVERLAY_RADIUS = 6;
    private static final int TOOLTIP_RADIUS = 4;
    private static final int OVERLAY_PAD_X = 8;
    private static final int RESULT_RADIUS = 4;
    private static final String KEY_DETAILS_EMPTY = "vulkanmod.details.empty";
    private static final String KEY_DETAILS_PERFORMANCE = "vulkanmod.details.performance";
    private static final String KEY_DETAILS_VISUAL = "vulkanmod.details.visual";
    private static final String KEY_DETAILS_RECOMMENDED = "vulkanmod.details.recommended";
    private static final String KEY_DETAILS_RESTART = "vulkanmod.details.restart";
    private static final String KEY_DETAILS_EXPERIMENTAL = "vulkanmod.details.experimental";
    private static final int DETAILS_GLYPH = 7;
    private static final int DETAILS_GLYPH_GAP = 3;
    private static final RouteId OVERVIEW = RouteId.parse("overview");
    private static final RouteId FAVORITES = RouteId.parse("favorites");
    private static final RouteId MODS = RouteId.parse("mods");
    private static final RouteId PLUGINS = RouteId.parse("plugins");
    private static final String KEY_PLUGINS_EMPTY = "vulkanmod.ui.plugins.empty";
    private static final String KEY_PLUGINS_INSTALLED = "vulkanmod.ui.plugins.installed";
    private static final String KEY_PLUGINS_MODS = "vulkanmod.ui.plugins.mods";
    private static final String KEY_PLUGINS_DISABLED = "vulkanmod.ui.plugins.disabled";
    private static final String KEY_PLUGIN_EMPTY = "vulkanmod.ui.plugin.empty";
    private static final int RATING_FROM_BOTTOM = 22;
    private static final String KEY_SUGGEST = "vulkanmod.overview.suggest";
    private static final String KEY_SUGGEST_WAIT = "vulkanmod.overview.suggest_wait";
    private static final String KEY_SUGGESTED = "vulkanmod.overview.suggested";
    private static final String KEY_PLAYING_NOW = "vulkanmod.overview.playing_now";
    private static final String KEY_PENDING = "vulkanmod.overview.pending";
    private static final String KEY_NOT_TRIED = "vulkanmod.overview.not_tried";
    private static final int ICON_RISE = -1;
    private static final String KEY_PROFILES = "vulkanmod.overview.profiles";
    private static final String KEY_PROFILES_INTRO = "vulkanmod.overview.profiles_intro";
    private static final String KEY_PROFILES_LEGEND = "vulkanmod.overview.profiles_legend";
    private static final String KEY_FRAMES = "vulkanmod.overview.frames";
    private static final String KEY_LOOKS = "vulkanmod.overview.looks";

    private static final int CARD_MARGIN = 4;

    private final Theme theme;
    private final SettingRowRenderer rowRenderer;
    private Rect lastCard = Rect.EMPTY;
    private final HoverState hover = new HoverState(Motion.HOVER_MS);
    private final HoverState focus = new HoverState(Motion.SELECTION_MS);

    public ShellRenderer(Theme theme) {
        if (theme == null) {
            throw new IllegalArgumentException("theme must not be null");
        }
        this.theme = theme;
        this.rowRenderer = new SettingRowRenderer(theme);
    }

    public void render(GuiGraphics graphics, SurfacePainter painter, Font font, ShellLayout layout,
                       NavPresenter presenter, int scroll, int contentScroll, int mouseX, int mouseY,
                       SettingId dragged, boolean drawerOpen, boolean searchFocused, long deltaMs) {
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
        if (deltaMs < 0) {
            throw new IllegalArgumentException("deltaMs must not be negative: " + deltaMs);
        }

        paintChrome(painter, layout, drawerOpen, searchFocused);
        painter.flush();

        paintBrand(graphics, layout);
        paintFavoritesButton(painter, font, layout, presenter, mouseX, mouseY);
        painter.flush();

        Rect nav = layout.sidebarOrDrawer(drawerOpen);
        if (!layout.hasDrawer()) {
            paintNav(graphics, painter, nav, presenter, scroll, mouseX, mouseY, deltaMs);
        }

        Rect content = layout.content();
        if (!content.isEmpty()) {
            graphics.enableScissor(content.x(), content.y(), content.right(), content.bottom());
            try {
                paintContent(painter, font, layout, presenter, contentScroll, mouseX, mouseY, dragged, deltaMs);
                painter.flush();
            } finally {
                graphics.disableScissor();
            }
        }

        Rect details = layout.details();
        if (!details.isEmpty()) {
            graphics.enableScissor(details.x(), details.y(), details.right(), details.bottom());
            try {
                SettingMeta target = searchFocused
                        ? presenter.focusedSetting()
                        : cardTarget(layout, presenter, contentScroll, mouseX, mouseY, null, dragged);
                paintDetailItems(painter, details, detailsItems(font, presenter, target, details));
                painter.flush();
            } finally {
                graphics.disableScissor();
            }
        }

        if (layout.hasDrawer() && !nav.isEmpty()) {
            painter.fill(layout.content(), theme.color(ColorToken.SURFACE_SUNKEN, SCRIM_ALPHA));
            painter.flush();
            paintNav(graphics, painter, nav, presenter, scroll, mouseX, mouseY, deltaMs);
            painter.fill(new Rect(nav.right(), nav.y(), 1, nav.height()),
                    theme.color(ColorToken.BORDER_ACCENT));
            painter.flush();
        }

        paintApplyBar(painter, font, layout, presenter, mouseX, mouseY);
        painter.flush();

        hover.endFrame();
        focus.endFrame();
    }

    private static List<String> wrapped(Font font, String key, int wrapWidth) {
        if (key == null || key.isBlank()) {
            return List.of();
        }
        return wrappedText(font, I18n.get(key), wrapWidth);
    }

    private static List<String> wrappedText(Font font, String text, int wrapWidth) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (FormattedText line : font.getSplitter().splitLines(text, wrapWidth, Style.EMPTY)) {
            lines.add(line.getString());
        }
        return lines;
    }

    private static DetailsContent.Text detailsText(Font font, int wrapWidth) {
        return (key, uppercase) -> {
            if (key == null || key.isBlank()) {
                return List.of();
            }
            String value = I18n.get(key);
            return wrappedText(font, uppercase ? value.toUpperCase(Locale.ROOT) : value, wrapWidth);
        };
    }

    private List<DetailsItem> detailsItems(Font font, NavPresenter presenter, SettingMeta meta, Rect box) {
        int wrapWidth = DetailsLayout.textWidth(box);
        int capacity = DetailsLayout.lineCapacity(box);
        if (wrapWidth <= 0 || capacity <= 0) {
            return List.of();
        }
        String reasonKey = meta == null ? null : presenter.catalog().disabledReason(meta.id()).orElse(null);
        return DetailsContent.fit(meta, reasonKey, detailsText(font, wrapWidth), capacity);
    }

    private SettingMeta cardTarget(ShellLayout layout, NavPresenter presenter, int contentScroll,
                                   int mouseX, int mouseY, SettingId pinned, SettingId dragged) {
        SettingMeta held = settingById(presenter, dragged != null ? dragged : pinned);
        if (held != null) {
            return held;
        }
        SettingMeta hovered = hoveredSetting(layout, presenter, contentScroll, mouseX, mouseY);
        return hovered != null ? hovered : presenter.focusedSetting();
    }

    private SettingMeta hoveredSetting(ShellLayout layout, NavPresenter presenter, int contentScroll,
                                       int mouseX, int mouseY) {
        if (!layout.content().contains(mouseX, mouseY)) {
            return null;
        }
        List<SettingMeta> settings = presenter.settings();
        List<Rect> boxes = settingRowBoxes(layout, presenter, contentScroll);
        for (int i = 0; i < settings.size() && i < boxes.size(); i++) {
            if (boxes.get(i).contains(mouseX, mouseY)) {
                return settings.get(i);
            }
        }
        return null;
    }

    private static SettingMeta settingById(NavPresenter presenter, SettingId id) {
        if (id == null) {
            return null;
        }
        for (SettingMeta meta : presenter.settings()) {
            if (meta.id().equals(id)) {
                return meta;
            }
        }
        return null;
    }

    private Rect anchorOf(ShellLayout layout, NavPresenter presenter, int contentScroll, SettingMeta meta) {
        List<SettingMeta> settings = presenter.settings();
        List<Rect> boxes = settingRowBoxes(layout, presenter, contentScroll);
        for (int i = 0; i < settings.size() && i < boxes.size(); i++) {
            if (settings.get(i).id().equals(meta.id())) {
                return boxes.get(i);
            }
        }
        return Rect.EMPTY;
    }

    public Rect lastCard() {
        return lastCard;
    }

    public void renderCard(SurfacePainter painter, Font font, ShellLayout layout, NavPresenter presenter,
                           int contentScroll, int mouseX, int mouseY, SettingId pinned, SettingId dragged) {
        if (painter == null) {
            throw new IllegalArgumentException("painter must not be null");
        }
        requireInputs(font, layout, presenter);
        if (contentScroll < 0) {
            throw new IllegalArgumentException("contentScroll must not be negative: " + contentScroll);
        }
        this.lastCard = Rect.EMPTY;

        boolean sheet = layout.breakpoint() == Breakpoint.COMPACT;
        SettingMeta meta = sheet
                ? settingById(presenter, dragged != null ? dragged : pinned)
                : cardTarget(layout, presenter, contentScroll, mouseX, mouseY, pinned, dragged);
        if (meta == null) {
            return;
        }
        Rect bounds = layout.content();
        Rect anchor = anchorOf(layout, presenter, contentScroll, meta);
        if (bounds.isEmpty() || (!sheet && anchor.isEmpty())) {
            return;
        }

        int width = Math.min(ShellLayout.DETAILS_WIDTH, bounds.width() - TooltipLayout.MARGIN * 2);
        Rect probe = sheet
                ? DetailsLayout.sheet(bounds, bounds.height(), CARD_MARGIN)
                : new Rect(0, 0, width, TooltipLayout.availableHeight(anchor, bounds));
        List<DetailsItem> items = detailsItems(font, presenter, meta, probe);
        int titleLines = detailsText(font, DetailsLayout.textWidth(probe)).lines(meta.titleKey(), true).size();
        if (items.size() <= titleLines) {
            return;
        }

        int height = DetailsLayout.height(items.size());
        Rect box = sheet
                ? DetailsLayout.sheet(bounds, height, CARD_MARGIN)
                : TooltipLayout.placeBox(anchor, width, height, bounds);
        if (box.isEmpty()) {
            return;
        }

        int radius = sheet ? OVERLAY_RADIUS : TOOLTIP_RADIUS;
        paintRoundedFill(painter, box, radius, theme.color(ColorToken.SURFACE_CHROME));
        paintRoundedOutline(painter, box, radius, theme.color(ColorToken.BORDER_ACCENT));
        paintDetailItems(painter, box, items);
        painter.flush();
        this.lastCard = box;
    }

    private void paintDetailItems(SurfacePainter painter, Rect box, List<DetailsItem> items) {
        int capacity = DetailsLayout.lineCapacity(box);
        for (int index = 0; index < items.size() && index < capacity; index++) {
            DetailsItem item = items.get(index);
            if (item.isBlank()) {
                continue;
            }
            if (item.isBar()) {
                paintImpactBar(painter, box, index, item);
                continue;
            }
            int top = DetailsLayout.lineTop(box, index);
            int x = box.x() + DetailsLayout.PAD_X;
            String[] glyph = glyphOf(item.glyph());
            if (glyph != null) {
                SettingRowRenderer.paintGlyph(painter, new Rect(x, top, DETAILS_GLYPH, DetailsLayout.TEXT_HEIGHT),
                        glyph, theme.color(item.token()), true);
                x += DETAILS_GLYPH + DETAILS_GLYPH_GAP;
            }
            painter.text(x, top, item.text(), theme.color(item.token()), false);
        }
    }

    private void paintImpactBar(SurfacePainter painter, Rect box, int index, DetailsItem item) {
        Rect track = DetailsLayout.bar(box, index);
        if (track.isEmpty()) {
            return;
        }
        painter.fill(track, theme.color(ColorToken.IMPACT_TRACK));
        Rect fill = DetailsLayout.barFill(track, item.bar());
        if (fill.isEmpty()) {
            return;
        }
        if (item.accentBar()) {
            painter.gradient(fill, theme.color(ColorToken.ACCENT_BRIGHT), theme.color(ColorToken.ACCENT_DEEP));
        } else {
            painter.fill(fill, theme.color(ColorToken.IMPACT_VISUAL));
        }
    }

    private static String[] glyphOf(DetailsItem.Glyph glyph) {
        return switch (glyph) {
            case CHECK -> SettingRowRenderer.CHECK;
            case FLASK -> SettingRowRenderer.FLASK;
            case NONE -> null;
        };
    }

    public void renderSearchOverlay(SurfacePainter painter, Font font, ShellLayout layout, NavPresenter presenter,
                                    SearchResultsModel results, String query, int selected,
                                    int mouseX, int mouseY) {
        if (painter == null) {
            throw new IllegalArgumentException("painter must not be null");
        }
        requireInputs(font, layout, presenter);
        if (results == null) {
            throw new IllegalArgumentException("results must not be null");
        }
        if (query == null) {
            throw new IllegalArgumentException("query must not be null; use \"\"");
        }

        Rect panel = results.panel();
        if (panel.isEmpty()) {
            return;
        }

        painter.fill(layout.content(), theme.color(ColorToken.SURFACE_SUNKEN, SCRIM_ALPHA));
        paintRoundedFill(painter, panel, OVERLAY_RADIUS, theme.color(ColorToken.SURFACE_CHROME));
        paintRoundedOutline(painter, panel, OVERLAY_RADIUS, theme.color(ColorToken.BORDER_ACCENT));

        List<SearchResultsModel.Row> rows = results.rows();
        if (rows.isEmpty()) {
            paintCentredNotice(painter, font, panel,
                    query.isBlank() ? I18n.get(KEY_SEARCH_PROMPT) : I18n.get(KEY_SEARCH_EMPTY, query));
            painter.flush();
            return;
        }

        List<Rect> boxes = results.boxes();
        int hovered = results.indexAt(mouseX, mouseY);
        for (int i = 0; i < boxes.size(); i++) {
            Rect box = boxes.get(i);
            switch (rows.get(i)) {
                case SearchResultsModel.Header(SettingSource source) -> painter.text(
                        box.x() + OVERLAY_PAD_X, box.y() + (box.height() - TEXT_HEIGHT) / 2,
                        I18n.get(KEY_SEARCH_SOURCE + source.name().toLowerCase(Locale.ROOT))
                                .toUpperCase(Locale.ROOT),
                        theme.color(ColorToken.TEXT_FAINT), false);
                case SearchResultsModel.Hit(SearchIndex.Entry entry) ->
                        paintResult(painter, font, box, presenter, entry, i == selected, i == hovered);
            }
        }
        painter.flush();
    }

    private void paintResult(SurfacePainter painter, Font font, Rect box, NavPresenter presenter,
                             SearchIndex.Entry entry, boolean selected, boolean hovered) {
        if (selected || hovered) {
            paintRoundedFill(painter, box, RESULT_RADIUS, theme.color(ColorToken.SURFACE_CARD_HOVER));
        }
        if (selected) {
            paintRoundedOutline(painter, box, RESULT_RADIUS, theme.color(ColorToken.ACCENT));
        }

        int top = box.y() + (box.height() - TEXT_HEIGHT) / 2;
        String path = routeLabel(presenter, entry.route());
        int pathX = box.right() - OVERLAY_PAD_X - font.width(path);
        painter.text(pathX, top, path, theme.color(ColorToken.TEXT_MUTED), false);

        int titleX = box.x() + OVERLAY_PAD_X;
        painter.text(titleX, top, font.plainSubstrByWidth(entry.title(), Math.max(0, pathX - OVERLAY_PAD_X - titleX)),
                theme.color(selected ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_DEFAULT), false);
    }

    private static String routeLabel(NavPresenter presenter, RouteId route) {
        String leaf = label(presenter, route);
        RouteId parent = route.parent();
        return parent.depth() == 0 ? leaf : label(presenter, parent) + " " + BREADCRUMB_SEPARATOR + " " + leaf;
    }

    private void paintNav(GuiGraphics graphics, SurfacePainter painter, Rect region, NavPresenter presenter,
                          int scroll, int mouseX, int mouseY, long deltaMs) {
        if (region.isEmpty()) {
            return;
        }
        graphics.enableScissor(region.x(), region.y(), region.right(), region.bottom());
        try {
            paintSidebar(painter, region, presenter, scroll, mouseX, mouseY, deltaMs);
            painter.flush();
        } finally {
            graphics.disableScissor();
        }
    }

    public int sidebarEntryAt(Rect nav, SidebarModel model, int scroll, int mouseX, int mouseY) {
        if (nav == null) {
            throw new IllegalArgumentException("nav must not be null");
        }
        if (model == null) {
            throw new IllegalArgumentException("model must not be null");
        }
        SidebarViewport viewport = new SidebarViewport(nav, scroll);
        return viewport.contains(mouseX, mouseY) ? model.entryIndexAt(viewport.contentY(mouseY)) : -1;
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

    public Rect modScreenButton(Font font, ShellLayout layout, NavPresenter presenter) {
        requireInputs(font, layout, presenter);
        if (presenter.modScreen().isEmpty()) {
            return Rect.EMPTY;
        }
        List<Rect> rows = SettingRowLayout.rows(layout.content(), 1, 0, layout.breakpoint());
        if (rows.isEmpty()) {
            return Rect.EMPTY;
        }
        Rect row = rows.get(0);
        int width = Math.min(row.width(), font.width(I18n.get(KEY_MODS_OPEN)) + MOD_BUTTON_PAD_X * 2);
        return new Rect(row.x() + (row.width() - width) / 2, row.y(), width, row.height());
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

    private void paintChrome(SurfacePainter painter, ShellLayout layout, boolean drawerOpen,
                             boolean searchFocused) {
        painter.fill(layout.screen(), theme.color(ColorToken.SURFACE_BASE));
        painter.fill(layout.topBar(), theme.color(ColorToken.SURFACE_CHROME));

        if (layout.hasDetailsPanel()) {
            painter.fill(layout.details(), theme.color(ColorToken.SURFACE_CHROME));
        }

        int border = theme.color(ColorToken.BORDER_DEFAULT);
        if (!layout.sidebar().isEmpty()) {
            painter.fill(new Rect(layout.sidebar().right(), layout.content().y(), 1,
                    layout.content().height()), border);
        }
        if (layout.hasDetailsPanel()) {
            painter.fill(new Rect(layout.details().x(), layout.details().y(), 1, layout.details().height()), border);
        }
        painter.fill(new Rect(layout.topBar().x(), layout.topBar().bottom() - 1, layout.topBar().width(), 1), border);

        Rect menu = layout.menuButton();
        if (!menu.isEmpty()) {
            paintMenuIcon(painter, menu, theme.color(drawerOpen ? ColorToken.ACCENT : ColorToken.TEXT_SECONDARY));
        }

        paintSearchFrame(painter, layout.searchField(), searchFocused);
    }

    private void paintSearchFrame(SurfacePainter painter, Rect field, boolean focused) {
        if (field.isEmpty()) {
            return;
        }
        paintRoundedFill(painter, field, SEARCH_RADIUS, theme.color(ColorToken.SURFACE_CARD));
        paintRoundedOutline(painter, field, SEARCH_RADIUS,
                theme.color(focused ? ColorToken.ACCENT : ColorToken.BORDER_SUBTLE));
    }

    public Rect applyButton(ShellLayout layout, NavPresenter presenter) {
        requireBarInputs(layout, presenter);
        return ApplyBarModel.of(presenter.pending()).visible() ? layout.applyButton() : Rect.EMPTY;
    }

    public Rect discardButton(ShellLayout layout, NavPresenter presenter) {
        requireBarInputs(layout, presenter);
        return ApplyBarModel.of(presenter.pending()).visible() ? layout.discardButton() : Rect.EMPTY;
    }

    public int favoritesLabelWidth(Font font, ShellLayout layout) {
        return layout.breakpoint() == Breakpoint.COMPACT ? 0 : font.width(I18n.get(KEY_FAVORITES));
    }

    public Rect favoritesButton(Font font, ShellLayout layout) {
        return layout.favoritesButton(favoritesLabelWidth(font, layout));
    }

    private void paintFavoritesButton(SurfacePainter painter, Font font, ShellLayout layout,
                                      NavPresenter presenter, int mouseX, int mouseY) {
        Rect box = favoritesButton(font, layout);
        if (box.isEmpty()) {
            return;
        }
        boolean open = FAVORITES.equals(presenter.stack().current());
        boolean hovered = box.contains(mouseX, mouseY);
        if (open || hovered) {
            paintRoundedFill(painter, box, SEARCH_RADIUS, theme.color(ColorToken.SURFACE_NAV_ACTIVE));
        }
        paintRoundedOutline(painter, box, SEARCH_RADIUS,
                theme.color(open ? ColorToken.ACCENT : ColorToken.BORDER_DEFAULT));

        ColorToken token = open ? ColorToken.ACCENT : ColorToken.TEXT_SECONDARY;
        Rect star = new Rect(box.x() + ShellLayout.FAV_PAD, box.y(), ShellLayout.FAV_ICON, box.height());
        SettingRowRenderer.paintGlyph(painter, star.inset(3), SettingRowRenderer.STAR, theme.color(token), open);

        int labelWidth = favoritesLabelWidth(font, layout);
        if (labelWidth > 0 && box.width() > ShellLayout.FAV_PAD * 2 + ShellLayout.FAV_ICON) {
            painter.text(star.right() + ShellLayout.FAV_GAP, box.y() + (box.height() - TEXT_HEIGHT) / 2,
                    I18n.get(KEY_FAVORITES), theme.color(token), false);
        }
    }

    private void paintBrand(GuiGraphics graphics, ShellLayout layout) {
        Rect logo = layout.brandLogo();
        if (!logo.isEmpty()) {
            graphics.blit(LOGO, logo.x(), logo.y(), 0, 0, logo.width(), logo.height(),
                    logo.width(), logo.height());
        }
        Rect title = layout.brandTitle();
        if (!title.isEmpty()) {
            graphics.blit(TITLE, title.x(), title.y(), 0, 0, title.width(), title.height(),
                    title.width(), title.height());
        }
    }

    private void paintApplyBar(SurfacePainter painter, Font font, ShellLayout layout, NavPresenter presenter,
                               int mouseX, int mouseY) {
        ApplyBarModel bar = ApplyBarModel.of(presenter.pending());
        Rect region = layout.bottomBar();
        if (!bar.visible() || region.isEmpty()) {
            return;
        }
        painter.fill(region, theme.color(ColorToken.SURFACE_CHROME));
        painter.fill(new Rect(region.x(), region.y(), region.width(), 1),
                theme.color(ColorToken.BORDER_ACCENT));
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
                              int scroll, int mouseX, int mouseY, long deltaMs) {
        Gradient background = theme.gradient(ColorToken.SURFACE_BASE, ColorToken.SURFACE_SIDEBAR_BOTTOM);
        painter.gradient(sidebar, background.topArgb(), background.bottomArgb());

        SidebarModel model = presenter.sidebar();
        SidebarViewport viewport = new SidebarViewport(sidebar, scroll);
        RouteId activeRoute = presenter.activeSidebarRoute();
        int hoveredIndex = sidebarEntryAt(sidebar, model, scroll, mouseX, mouseY);
        String focusedId = focusedIn(presenter, NavPresenter.REGION_SIDEBAR);

        int first = model.firstVisible(scroll);
        int last = model.lastVisible(scroll, sidebar.height());
        for (int index = first; first >= 0 && index <= last; index++) {
            int height = model.heightOf(index);
            int top = viewport.screenTop(model.offsetOf(index));

            switch (model.entries().get(index)) {
                case SidebarModel.Section(String labelKey, boolean collapsed) -> {
                    boolean lit = index == hoveredIndex;
                    painter.text(sidebar.x() + SECTION_TEXT_X + CHEVRON + 4,
                            top + (height - TEXT_HEIGHT) / 2, I18n.get(labelKey).toUpperCase(Locale.ROOT),
                            theme.color(lit ? ColorToken.TEXT_SECONDARY : ColorToken.TEXT_FAINT), false);
                    SettingRowRenderer.paintGlyph(painter,
                            new Rect(sidebar.x() + SECTION_TEXT_X, top + (height - CHEVRON) / 2,
                                    CHEVRON, CHEVRON),
                            collapsed ? CHEVRON_RIGHT : CHEVRON_DOWN,
                            theme.color(lit ? ColorToken.TEXT_SECONDARY : ColorToken.TEXT_FAINT), true);
                }
                case SidebarModel.Row(RouteId route, String titleKey, int depth) -> {
                    Rect box = rowBox(sidebar, top, height);
                    boolean active = route.equals(activeRoute);
                    String key = route.toString();
                    paintRowSurface(painter, box, active,
                            hover.advance(key, index == hoveredIndex, deltaMs),
                            focus.advance(key, key.equals(focusedId), deltaMs));
                    ColorToken token = presenter.rowGreyed(route) ? ColorToken.TEXT_MUTED
                            : active ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_SECONDARY;
                    painter.text(sidebar.x() + ROW_TEXT_X + (depth - 1) * ROW_INDENT,
                            top + (height - TEXT_HEIGHT) / 2, I18n.get(titleKey), theme.color(token), false);
                }
            }
        }

        paintScrollIndicator(painter, ScrollIndicator.of(sidebar, model.totalHeight(), scroll));
    }

    private void paintRowSurface(SurfacePainter painter, Rect box, boolean active, float hovered, float focused) {
        if (active) {
            paintRoundedGradient(painter, box, NAV_RADIUS,
                    theme.color(ColorToken.SURFACE_NAV_ACTIVE), theme.color(ColorToken.SURFACE_SIDEBAR_BOTTOM));
            paintRoundedOutline(painter, box, NAV_RADIUS, theme.color(ColorToken.BORDER_ACCENT));
            paintLeadingEdge(painter, box, NAV_RADIUS, theme.color(ColorToken.ACCENT));
        } else if (hovered > 0.0f) {
            paintRoundedFill(painter, box, NAV_RADIUS, theme.color(ColorToken.SURFACE_CARD_HOVER, hovered));
        }
        if (focused > 0.0f) {
            paintRoundedOutline(painter, box, NAV_RADIUS, theme.color(ColorToken.ACCENT, focused));
        }
    }

    private void paintContent(SurfacePainter painter, Font font, ShellLayout layout, NavPresenter presenter,
                              int contentScroll, int mouseX, int mouseY, SettingId dragged, long deltaMs) {
        Rect content = layout.content();
        if (content.isEmpty()) {
            return;
        }

        if (OVERVIEW.equals(presenter.stack().current())) {
            paintOverview(painter, font, layout, presenter, contentScroll, mouseX, mouseY);
        } else {
            paintBreadcrumbs(painter, font, layout, presenter);
            painter.text(content.x() + CONTENT_PAD_X, content.y() + TITLE_Y,
                    I18n.get(presenter.currentTitleKey()), theme.color(ColorToken.TEXT_PRIMARY), false);
            paintTabStrip(painter, font, layout, presenter);
            paintSettings(painter, font, layout, presenter, contentScroll, mouseX, mouseY, dragged, deltaMs);
        }
        paintScrollIndicator(painter, layout, presenter, contentScroll);
    }

    public List<Rect> presetCardBoxes(ShellLayout layout, NavPresenter presenter, int scroll) {
        if (layout == null || presenter == null) {
            throw new IllegalArgumentException("layout and presenter must not be null");
        }
        return PresetCardLayout.page(layout.content(), presenter.presetCards().size(), scroll,
                layout.breakpoint()).cards();
    }

    private void paintOverview(SurfacePainter painter, Font font, ShellLayout layout, NavPresenter presenter,
                               int contentScroll, int mouseX, int mouseY) {
        List<PresetCardModel.Card> cards = presenter.presetCards();
        PresetCardLayout.Page page = PresetCardLayout.page(layout.content(), cards.size(),
                contentScroll, layout.breakpoint());
        paintProfilesHeader(painter, font, page.header());
        for (int i = 0; i < cards.size() && i < page.cards().size(); i++) {
            paintPresetCard(painter, font, page.cards().get(i), cards.get(i),
                    page.cards().get(i).contains(mouseX, mouseY));
        }
        paintSuggestionLine(painter, font, page.suggestion(), presenter);
    }

    private void paintProfilesHeader(SurfacePainter painter, Font font, Rect header) {
        if (header.isEmpty()) {
            return;
        }
        painter.gradient(new Rect(header.x(), header.y(), PresetCardLayout.HEADER_BAR, header.height()),
                theme.color(ColorToken.ACCENT_BRIGHT), theme.color(ColorToken.ACCENT_DEEP));

        int left = header.x() + PresetCardLayout.HEADER_BAR + 9;
        int width = header.right() - left;
        painter.text(left, header.y(), I18n.get(KEY_PROFILES), theme.color(ColorToken.TEXT_PRIMARY), false);
        paintSmallLines(painter, font,
                new Rect(left, header.y() + 12, width, PresetCardLayout.SMALL_LINE * 2),
                I18n.get(KEY_PROFILES_INTRO), ColorToken.TEXT_MUTED);
        paintSmallLines(painter, font,
                new Rect(left, header.y() + 29, width, PresetCardLayout.SMALL_LINE),
                I18n.get(KEY_PROFILES_LEGEND), ColorToken.TEXT_FAINT);
    }

    private void paintSuggestionLine(SurfacePainter painter, Font font, Rect box, NavPresenter presenter) {
        if (box.isEmpty()) {
            return;
        }
        String suggested = OverviewSignals.suggestedPresetKey(presenter.playingProfileKey());
        String text = suggested == null
                ? I18n.get(KEY_SUGGEST_WAIT)
                : I18n.get(KEY_SUGGEST, I18n.get(suggested));
        float scale = painter.smallScale();
        int drawn = Math.round(font.width(text) * scale);
        painter.smallText(box.x() + Math.max(0, (box.width() - drawn) / 2), box.y(),
                trimToWidth(font, text, Math.round(box.width() / scale)),
                theme.color(suggested == null ? ColorToken.TEXT_FAINT : ColorToken.TEXT_SECONDARY));
    }

    private void paintPresetCard(SurfacePainter painter, Font font, Rect card, PresetCardModel.Card model,
                                 boolean hovered) {
        if (card.isEmpty()) {
            return;
        }
        PresetCardLayout.Slots slots = PresetCardLayout.slots(card,
                card.height() >= PresetCardLayout.CARD_HEIGHT);
        if (slots.name().isEmpty()) {
            return;
        }
        boolean lit = hovered && model.selectable();
        ColorToken edge = model.playing() ? ColorToken.ACCENT
                : model.staged() ? ColorToken.WARNING
                : model.suggested() ? ColorToken.BORDER_ACCENT
                : ColorToken.BORDER_SUBTLE;

        paintRoundedFill(painter, card, SettingRowLayout.CARD_RADIUS,
                theme.color(lit ? ColorToken.SURFACE_CARD_HOVER : ColorToken.SURFACE_CARD));
        if (model.playing() || model.staged() || model.suggested()) {
            paintRoundedGradient(painter, card, SettingRowLayout.CARD_RADIUS,
                    theme.color(edge, 0.10f), theme.color(ColorToken.SURFACE_CARD, 0.0f));
        }
        paintRoundedOutline(painter, card, SettingRowLayout.CARD_RADIUS, theme.color(edge));
        paintAccentStripe(painter, card, slots.accent(), theme.color(edge));

        String[] icon = PresetIcons.of(model.key());
        int iconGap = 0;
        if (icon != null && slots.name().width() > PresetIcons.SIZE + 30) {
            SettingRowRenderer.paintGlyph(painter,
                    new Rect(slots.name().x(), slots.name().y() + ICON_RISE, PresetIcons.SIZE, PresetIcons.SIZE),
                    icon, theme.color(model.playing() ? ColorToken.ACCENT_BRIGHT : ColorToken.TEXT_SECONDARY),
                    true);
            iconGap = PresetIcons.SIZE + 5;
        }
        String name = I18n.get(model.key()).toUpperCase(Locale.ROOT);
        String badge = model.playing() ? I18n.get(KEY_PLAYING_NOW)
                : model.staged() ? I18n.get(KEY_PENDING)
                : model.suggested() ? I18n.get(KEY_SUGGESTED) : null;
        int badgeWidth = badge == null ? 0 : smallWidth(painter, font, badge) + 4;
        painter.text(slots.name().x() + iconGap, slots.name().y(),
                trimToWidth(font, name, slots.name().width() - badgeWidth - iconGap),
                theme.color(model.playing() ? ColorToken.ACCENT_BRIGHT : ColorToken.TEXT_PRIMARY), false);
        if (badge != null) {
            painter.smallText(slots.badge().right() - badgeWidth + 4, slots.badge().y(), badge,
                    theme.color(model.playing() ? ColorToken.SUCCESS
                            : model.staged() ? ColorToken.WARNING : ColorToken.ACCENT));
        }

        paintSmallLines(painter, font, slots.blurb(), I18n.get(model.key() + ".card"),
                ColorToken.TEXT_MUTED);
        if (!slots.changes().isEmpty()) {
            paintSmallLines(painter, font, slots.changes(), I18n.get(model.key() + ".changes"),
                    ColorToken.TEXT_SECONDARY);
        }

        PresetRating.Rating rating = PresetRating.of(model.key());
        if (rating != null) {
            paintRatingBar(painter, font, slots.framesBar(), KEY_FRAMES, rating.frames(), true);
            paintRatingBar(painter, font, slots.looksBar(), KEY_LOOKS, rating.looks(), false);
        }

        String measured = OverviewSignals.fpsOf(model.key());
        painter.smallText(slots.measured().x(), slots.measured().y(),
                measured != null ? measured : I18n.get(KEY_NOT_TRIED),
                theme.color(measured != null ? ColorToken.SUCCESS : ColorToken.TEXT_FAINT));
    }

    private void paintAccentStripe(SurfacePainter painter, Rect card, Rect stripe, int argb) {
        for (Rect span : RoundedScanline.fillSpans(card, SettingRowLayout.CARD_RADIUS)) {
            int width = Math.min(stripe.width(), span.width());
            if (width > 0) {
                painter.fill(new Rect(span.x(), span.y(), width, span.height()), argb);
            }
        }
    }

    private void paintSmallLines(SurfacePainter painter, Font font, Rect box, String text, ColorToken token) {
        if (box.isEmpty()) {
            return;
        }
        float scale = painter.smallScale();
        int wrapWidth = Math.round(box.width() / scale);
        List<String> lines = wrappedText(font, text, wrapWidth);
        int rows = Math.max(1, box.height() / PresetCardLayout.SMALL_LINE);
        for (int i = 0; i < lines.size() && i < rows; i++) {
            String line = i == rows - 1 && lines.size() > rows
                    ? trimToWidth(font, lines.get(i) + "…", wrapWidth)
                    : lines.get(i);
            painter.smallText(box.x(), box.y() + i * PresetCardLayout.SMALL_LINE, line, theme.color(token));
        }
    }

    private void paintRatingBar(SurfacePainter painter, Font font, Rect track, String labelKey,
                                int level, boolean accent) {
        if (track.isEmpty()) {
            return;
        }
        painter.smallText(track.x() - PresetCardLayout.BAR_LABEL_WIDTH, track.y() - 2, I18n.get(labelKey),
                theme.color(ColorToken.TEXT_FAINT));
        painter.fill(track, theme.color(ColorToken.IMPACT_TRACK));
        Rect fill = PresetCardLayout.barFill(track, level, PresetRating.LEVELS);
        if (!fill.isEmpty()) {
            if (accent) {
                painter.gradient(fill, theme.color(ColorToken.ACCENT_BRIGHT), theme.color(ColorToken.ACCENT_DEEP));
            } else {
                painter.fill(fill, theme.color(ColorToken.TEXT_MUTED));
            }
        }
        painter.smallText(track.right() + 5, track.y() - 2, I18n.get(labelKey + "." + level),
                theme.color(ColorToken.TEXT_SECONDARY));
    }

    private static int smallWidth(SurfacePainter painter, Font font, String text) {
        return Math.round(font.width(text) * painter.smallScale());
    }

    private static String smallTrim(SurfacePainter painter, Font font, String text, int width) {
        float scale = painter.smallScale();
        return trimToWidth(font, text, Math.round(width / scale));
    }

    private static String trimToWidth(Font font, String text, int width) {
        if (width <= 0 || font.width(text) <= width) {
            return text;
        }
        String cut = text;
        while (!cut.isEmpty() && font.width(cut + "…") > width) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut.isEmpty() ? text : cut + "…";
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
                               int contentScroll, int mouseX, int mouseY, SettingId dragged, long deltaMs) {
        List<SettingMeta> settings = presenter.settings();
        if (settings.isEmpty() && paintEmptyPage(painter, font, layout, presenter, mouseX, mouseY)) {
            return;
        }

        List<Rect> boxes = settingRowBoxes(layout, presenter, contentScroll);
        SettingsCatalog catalog = presenter.catalog();
        String focusedId = focusedIn(presenter, NavPresenter.REGION_CONTENT);
        SettingMeta pointed = hoveredSetting(layout, presenter, contentScroll, mouseX, mouseY);
        for (int i = 0; i < boxes.size(); i++) {
            Rect box = boxes.get(i);
            SettingMeta meta = settings.get(i);
            SettingBinding binding = catalog.binding(meta.id());
            String key = meta.id().toString();
            boolean onRow = meta.equals(pointed);
            rowRenderer.render(painter, font, box, meta, binding, presenter.valueOf(meta),
                    catalog.enabled(meta.id()),
                    hover.advance(key, onRow || meta.id().equals(dragged), deltaMs),
                    presenter.resettable(meta),
                    onRow && SettingRowLayout.resetBox(box).contains(mouseX, mouseY),
                    presenter.isFavorite(meta.id()),
                    onRow && SettingRowLayout.starBox(box).contains(mouseX, mouseY),
                    onRow && SettingRowLayout.cyclerPrevBox(box).contains(mouseX, mouseY),
                    onRow && SettingRowLayout.cyclerNextBox(box).contains(mouseX, mouseY));

            float focused = focus.advance(key, key.equals(focusedId), deltaMs);
            if (focused > 0.0f) {
                paintRoundedOutline(painter, SettingRowLayout.cardBox(box), SettingRowLayout.CARD_RADIUS,
                        theme.color(ColorToken.ACCENT, focused));
            }
        }
    }

    private boolean paintEmptyPage(SurfacePainter painter, Font font, ShellLayout layout, NavPresenter presenter,
                                   int mouseX, int mouseY) {
        RouteId current = presenter.stack().current();
        if (FAVORITES.equals(current)) {
            paintCentredNotice(painter, font, layout.content(), I18n.get(KEY_FAVORITES_EMPTY));
            return true;
        }
        if (MODS.equals(current)) {
            paintCentredNotice(painter, font, layout.content(), I18n.get(KEY_MODS_EMPTY));
            return true;
        }
        if (PLUGINS.equals(current)) {
            paintPluginsPage(painter, font, layout, presenter);
            return true;
        }
        if (PLUGINS.equals(current.parent())) {
            paintCentredNotice(painter, font, layout.content(), I18n.get(KEY_PLUGIN_EMPTY));
            return true;
        }

        Rect button = modScreenButton(font, layout, presenter);
        if (button.isEmpty()) {
            return false;
        }
        paintBarButton(painter, font, button, I18n.get(KEY_MODS_OPEN), ColorToken.BORDER_STRONG, mouseX, mouseY);
        if (presenter.modScreenFocused()) {
            paintRoundedOutline(painter, button, BAR_BUTTON_RADIUS, theme.color(ColorToken.ACCENT));
        }
        paintCentredNotice(painter, font, new Rect(layout.content().x(), button.bottom() + MOD_NOTE_GAP,
                layout.content().width(), TEXT_HEIGHT), I18n.get(KEY_MODS_PASSTHROUGH));
        return true;
    }

    private void paintPluginsPage(SurfacePainter painter, Font font, ShellLayout layout,
                                  NavPresenter presenter) {
        Rect content = layout.content();
        if (content.isEmpty()) {
            return;
        }
        List<NavPresenter.PluginPage> plugins = presenter.pluginPages();
        List<String> mods = presenter.catalog().modIds();
        if (plugins.isEmpty() && mods.isEmpty()) {
            paintCentredNotice(painter, font, content, I18n.get(KEY_PLUGINS_EMPTY));
            return;
        }

        int left = content.x() + CONTENT_PAD_X;
        int line = content.y() + PLUGIN_LIST_TOP;
        if (!plugins.isEmpty()) {
            painter.smallText(left, line, I18n.get(KEY_PLUGINS_INSTALLED),
                    theme.color(ColorToken.TEXT_FAINT));
            line += 12;
            for (NavPresenter.PluginPage plugin : plugins) {
                painter.text(left, line, plugin.name(),
                        theme.color(plugin.enabled() ? ColorToken.TEXT_DEFAULT : ColorToken.TEXT_MUTED), false);
                if (!plugin.enabled()) {
                    painter.smallText(left + font.width(plugin.name()) + 6, line + 1,
                            I18n.get(KEY_PLUGINS_DISABLED), theme.color(ColorToken.WARNING));
                }
                line += 13;
            }
            line += 8;
        }
        if (!mods.isEmpty()) {
            painter.smallText(left, line, I18n.get(KEY_PLUGINS_MODS), theme.color(ColorToken.TEXT_FAINT));
            line += 12;
            for (String modId : mods) {
                painter.text(left, line, NavPresenter.modName(modId),
                        theme.color(ColorToken.TEXT_SECONDARY), false);
                line += 13;
            }
        }
    }

    private void paintCentredNotice(SurfacePainter painter, Font font, Rect content, String text) {
        if (content.isEmpty()) {
            return;
        }
        painter.text(content.x() + (content.width() - font.width(text)) / 2,
                content.y() + (content.height() - TEXT_HEIGHT) / 2, text,
                theme.color(ColorToken.TEXT_MUTED), false);
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

    static int lerpArgb(int from, int to, float progress) {
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
