package net.vulkanmod.config.ui.shell;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.ui.core.BreadcrumbModel;
import net.vulkanmod.config.ui.core.FocusHandoff;
import net.vulkanmod.config.ui.core.KeyAction;
import net.vulkanmod.config.ui.core.ProfileChipRow;
import net.vulkanmod.config.ui.core.PresetCardLayout;
import net.vulkanmod.config.ui.core.PresetCardModel;
import net.vulkanmod.config.ui.core.Rect;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.SearchIndex;
import net.vulkanmod.config.ui.core.SearchResultsModel;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingType;
import net.vulkanmod.config.ui.core.SettingRowLayout;
import net.vulkanmod.config.ui.core.ShellLayout;
import net.vulkanmod.config.ui.core.SidebarModel;
import net.vulkanmod.config.ui.core.SliderGeometry;
import net.vulkanmod.config.ui.core.TabStripModel;
import net.vulkanmod.config.ui.core.Theme;
import net.vulkanmod.config.ui.mods.ModScreens;
import net.vulkanmod.config.ui.render.SurfacePainter;
import net.vulkanmod.config.ui.settings.SettingBinding;

import java.util.List;
import java.util.Optional;

public class VolcanicScreen extends Screen {
    private static final int SIDEBAR_SCROLL_STEP = 25;
    private static final int CONTENT_SCROLL_STEP = 16;
    private static final int PRIMARY_BUTTON = 0;
    private static final long NANOS_PER_MS = 1_000_000L;
    private static final Theme THEME = Theme.volcanic();

    private final Screen parent;
    private final NavPresenter presenter = new NavPresenter();
    private final ShellRenderer renderer = new ShellRenderer(THEME);
    private ShellLayout layout = ShellLayout.of(0, 0);
    private SearchIndex searchIndex;
    private SearchField search;
    private int sidebarScroll;
    private int contentScroll;
    private RouteId scrolledRoute;
    private SettingId dragged;
    private SettingId pinned;
    private boolean drawerOpen;
    private int searchSelection = -1;
    private long frameNanos;

    public VolcanicScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.layout = ShellLayout.of(this.width, this.height);
        if (!layout.hasDrawer()) {
            this.drawerOpen = false;
        }
        if (!isNavVisible() && NavPresenter.REGION_SIDEBAR.equals(presenter.focus().activeRegion())) {
            FocusHandoff.enter(presenter.focus(), NavPresenter.REGION_CONTENT,
                    presenter.stack().current().toString());
        }
        this.sidebarScroll = presenter.sidebar().clampScroll(this.sidebarScroll, navViewport().height());
        initSearch();
    }

    private void initSearch() {
        String query = search == null ? "" : search.query();
        this.search = null;
        Rect box = layout.searchField();
        if (box.isEmpty()) {
            return;
        }
        if (searchIndex == null) {
            this.searchIndex = SearchField.indexOf(presenter.catalog());
        }
        SearchField field = new SearchField(this.font, box, searchIndex, THEME);
        field.setQuery(query);
        addRenderableWidget(field.widget());
        this.search = field;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        syncContentScroll();
        this.sidebarScroll = presenter.sidebar().clampScroll(this.sidebarScroll, navViewport().height());
        SurfacePainter painter = SurfacePainter.create(guiGraphics, this.font);
        long deltaMs = frameDeltaMs();
        guiGraphics.drawManaged(() -> {
            renderer.render(guiGraphics, painter, this.font, layout, presenter, sidebarScroll, contentScroll,
                    mouseX, mouseY, dragged, drawerOpen, searchFocused(), deltaMs);
            if (searchFocused()) {
                renderer.renderSearchOverlay(painter, this.font, layout, presenter, searchResults(),
                        search.query(), searchSelection, mouseX, mouseY);
            } else if (!layout.hasDetailsPanel() && !drawerOpen) {
                renderer.renderCard(painter, this.font, layout, presenter, contentScroll, mouseX, mouseY,
                        pinned, dragged);
            }
        });
        if (search != null) {
            search.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    private long frameDeltaMs() {
        long now = System.nanoTime();
        long previous = this.frameNanos;
        if (previous == 0L || now <= previous) {
            this.frameNanos = now;
            return 0L;
        }
        long elapsed = now - previous;
        this.frameNanos = now - elapsed % NANOS_PER_MS;
        return elapsed / NANOS_PER_MS;
    }

    private SearchResultsModel searchResults() {
        return SearchResultsModel.of(search == null ? List.of() : search.results(), layout.content());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != PRIMARY_BUTTON) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int x = (int) mouseX;
        int y = (int) mouseY;
        if (search != null && layout.searchField().contains(x, y)) {
            focusSearch(true);
            search.click(mouseX, mouseY, button);
            return true;
        }
        if (searchFocused()) {
            return clickSearchResult(x, y);
        }
        if (renderer.favoritesButton(this.font, layout).contains(x, y)) {
            presenter.openFavorites();
            return true;
        }
        if (layout.menuButton().contains(x, y)) {
            setDrawerOpen(!drawerOpen);
            return true;
        }
        if (applyBarVisible() && layout.bottomBar().contains(x, y)) {
            clickApplyBar(x, y);
            return true;
        }
        if (clickSidebar(x, y)) {
            return true;
        }
        if (drawerOpen) {
            setDrawerOpen(false);
            return true;
        }
        if (renderer.lastCard().contains(x, y)) {
            return true;
        }
        if (!presenter.isOverview() && (clickTabStrip(x, y) || clickBreadcrumb(x, y))
                || clickPresetCard(x, y) || clickModScreen(x, y) || clickSettingRow(x, y)) {
            return true;
        }
        this.pinned = null;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == PRIMARY_BUTTON && dragSlider((int) mouseX)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == PRIMARY_BUTTON && dragged != null) {
            this.dragged = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (dragged != null || searchFocused()) {
            return true;
        }

        int direction = (int) Math.signum(scrollY);
        if (direction == 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        Rect nav = layout.sidebarOrDrawer(drawerOpen);
        if (nav.contains((int) mouseX, (int) mouseY)) {
            this.sidebarScroll = presenter.sidebar()
                    .clampScroll(this.sidebarScroll - direction * SIDEBAR_SCROLL_STEP, nav.height());
            return true;
        }
        if (!drawerOpen && layout.content().contains((int) mouseX, (int) mouseY)) {
            this.contentScroll = Math.min(Math.max(0, this.contentScroll - direction * CONTENT_SCROLL_STEP),
                    maxContentScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        KeyAction action = UiKeys.actionFor(keyCode, modifiers);
        if (action == KeyAction.SEARCH && search != null) {
            focusSearch(true);
            search.selectAll();
            return true;
        }
        if (searchFocused()) {
            return searchKeyPressed(action, keyCode, scanCode, modifiers);
        }
        switch (action) {
            case CLOSE -> {
                if (pinned != null) {
                    this.pinned = null;
                    return true;
                }
                if (drawerOpen) {
                    setDrawerOpen(false);
                } else {
                    this.onClose();
                }
                return true;
            }
            case BACK -> {
                if (drawerOpen) {
                    setDrawerOpen(false);
                    return true;
                }
                if (!presenter.stack().canGoBack()) {
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
                presenter.back();
                return true;
            }
            case NEXT, PREVIOUS, UP, DOWN, HOME, END -> {
                presenter.focus().apply(action);
                revealFocusedSetting();
                pinFocused();
                return true;
            }
            case ACTIVATE -> {
                RouteId route = presenter.focusedRoute();
                if (route != null) {
                    select(route, presenter.focus().activeRegion());
                    return true;
                }
                if (presenter.modScreenFocused()) {
                    openModScreen();
                    return true;
                }
                if (presenter.favoritesFocused()) {
                    presenter.openFavorites();
                    return true;
                }
                SettingMeta focused = presenter.focusedSetting();
                if (focused == null) {
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
                presenter.activate(focused);
                return true;
            }
            case INCREASE, DECREASE -> {
                SettingMeta focused = presenter.focusedSetting();
                if (focused == null) {
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
                presenter.step(focused, action == KeyAction.INCREASE ? 1 : -1);
                return true;
            }
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    private boolean searchKeyPressed(KeyAction action, int keyCode, int scanCode, int modifiers) {
        if (action == KeyAction.CLOSE) {
            search.setQuery("");
            focusSearch(false);
            return true;
        }
        if (action == KeyAction.NEXT || action == KeyAction.PREVIOUS) {
            focusSearch(false);
            return true;
        }
        if (action == KeyAction.UP || action == KeyAction.DOWN) {
            this.searchSelection = searchResults()
                    .nextHit(searchSelection, action == KeyAction.DOWN ? 1 : -1);
            return true;
        }
        if (action == KeyAction.ACTIVATE) {
            openSelectedResult();
            return true;
        }
        search.keyPressed(keyCode, scanCode, modifiers);
        this.searchSelection = -1;
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        boolean handled = super.charTyped(codePoint, modifiers);
        if (searchFocused()) {
            this.searchSelection = -1;
        }
        return handled;
    }

    private boolean clickSearchResult(int mouseX, int mouseY) {
        SearchResultsModel results = searchResults();
        Optional<SearchIndex.Entry> hit = results.hitAt(results.indexAt(mouseX, mouseY));
        if (hit.isEmpty()) {
            focusSearch(false);
            return true;
        }
        openResult(hit.get());
        return true;
    }

    private void openSelectedResult() {
        SearchResultsModel results = searchResults();
        results.hitAt(searchSelection).or(() -> results.hitAt(results.firstHit())).ifPresent(this::openResult);
    }

    private void openResult(SearchIndex.Entry entry) {
        focusSearch(false);
        presenter.reveal(entry.id());
        syncContentScroll();
        revealFocusedSetting();
    }

    private boolean searchFocused() {
        return search != null && search.isFocused();
    }

    private void focusSearch(boolean focused) {
        setFocused(focused ? search.widget() : null);
        this.searchSelection = -1;
    }

    @Override
    public void onClose() {
        presenter.discard();
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean clickSidebar(int mouseX, int mouseY) {
        Rect nav = layout.sidebarOrDrawer(drawerOpen);
        if (!nav.contains(mouseX, mouseY)) {
            return false;
        }

        int index = renderer.sidebarEntryAt(nav, presenter.sidebar(), sidebarScroll, mouseX, mouseY);
        SidebarModel.Entry entry = presenter.sidebar().entryAt(index);
        if (entry == null) {
            return true;
        }
        switch (entry) {
            case SidebarModel.Section(String labelKey, boolean collapsed) -> {
                presenter.toggleSection(labelKey);
                this.sidebarScroll = presenter.sidebar().clampScroll(this.sidebarScroll, nav.height());
            }
            case SidebarModel.Row(RouteId route, String titleKey, int depth) -> {
                select(route, NavPresenter.REGION_SIDEBAR);
                setDrawerOpen(false);
            }
        }
        return true;
    }

    private boolean clickApplyBar(int mouseX, int mouseY) {
        if (renderer.applyButton(layout, presenter).contains(mouseX, mouseY)) {
            presenter.apply();
            return true;
        }
        if (renderer.discardButton(layout, presenter).contains(mouseX, mouseY)) {
            presenter.discard();
            return true;
        }
        return false;
    }

    private boolean clickTabStrip(int mouseX, int mouseY) {
        List<Rect> boxes = renderer.tabStripBoxes(this.font, layout, presenter);
        int index = TabStripModel.indexAt(boxes, mouseX, mouseY);
        if (index < 0) {
            return false;
        }

        select(presenter.subTabs().get(index).route(), NavPresenter.REGION_CONTENT);
        return true;
    }

    private boolean applyBarVisible() {
        return !presenter.pending().isEmpty();
    }

    private int maxContentScroll() {
        Rect content = layout.content();
        int reserve = presenter.pending().isEmpty() ? 0 : layout.overlayReserve();
        if (presenter.isOverview()) {
            PresetCardLayout.Page page = PresetCardLayout.page(content, presenter.presetCards().size(),
                    0, layout.breakpoint());
            return page.centred() || content.isEmpty()
                    ? 0
                    : Math.max(0, page.height() + reserve - content.height());
        }
        return SettingRowLayout.maxScroll(content, presenter.contentRowCount(), layout.breakpoint()) + reserve;
    }

    private boolean clickPresetCard(int mouseX, int mouseY) {
        if (!presenter.stack().current().equals(RouteId.parse("overview"))) {
            return false;
        }
        List<PresetCardModel.Card> cards = presenter.presetCards();
        List<Rect> boxes = renderer.presetCardBoxes(layout, presenter, contentScroll);
        for (int index = 0; index < cards.size() && index < boxes.size(); index++) {
            if (!boxes.get(index).contains(mouseX, mouseY)) {
                continue;
            }
            if (!cards.get(index).selectable()) {
                return true;
            }
            presenter.applyProfile(cards.get(index).key());
            return true;
        }
        return false;
    }

    private boolean clickModScreen(int mouseX, int mouseY) {
        if (!renderer.modScreenButton(this.font, layout, presenter).contains(mouseX, mouseY)) {
            return false;
        }
        openModScreen();
        return true;
    }

    private void openModScreen() {
        if (this.minecraft == null) {
            return;
        }
        presenter.modScreen()
                .flatMap(modId -> ModScreens.screenOf(modId, this))
                .ifPresent(this.minecraft::setScreen);
    }

    private boolean clickSettingRow(int mouseX, int mouseY) {
        List<Rect> boxes = renderer.settingRowBoxes(layout, presenter, contentScroll);
        int index = TabStripModel.indexAt(boxes, mouseX, mouseY);
        if (index < 0 || index >= presenter.settings().size()) {
            return false;
        }

        SettingMeta meta = presenter.settings().get(index);
        presenter.focus().focusRegion(NavPresenter.REGION_CONTENT);
        presenter.focus().ring(NavPresenter.REGION_CONTENT).focus(meta.id().toString());
        this.pinned = meta.id();
        if (SettingRowLayout.starBox(boxes.get(index)).contains(mouseX, mouseY)) {
            presenter.toggleFavorite(meta.id());
            return true;
        }
        if (SettingRowLayout.resetBox(boxes.get(index)).contains(mouseX, mouseY) && presenter.reset(meta)) {
            return true;
        }

        if (meta.type() == SettingType.ENUM && presenter.catalog().enabled(meta.id())) {
            if (SettingRowLayout.cyclerPrevBox(boxes.get(index)).contains(mouseX, mouseY)) {
                presenter.step(meta, -1);
                return true;
            }
            if (SettingRowLayout.cyclerNextBox(boxes.get(index)).contains(mouseX, mouseY)) {
                presenter.step(meta, 1);
                return true;
            }
        }

        if (meta.type().slider() && presenter.catalog().enabled(meta.id())) {
            Rect track = ShellRenderer.sliderTrack(boxes.get(index));
            if (track.contains(mouseX, mouseY)) {
                this.dragged = meta.id();
                applySlider(meta, track, mouseX);
                return true;
            }
        }

        presenter.activate(meta);
        return true;
    }

    private boolean dragSlider(int mouseX) {
        if (dragged == null) {
            return false;
        }

        List<SettingMeta> settings = presenter.settings();
        List<Rect> boxes = renderer.settingRowBoxes(layout, presenter, contentScroll);
        for (int index = 0; index < settings.size() && index < boxes.size(); index++) {
            SettingMeta meta = settings.get(index);
            if (meta.id().equals(dragged)) {
                applySlider(meta, ShellRenderer.sliderTrack(boxes.get(index)), mouseX);
                return true;
            }
        }

        this.dragged = null;
        return false;
    }

    private void applySlider(SettingMeta meta, Rect track, int mouseX) {
        if (track.isEmpty()) {
            return;
        }

        SettingBinding binding = presenter.catalog().binding(meta.id());
        presenter.set(meta, SliderGeometry.valueAt(track, mouseX, binding.min(), binding.max(), binding.step()));
    }

    private boolean clickBreadcrumb(int mouseX, int mouseY) {
        List<Rect> segments = renderer.breadcrumbBoxes(this.font, layout, presenter);
        int index = BreadcrumbModel.indexAt(segments, mouseX, mouseY);
        if (index < 0) {
            return false;
        }

        presenter.navigate(presenter.stack().trail().get(index));
        return true;
    }

    private void pinFocused() {
        SettingMeta focused = presenter.focusedSetting();
        this.pinned = focused == null ? null : focused.id();
    }

    private void select(RouteId route, String regionId) {
        presenter.navigate(route);
        presenter.focus().focusRegion(regionId);
        presenter.focus().ring(regionId).focus(route.toString());
    }

    private void setDrawerOpen(boolean open) {
        if (!layout.hasDrawer()) {
            this.drawerOpen = false;
            return;
        }
        this.drawerOpen = open;
        if (open) {
            FocusHandoff.enter(presenter.focus(), NavPresenter.REGION_SIDEBAR,
                    presenter.activeSidebarRoute().toString());
        } else {
            FocusHandoff.enter(presenter.focus(), NavPresenter.REGION_CONTENT,
                    presenter.stack().current().toString());
        }
    }

    private void revealFocusedSetting() {
        SettingMeta focused = presenter.focusedSetting();
        if (focused == null) {
            return;
        }
        List<SettingMeta> settings = presenter.settings();
        this.contentScroll = SettingRowLayout.scrollToReveal(layout.content(), settings.size(),
                settings.indexOf(focused), contentScroll, layout.breakpoint());
    }

    private void syncContentScroll() {
        RouteId current = presenter.stack().current();
        if (!current.equals(scrolledRoute)) {
            this.scrolledRoute = current;
            this.contentScroll = 0;
        }
        this.contentScroll = SettingRowLayout.clampScroll(this.contentScroll, layout.content(),
                presenter.contentRowCount(), layout.breakpoint());
    }

    private Rect navViewport() {
        return layout.sidebarOrDrawer(true);
    }

    private boolean isNavVisible() {
        return !layout.sidebarOrDrawer(drawerOpen).isEmpty();
    }
}
