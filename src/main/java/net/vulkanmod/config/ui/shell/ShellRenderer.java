package net.vulkanmod.config.ui.shell;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
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
import net.vulkanmod.config.ui.core.FrameGraphLayout;
import net.vulkanmod.config.ui.core.InfoRowLayout;
import net.vulkanmod.config.ui.settings.SystemReport;
import net.vulkanmod.config.ui.core.FrameHistory;
import net.vulkanmod.render.profiling.StackSampler;
import net.vulkanmod.config.ui.settings.Diagnosis;
import net.vulkanmod.config.ui.settings.StatsReport;
import net.vulkanmod.vulkan.FrameTimer;
import net.vulkanmod.config.ui.core.PageHeader;
import net.vulkanmod.config.ui.core.PluginPageLayout;
import net.vulkanmod.config.ui.core.PluginShowcase;
import net.vulkanmod.config.ui.settings.MenuPlugins;
import net.vulkanmod.config.ui.settings.PluginSettings;
import net.vulkanmod.config.ui.core.PresetCardLayout;
import net.vulkanmod.config.ui.core.PresetCardModel;
import net.vulkanmod.config.ui.core.PresetRating;
import net.vulkanmod.config.ui.core.Recommendation;
import net.vulkanmod.config.ui.settings.OverviewSignals;
import net.vulkanmod.vulkan.SessionSamples;
import net.vulkanmod.config.ui.core.Gradient;
import net.vulkanmod.config.ui.core.Glide;
import net.vulkanmod.config.ui.core.HoverState;
import net.vulkanmod.config.ui.core.ImpactLevel;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.ui.core.CoalArt;
import net.vulkanmod.config.ui.core.CoalScene;
import net.vulkanmod.config.ui.core.Motion;
import net.vulkanmod.config.ui.core.NavNode;
import net.vulkanmod.config.ui.core.OverviewModel;
import net.vulkanmod.config.ui.core.ProfileChipRow;
import net.vulkanmod.config.ui.core.PresetFx;
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
    private static final int ACCENT_BAR_WIDTH = 2;
    private static final int CHEVRON = 5;
    private static final String[] CHEVRON_RIGHT = {"#....", "##...", "###..", "##...", "#...."};
    private static final String[] CHEVRON_DOWN = {"#####", ".###.", ".###.", "..#..", "....."};

    static final int CARD_PAD_X = SettingRowLayout.CARD_PAD_X;
    static final int SLIDER_TRACK_WIDTH = 56;


    private static final float SCRIM_ALPHA = 0.72f;
    private static final long INSTANT_MS = 10_000L;
    private static final ResourceLocation COAL_BED =
            ResourceLocation.fromNamespaceAndPath("vulkanmod", "textures/gui/coalbed.png");
    private static final ResourceLocation[] COAL_ZONES = coalZones();
    private static final ResourceLocation SPARK_TEX =
            ResourceLocation.withDefaultNamespace("textures/particle/flame.png");
    private static final ResourceLocation LAVA_TEX =
            ResourceLocation.withDefaultNamespace("textures/particle/lava.png");
    private static final ResourceLocation[] SMOKE_TEX = smokeFrames();

    private static ResourceLocation[] coalZones() {
        ResourceLocation[] zones = new ResourceLocation[CoalScene.ZONES];
        for (int zone = 0; zone < zones.length; zone++) {
            zones[zone] = ResourceLocation.fromNamespaceAndPath("vulkanmod",
                    "textures/gui/coal_zone_" + zone + ".png");
        }
        return zones;
    }

    private static ResourceLocation[] smokeFrames() {
        ResourceLocation[] frames = new ResourceLocation[CoalScene.SMOKE_FRAMES];
        for (int frame = 0; frame < frames.length; frame++) {
            frames[frame] = ResourceLocation.withDefaultNamespace(
                    "textures/particle/big_smoke_" + frame + ".png");
        }
        return frames;
    }

    private ResourceLocation particleTexture(int index) {
        return switch (coals.kindOf(index)) {
            case CoalScene.SPARK -> SPARK_TEX;
            case CoalScene.LAVA -> LAVA_TEX;
            default -> SMOKE_TEX[coals.smokeFrame(index)];
        };
    }
    private static final int BAR_BUTTON_RADIUS = 5;
    private static final int SEARCH_RADIUS = 5;
    private static final ResourceLocation LOGO =
            ResourceLocation.fromNamespaceAndPath("vulkanmod", "textures/gui/volcanic_logo.png");
    private static final ResourceLocation TITLE =
            ResourceLocation.fromNamespaceAndPath("vulkanmod", "textures/gui/volcanic_title.png");
    private static final int LOGO_TEX_W = 100;
    private static final int LOGO_TEX_H = 88;
    private static final int TITLE_TEX_W = 336;
    private static final int TITLE_TEX_H = 64;
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
    private static final String KEY_PLUGINS_ON = "vulkanmod.ui.plugins.on";
    private static final String KEY_PLUGIN_SETTINGS = "vulkanmod.ui.plugins.groups";
    private static final String KEY_PLUGIN_NO_SETTINGS = "vulkanmod.ui.plugins.nogroups";
    private static final String KEY_PLUGINS_INTRO = "vulkanmod.ui.plugins.intro";
    private static final String KEY_STATS_WAITING = "vulkanmod.ui.stats.waiting";
    private static final String KEY_STATS_UNIT_FPS = "vulkanmod.ui.stats.unit.fps";
    private static final String KEY_STATS_UNIT_MS = "vulkanmod.ui.stats.unit.ms";
    private static final String KEY_STATS_PROFILE = "vulkanmod.ui.stats.profile";
    private static final String KEY_STATS_CAUSE_GC = "vulkanmod.ui.stats.cause.gc";
    private static final String KEY_STATS_CAUSE_UPLOAD = "vulkanmod.ui.stats.cause.upload";
    private static final String KEY_STATS_CAUSE_NONE = "vulkanmod.ui.stats.cause.none";
    private static final String KEY_STATS_SCALE = "vulkanmod.ui.stats.scale";
    private static final String KEY_STATS_GROUP_FINGERPRINT = "vulkanmod.ui.stats.group.fingerprint";
    private static final String KEY_STATS_GROUP_SCENE = "vulkanmod.ui.stats.group.scene";
    private static final String KEY_STATS_GROUP_TERRAIN = "vulkanmod.ui.stats.group.terrain";
    private static final String KEY_STATS_GROUP_MEMORY = "vulkanmod.ui.stats.group.memory";
    private static final String KEY_STATS_GROUP_MACHINE = "vulkanmod.ui.stats.group.machine";
    private static final String KEY_STATS_GROUP_STUTTERS = "vulkanmod.ui.stats.group.stutters";
    private static final String KEY_STATS_NOTE_TERRAIN = "vulkanmod.ui.stats.note.terrain";
    private static final String KEY_STATS_NOTE_MEMORY = "vulkanmod.ui.stats.note.memory";
    private static final String KEY_STATS_NOTE_MACHINE = "vulkanmod.ui.stats.note.machine";
    private static final String KEY_STATS_COL_WHEN = "vulkanmod.ui.stats.col.when";
    private static final String KEY_STATS_COL_WORST = "vulkanmod.ui.stats.col.worst";
    private static final String KEY_STATS_COL_GC = "vulkanmod.ui.stats.col.gc";
    private static final String KEY_STATS_COL_UPLOADS = "vulkanmod.ui.stats.col.uploads";
    private static final String KEY_STATS_COL_CAUSE = "vulkanmod.ui.stats.col.cause";
    private static final String KEY_STATS_TAG_GC = "vulkanmod.ui.stats.tag.gc";
    private static final String KEY_STATS_TAG_UPLOAD = "vulkanmod.ui.stats.tag.upload";
    private static final String KEY_STATS_KEY_AVERAGE = "vulkanmod.ui.stats.key.average";
    private static final String KEY_STATS_KEY_RANGE = "vulkanmod.ui.stats.key.range";
    private static final String KEY_STATS_KEY_SPIKE = "vulkanmod.ui.stats.key.spike";
    private static final String KEY_STATS_KEY_TARGET = "vulkanmod.ui.stats.key.target";
    private static final String KEY_STATS_KEY_GC = "vulkanmod.ui.stats.key.gc";
    private static final String KEY_STATS_COPY = "vulkanmod.ui.stats.copy";
    private static final String KEY_STATS_RESET = "vulkanmod.ui.stats.reset";
    private static final String KEY_STATS_REBUILD = "vulkanmod.ui.stats.rebuild";
    private static final String KEY_STATS_GROUP_TIME = "vulkanmod.ui.stats.group.time";
    private static final String KEY_STATS_ADVICE = "vulkanmod.ui.stats.advice";
    private static final String KEY_STATS_BAR_FRAME = "vulkanmod.ui.stats.bar.frame";
    private static final String KEY_STATS_BAR_THREAD = "vulkanmod.ui.stats.bar.thread";
    private static final String KEY_STATS_STUTTER_CAPTION = "vulkanmod.ui.stats.stutter.caption";
    private static final String KEY_STATS_COL_BUILDS = "vulkanmod.ui.stats.col.builds";
    private static final String KEY_STATS_SAMPLES = "vulkanmod.ui.stats.samples";
    private static final String KEY_STATS_NOW = "vulkanmod.ui.stats.now";
    private static final String KEY_STATS_ALLOCATING = "vulkanmod.ui.stats.allocating";
    private static final String KEY_STATS_NO_SAMPLES = "vulkanmod.ui.stats.nosamples";
    private static final String KEY_STATS_GROUP_FINDINGS = "vulkanmod.ui.stats.group.findings";
    private static final String KEY_STATS_LEGEND = "vulkanmod.ui.stats.legend";
    private static final String DASH = "—";
    private static final String[] STAT_TILES = {
            "vulkanmod.ui.stats.average", "vulkanmod.ui.stats.median", "vulkanmod.ui.stats.low1",
            "vulkanmod.ui.stats.low01", "vulkanmod.ui.stats.p95", "vulkanmod.ui.stats.spikes"};
    private static final String KEY_EXPERIMENTAL_INTRO = "vulkanmod.ui.experimental.intro";
    private static final String KEY_DEVELOPER_INTRO = "vulkanmod.ui.developer.intro";
    private static final RouteId EXPERIMENTAL = RouteId.parse("experimental");
    private static final RouteId DEVELOPER = RouteId.parse("developer");
    private static final String KEY_PLUGINS_OPEN = "vulkanmod.ui.plugins.open";
    private static final String KEY_PLUGINS_CLOSE = "vulkanmod.ui.plugins.close";
    private static final String KEY_PLUGINS_SETTINGS = "vulkanmod.ui.plugins.settings_button";
    private static final String KEY_PLUGINS_REQUIRES = "vulkanmod.ui.plugins.requires";
    private String showcaseShownId;
    private long showcaseElapsed = 9_999L;
    private static final String KEY_PLUGINS_NONE = "vulkanmod.ui.plugins.none";
    private static final String KEY_PLUGINS_EMPTY_HINT = "vulkanmod.ui.plugins.empty.hint";
    private static final int RATING_FROM_BOTTOM = 22;
    private static final String KEY_SUGGEST = "vulkanmod.overview.suggest";
    private static final String KEY_SUGGEST_WAIT = "vulkanmod.overview.suggest_wait";
    private static final String KEY_SUGGESTED = "vulkanmod.overview.suggested";
    private static final String KEY_PLAYING_NOW = "vulkanmod.overview.playing_now";
    private static final String KEY_PENDING = "vulkanmod.overview.pending";
    private static final String KEY_NOT_TRIED = "vulkanmod.overview.not_tried";
    private static final String KEY_SELECT = "vulkanmod.overview.select";
    private static final String KEY_PROFILES_INTRO = "vulkanmod.overview.profiles_intro";
    private static final String KEY_PROFILES_LEGEND = "vulkanmod.overview.profiles_legend";
    private static final String KEY_FRAMES = "vulkanmod.overview.frames";
    private static final String KEY_LOOKS = "vulkanmod.overview.looks";

    private static final int CARD_MARGIN = 4;

    private final Theme theme;
    private final SettingRowRenderer rowRenderer;
    private Rect lastCard = Rect.EMPTY;
    private final HoverState hover = new HoverState(Motion.HOVER_MS);
    private final Glide applyBarSlide = new Glide(80.0f);
    private ApplyBarModel lastBar;
    private boolean barOnScreen;
    private static final int MARKER_LAND_MS = 110;
    private static final int MARKER_OVERSHOOT = 2;
    private Rect tabPillNow = Rect.EMPTY;
    private final Glide navMarkerTop = new Glide(42.0f);
    private final Glide navMarkerHeight = new Glide(42.0f);
    private boolean navMarkerPlaced;
    private float navMarkerOffset;
    private float navMarkerSpan;
    private long navLand;
    private int navDir;
    private final Glide tabMarkerX = new Glide(42.0f);
    private final Glide tabMarkerWidth = new Glide(42.0f);
    private boolean tabMarkerPlaced;
    private long tabLand;
    private int tabDir;
    private int tabStripOrigin;
    private final CoalScene coals = new CoalScene(0x5A1FL);
    private final PresetFx presetFx = new PresetFx();
    private final Glide drawerSlide = new Glide(60.0f);
    private RouteId enteredRoute;
    private int enteredDepth;
    private long pageElapsed = Motion.SEQUENCE_MS;
    private int pageDirection;
    private long rowsElapsed = Motion.SEQUENCE_MS;
    private long searchElapsed;
    private boolean searchWasOpen;
    private int rowCount = -1;

    public ShellRenderer(Theme theme) {
        if (theme == null) {
            throw new IllegalArgumentException("theme must not be null");
        }
        this.theme = theme;
        this.rowRenderer = new SettingRowRenderer(theme);
    }

    public void render(GuiGraphics graphics, SurfacePainter painter, Font font, ShellLayout layout,
                       NavPresenter presenter, int scroll, int contentScroll, int mouseX, int mouseY,
                       SettingId dragged, boolean drawerOpen, boolean searchFocused, boolean keyboardMode,
                       long deltaMs) {
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
        deltaMs = motionEnabled() ? deltaMs : INSTANT_MS;
        this.lastCard = Rect.EMPTY;
        rowRenderer.tick(deltaMs);
        this.searchElapsed = searchFocused == searchWasOpen
                ? Math.min(this.searchElapsed + deltaMs, Motion.PAGE_MS) : 0L;
        this.searchWasOpen = searchFocused;

        advancePage(presenter, deltaMs);
        presetFx.advance(deltaMs);

        paintChrome(painter, layout, drawerOpen, searchFocused);
        painter.flush();

        paintFavoritesButton(painter, font, layout, presenter, mouseX, mouseY);
        painter.flush();

        Rect nav = layout.sidebarOrDrawer(drawerOpen);
        if (!layout.hasDrawer()) {
            paintNav(graphics, painter, nav, presenter, scroll, mouseX, mouseY, deltaMs);
        }

        Rect content = layout.content();
        if (!content.isEmpty()) {
            float reveal = Motion.easeOut(pageElapsed, Motion.PAGE_MS);
            graphics.enableScissor(content.x(), content.y(), content.right(), content.bottom());
            try {
                paintCoals(graphics, painter, content, deltaMs);
                painter.flush();
                painter.setOffset(Motion.slide(reveal, pageDirection, Motion.PAGE_TRAVEL), 0);
                paintContent(graphics, painter, font, layout, presenter, contentScroll, mouseX, mouseY,
                        dragged, keyboardMode, deltaMs);
                painter.flush();
                painter.setOffset(pageDirection == 0
                        ? Motion.slide(reveal, 0, Motion.PAGE_TRAVEL) : 0, 0);
                paintBand(painter, font, layout, presenter, deltaMs);
                paintScrollIndicator(painter, layout, presenter, contentScroll, deltaMs);
                painter.flush();
            } finally {
                painter.setOffset(0, 0);
                graphics.disableScissor();
            }
        }

        Rect details = layout.details();
        if (!details.isEmpty()) {
            graphics.enableScissor(details.x(), details.y(), details.right(), details.bottom());
            try {
                SettingMeta target = searchFocused
                        ? presenter.focusedSetting()
                        : cardTarget(layout, presenter, contentScroll, mouseX, mouseY, keyboardMode, dragged);
                if (target != null) {
                    paintCardWash(painter, details, 0, theme.color(ColorToken.ACCENT), 0.06f,
                            DetailsLayout.PAD_Y + DetailsLayout.TEXT_HEIGHT);
                    painter.fill(new Rect(details.x(), details.y(), 3, details.height()),
                            theme.color(ColorToken.ACCENT_DEEP));
                }
                paintDetailItems(painter, details, detailsItems(font, presenter, target, details));
                painter.flush();
            } finally {
                graphics.disableScissor();
            }
        }

        Rect drawer = layout.sidebarOrDrawer(true);
        if (!layout.hasDrawer() || drawer.isEmpty()) {
            drawerSlide.jumpTo(drawer.width() + 1.0f);
        } else {
            float hidden = drawer.width() + 1;
            int shift = Math.round(drawerSlide.advance(drawerOpen ? 0.0f : hidden, deltaMs));
            if (shift < hidden) {
                float openness = 1.0f - shift / hidden;
                painter.fill(layout.content(),
                        theme.color(ColorToken.SURFACE_SUNKEN, SCRIM_ALPHA * openness));
                painter.flush();
                painter.setOffset(-shift, 0);
                paintNav(graphics, painter, drawer, presenter, scroll, mouseX, mouseY, deltaMs);
                painter.fill(new Rect(drawer.right(), drawer.y(), 1, drawer.height()),
                        theme.color(ColorToken.BORDER_ACCENT));
                painter.flush();
                painter.setOffset(0, 0);
            }
        }

        paintApplyBar(painter, font, layout, presenter, mouseX, mouseY, deltaMs);
        painter.flush();

        hover.endFrame();
        rowRenderer.endFrame();
    }

    private void advancePage(NavPresenter presenter, long deltaMs) {
        RouteId current = presenter.stack().current();
        int rows = presenter.contentRowCount();
        if (current.equals(enteredRoute)) {
            this.pageElapsed = Math.min(this.pageElapsed + deltaMs, Motion.SEQUENCE_MS);
            this.rowsElapsed = rows == rowCount
                    ? Math.min(this.rowsElapsed + deltaMs, Motion.SEQUENCE_MS) : 0L;
            this.rowCount = rows;
            return;
        }
        this.rowCount = rows;
        this.rowsElapsed = Motion.SEQUENCE_MS;
        this.pageDirection = enteredRoute == null || current.depth() >= enteredDepth ? 1 : -1;
        this.enteredRoute = current;
        this.enteredDepth = current.depth();
        this.pageElapsed = 0L;
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
                                   int mouseX, int mouseY, boolean keyboardMode, SettingId dragged) {
        SettingMeta held = settingById(presenter, dragged);
        if (held != null) {
            return held;
        }
        SettingMeta hovered = hoveredSetting(layout, presenter, contentScroll, mouseX, mouseY);
        if (hovered != null) {
            return hovered;
        }
        return keyboardMode ? presenter.focusedSetting() : null;
    }

    private SettingMeta hoveredSetting(ShellLayout layout, NavPresenter presenter, int contentScroll,
                                       int mouseX, int mouseY) {
        if (!layout.content().contains(mouseX, mouseY)) {
            return null;
        }
        List<NavPresenter.ContentRow> rows = presenter.contentRows();
        List<Rect> boxes = settingRowBoxes(layout, presenter, contentScroll);
        for (int i = 0; i < rows.size() && i < boxes.size(); i++) {
            if (boxes.get(i).contains(mouseX, mouseY)
                    && rows.get(i) instanceof NavPresenter.SettingRow row) {
                return row.meta();
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
        List<NavPresenter.ContentRow> rows = presenter.contentRows();
        List<Rect> boxes = settingRowBoxes(layout, presenter, contentScroll);
        for (int i = 0; i < rows.size() && i < boxes.size(); i++) {
            if (rows.get(i) instanceof NavPresenter.SettingRow row
                    && row.meta().id().equals(meta.id())) {
                return boxes.get(i);
            }
        }
        return Rect.EMPTY;
    }

    public Rect lastCard() {
        return lastCard;
    }

    private static final long CARD_HOVER_DELAY_MS = 450L;
    private SettingId cardShown;
    private long cardElapsed = 9_999L;
    private SettingId cardArmed;
    private long cardArmedMs;

    public void renderCard(SurfacePainter painter, Font font, ShellLayout layout, NavPresenter presenter,
                           int contentScroll, int mouseX, int mouseY, boolean keyboardMode, SettingId dragged,
                           long deltaMs) {
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
                ? settingById(presenter, dragged)
                : cardTarget(layout, presenter, contentScroll, mouseX, mouseY, keyboardMode, dragged);
        if (meta == null) {
            this.cardShown = null;
            this.cardArmed = null;
            return;
        }
        if (!sheet && !keyboardMode && dragged == null
                && cardShown == null && !meta.id().equals(cardShown)) {
            if (meta.id().equals(cardArmed)) {
                this.cardArmedMs += Math.max(0L, deltaMs);
            } else {
                this.cardArmed = meta.id();
                this.cardArmedMs = 0L;
            }
            if (cardArmedMs < CARD_HOVER_DELAY_MS) {
                return;
            }
        }
        if (meta.id().equals(cardShown)) {
            this.cardElapsed = Math.min(9_999L, cardElapsed + Math.max(0L, deltaMs));
        } else {
            this.cardShown = meta.id();
            this.cardElapsed = 0L;
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
        float reveal = motionEnabled() ? Motion.easeOut(cardElapsed, 140) : 1.0f;
        painter.setOffset(0, Motion.slide(reveal, 1, 5));
        painter.setAlpha(reveal);
        try {
            paintRoundedFill(painter, box, radius, theme.color(ColorToken.SURFACE_CHROME));
            paintCardWash(painter, box, radius, theme.color(ColorToken.ACCENT), 0.07f,
                    DetailsLayout.PAD_Y + DetailsLayout.TEXT_HEIGHT);
            paintRoundedOutline(painter, box, radius, theme.color(ColorToken.BORDER_ACCENT));
            for (Rect span : RoundedScanline.fillSpans(box, radius)) {
                painter.fill(new Rect(span.x(), span.y(), Math.min(3, span.width()), span.height()),
                        theme.color(ColorToken.ACCENT_DEEP));
            }
            paintDetailItems(painter, box, items);
            painter.flush();
        } finally {
            painter.setOffset(0, 0);
            painter.setAlpha(1.0f);
        }
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
        int lit = Math.round(item.bar().fill() * PresetCardLayout.SEGMENTS);
        int bright = item.accentBar() ? theme.color(ColorToken.ACCENT_BRIGHT)
                : theme.color(ColorToken.IMPACT_VISUAL);
        int deep = item.accentBar() ? theme.color(ColorToken.ACCENT_DEEP)
                : Motion.blend(theme.color(ColorToken.IMPACT_VISUAL), 0xFF000000, 0.45f);
        int half = track.height() / 2;
        for (int cell = 0; cell < PresetCardLayout.SEGMENTS; cell++) {
            Rect seg = PresetCardLayout.segment(track, cell);
            if (seg.isEmpty()) {
                continue;
            }
            if (cell < lit) {
                painter.fill(new Rect(seg.x(), seg.y(), seg.width(), half), bright);
                painter.fill(new Rect(seg.x(), seg.y() + half, seg.width(), seg.height() - half), deep);
            } else {
                painter.fill(seg, theme.color(ColorToken.IMPACT_TRACK));
                painter.fill(seg.inset(1), theme.color(ColorToken.SURFACE_SUNKEN));
            }
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

        float open = Motion.easeOut(searchElapsed, Motion.PAGE_MS);
        painter.setOffset(0, Motion.slide(open, -1, Motion.ROW_TRAVEL));
        painter.setAlpha(open);
        try {
            paintSearchBody(painter, font, layout, presenter, results, query, selected, mouseX, mouseY);
        } finally {
            painter.setOffset(0, 0);
            painter.setAlpha(1.0f);
        }
    }

    private void paintSearchBody(SurfacePainter painter, Font font, ShellLayout layout,
                                 NavPresenter presenter, SearchResultsModel results, String query,
                                 int selected, int mouseX, int mouseY) {
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

    public List<Rect> tabStripBoxes(Font font, ShellLayout layout, NavPresenter presenter) {
        requireInputs(font, layout, presenter);
        Rect strip = headerBand(layout, presenter).tabs();
        if (strip.isEmpty()) {
            return List.of();
        }
        List<NavNode> tabs = presenter.subTabs();
        int[] widths = new int[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) {
            widths[i] = font.width(I18n.get(tabs.get(i).titleKey()));
        }

        int left = strip.x();
        int right = Math.max(left, strip.right());
        List<Rect> boxes = TabStripModel.layout(widths, left, strip.y());
        return TabStripModel.shifted(boxes,
                TabStripModel.scrollToReveal(boxes, revealIndex(presenter, tabs), left, right));
    }

    public int statsColumns(ShellLayout layout, NavPresenter presenter) {
        return statsPage(layout, presenter, 0).plot().width();
    }

    public List<Rect> settingRowBoxes(ShellLayout layout, NavPresenter presenter, int scroll) {
        if (layout == null) {
            throw new IllegalArgumentException("layout must not be null");
        }
        if (presenter == null) {
            throw new IllegalArgumentException("presenter must not be null");
        }
        List<Rect> rows = SettingRowLayout.rows(contentBody(layout, presenter),
                presenter.contentRowCount(), scroll, layout.breakpoint());
        if (pageElapsed >= Motion.SEQUENCE_MS && rowsElapsed >= Motion.SEQUENCE_MS) {
            return rows;
        }
        List<Rect> shifted = new ArrayList<>(rows.size());
        for (int index = 0; index < rows.size(); index++) {
            shifted.add(rows.get(index).translated(
                    Motion.slide(Motion.rowReveal(pageElapsed, index), pageDirection, Motion.PAGE_TRAVEL),
                    Motion.slide(Motion.easeOut(rowsElapsed - index * 12L, Motion.ROWS_MS), 1,
                            Motion.ROW_TRAVEL)));
        }
        return List.copyOf(shifted);
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
        List<Rect> rows = SettingRowLayout.rows(contentBody(layout, presenter), 1, 0, layout.breakpoint());
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
        Rect body = contentBody(layout, presenter);
        RouteId current = presenter.stack().current();
        if (presenter.isDeveloperInfo()) {
            return ScrollIndicator.of(body, InfoRowLayout.contentHeight(presenter.infoSections()), scroll);
        }
        if (presenter.isDeveloperStats()) {
            return ScrollIndicator.of(body,
                    FrameGraphLayout.contentHeight(statsCounts(presenter), layout.breakpoint()), scroll);
        }
        if (PluginSettings.ROOT.equals(current)) {
            return ScrollIndicator.of(body, PluginPageLayout.contentHeight(presenter.pluginPages().size(),
                    presenter.catalog().modIds().size(), layout.breakpoint()), scroll);
        }
        if (OVERVIEW.equals(current)) {
            return ScrollIndicator.of(body, PresetCardLayout.page(body, presenter.presetCards().size(),
                    0, layout.breakpoint(),
                    PresetCardModel.customTail(presenter.presetCards())).height(), scroll);
        }
        return ScrollIndicator.of(body,
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

    public void paintBrand(GuiGraphics graphics, ShellLayout layout) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Rect logo = layout.brandLogo();
        if (!logo.isEmpty()) {
            graphics.blit(LOGO, logo.x(), logo.y(), logo.width(), logo.height(), 0.0F, 0.0F,
                    LOGO_TEX_W, LOGO_TEX_H, LOGO_TEX_W, LOGO_TEX_H);
        }
        Rect title = layout.brandTitle();
        if (!title.isEmpty()) {
            graphics.blit(TITLE, title.x(), title.y(), title.width(), title.height(), 0.0F, 0.0F,
                    TITLE_TEX_W, TITLE_TEX_H, TITLE_TEX_W, TITLE_TEX_H);
        }
        RenderSystem.disableBlend();
    }

    private void paintApplyBar(SurfacePainter painter, Font font, ShellLayout layout, NavPresenter presenter,
                               int mouseX, int mouseY, long deltaMs) {
        ApplyBarModel bar = ApplyBarModel.of(presenter.pending());
        Rect region = layout.bottomBar();
        if (region.isEmpty()) {
            return;
        }
        if (bar.visible()) {
            this.lastBar = bar;
        }
        int hidden = region.height() + 2;
        int lift = Math.round(applyBarSlide.advance(bar.visible() ? 0.0f : hidden, deltaMs));
        this.barOnScreen = lift < hidden && lastBar != null;
        if (!barOnScreen) {
            return;
        }
        painter.setOffset(0, lift);
        try {
            paintApplyBarBody(painter, font, layout, presenter, lastBar, region,
                    layout.applyButton(), layout.discardButton(), mouseX, mouseY);
        } finally {
            painter.setOffset(0, 0);
        }
    }

    public boolean applyBarOnScreen() {
        return this.barOnScreen;
    }

    private void paintApplyBarBody(SurfacePainter painter, Font font, ShellLayout layout,
                                   NavPresenter presenter, ApplyBarModel bar, Rect region,
                                   Rect apply, Rect discard, int mouseX, int mouseY) {
        painter.fill(region, theme.color(ColorToken.SURFACE_CHROME));
        painter.fill(new Rect(region.x(), region.y(), region.width(), 1),
                theme.color(ColorToken.BORDER_ACCENT));
        ColorToken token = bar.scope() == ApplyScope.RESTART ? ColorToken.WARNING : ColorToken.TEXT_SECONDARY;
        painter.text(region.x() + PageHeader.PAD_X, region.y() + (region.height() - TEXT_HEIGHT) / 2,
                I18n.get(bar.messageKey(), bar.count()), theme.color(token), false);

        paintBarButton(painter, font, apply, I18n.get(KEY_APPLY),
                ColorToken.ACCENT, mouseX, mouseY);
        paintBarButton(painter, font, discard, I18n.get(KEY_DISCARD),
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

        paintNavMarker(painter, sidebar, model, viewport, activeRoute, deltaMs);

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
                    Rect box = SidebarModel.rowBox(sidebar, top, height, depth);
                    boolean active = route.equals(activeRoute);
                    String key = route.toString();
                    float hovered = hover.advance(key, index == hoveredIndex, deltaMs);
                    paintRowSurface(painter, box, false, active ? 0.0f : hovered,
                            !active && key.equals(focusedId) ? 1.0f : 0.0f);
                    int argb = presenter.rowGreyed(route) ? theme.color(ColorToken.TEXT_MUTED)
                            : Motion.blend(theme.color(ColorToken.TEXT_SECONDARY),
                                    theme.color(ColorToken.TEXT_PRIMARY),
                                    volcanic$markerOn(model.offsetOf(index), height));
                    painter.text(sidebar.x() + SidebarModel.ROW_TEXT_X
                                    + (depth - 1) * SidebarModel.ROW_INDENT,
                            top + (height - TEXT_HEIGHT) / 2, I18n.get(titleKey), argb, false);
                }
            }
        }

        paintPluginRail(painter, model, sidebar, scroll, presenter);
        paintScrollIndicator(painter, ScrollIndicator.of(sidebar, model.totalHeight(), scroll));
    }

    private void paintPluginRail(SurfacePainter painter, SidebarModel model, Rect sidebar, int scroll,
                                 NavPresenter presenter) {
        SidebarModel.Rail rail = model.rail(sidebar, scroll, PluginSettings.ROOT);
        if (rail.stem().isEmpty() && rail.ticks().isEmpty()) {
            return;
        }
        RouteId active = presenter.activeSidebarRoute();
        int argb = theme.color(PluginSettings.ROOT.equals(active) || PluginSettings.ROOT.isAncestorOf(active)
                ? ColorToken.ACCENT : ColorToken.BORDER_ACCENT);
        painter.fill(rail.stem(), argb);
        for (Rect tick : rail.ticks()) {
            painter.fill(tick, argb);
        }
    }

    private static boolean motionEnabled() {
        try {
            return Initializer.CONFIG == null || Initializer.CONFIG.uiAnimations;
        } catch (Throwable unavailable) {
            return true;
        }
    }

    private static boolean backgroundEnabled() {
        try {
            return Initializer.CONFIG == null || Initializer.CONFIG.backgroundAnimation;
        } catch (Throwable unavailable) {
            return true;
        }
    }

    private void paintCoals(GuiGraphics graphics, SurfacePainter painter, Rect content, long deltaMs) {
        boolean alive = backgroundEnabled();
        if (alive) {
            coals.advance(deltaMs, content);
        }

        Rect bed = coals.bedRect(content);
        if (bed.isEmpty()) {
            return;
        }
        graphics.blit(COAL_BED, bed.x(), bed.y(), bed.width(), bed.height(),
                0.0f, 0.0f, CoalArt.TEX_W, CoalArt.TEX_H, CoalArt.TEX_W, CoalArt.TEX_H);

        for (int zone = 0; zone < CoalScene.ZONES; zone++) {
            int tint = coals.zoneTint(zone);
            graphics.setColor(((tint >> 16) & 0xFF) / 255.0f, ((tint >> 8) & 0xFF) / 255.0f,
                    (tint & 0xFF) / 255.0f, ((tint >>> 24) & 0xFF) / 255.0f);
            graphics.blit(COAL_ZONES[zone], bed.x(), bed.y(), bed.width(), bed.height(),
                    0.0f, 0.0f, CoalArt.TEX_W, CoalArt.TEX_H, CoalArt.TEX_W, CoalArt.TEX_H);
        }
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        if (!alive) {
            return;
        }

        float grow = coals.particleScale(content);
        for (int index = 0; index < CoalScene.PARTICLES; index++) {
            if (coals.waiting(index)) {
                continue;
            }
            int argb = coals.argbOf(index);
            int alpha = argb >>> 24;
            if (alpha == 0) {
                continue;
            }
            int side = Math.max(2, Math.round(coals.sizeOf(index) * grow));
            int left = coals.xOf(index, content);
            if (left >= content.right() || left + side <= content.x()) {
                continue;
            }
            graphics.setColor(((argb >> 16) & 0xFF) / 255.0f, ((argb >> 8) & 0xFF) / 255.0f,
                    (argb & 0xFF) / 255.0f, alpha / 255.0f);
            int source = coals.kindOf(index) == CoalScene.SMOKE ? 16 : 8;
            graphics.blit(particleTexture(index), left, coals.yOf(index, content) - side / 2,
                    side, side, 0.0f, 0.0f, source, source, source, source);
        }
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private float volcanic$markerOn(int rowOffset, int rowHeight) {
        if (!navMarkerPlaced || rowHeight <= 0) {
            return 0.0f;
        }
        float low = Math.max(rowOffset, navMarkerOffset);
        float high = Math.min(rowOffset + rowHeight, navMarkerOffset + navMarkerSpan);
        return Math.max(0.0f, (high - low) / rowHeight);
    }

    private void paintNavMarker(SurfacePainter painter, Rect sidebar, SidebarModel model,
                                SidebarViewport viewport, RouteId activeRoute, long deltaMs) {
        int index = -1;
        int depth = 1;
        for (int candidate = 0; candidate < model.entries().size(); candidate++) {
            if (model.entries().get(candidate) instanceof SidebarModel.Row row
                    && row.route().equals(activeRoute)) {
                index = candidate;
                depth = row.depth();
                break;
            }
        }
        if (index < 0) {
            this.navMarkerPlaced = false;
            return;
        }

        float offset = model.offsetOf(index);
        float span = model.heightOf(index);
        if (!navMarkerPlaced) {
            this.navMarkerPlaced = true;
            navMarkerTop.jumpTo(offset);
            navMarkerHeight.jumpTo(span);
            this.navLand = 0L;
        }
        boolean travelling = !(navMarkerTop.settled(offset) && navMarkerHeight.settled(span));
        if (travelling) {
            this.navDir = Float.compare(offset, navMarkerOffset) >= 0 ? 1 : -1;
        }
        this.navMarkerOffset = navMarkerTop.advance(offset, deltaMs);
        this.navMarkerSpan = navMarkerHeight.advance(span, deltaMs);
        this.navLand = Math.max(0L, navLand - deltaMs);
        if (travelling && navMarkerTop.settled(offset) && navMarkerHeight.settled(span)) {
            this.navLand = MARKER_LAND_MS;
        }

        float landT = 1.0f - navLand / (float) MARKER_LAND_MS;
        int bounce = navLand > 0L
                ? Math.round(navDir * MARKER_OVERSHOOT
                        * (float) Math.sin(Math.PI * landT) * (1.0f - landT)) : 0;
        int top = viewport.screenTop(Math.round(navMarkerOffset) + bounce);
        int spanNow = Math.round(navMarkerSpan);
        Rect box = SidebarModel.rowBox(sidebar, top, spanNow, depth);
        paintRoundedGradient(painter, box, NAV_RADIUS,
                theme.color(ColorToken.SURFACE_NAV_ACTIVE), theme.color(ColorToken.SURFACE_SIDEBAR_BOTTOM));
        if (navLand > 0L) {
            paintRoundedFill(painter, box, NAV_RADIUS,
                    Motion.fade(theme.color(ColorToken.ACCENT), 0.13f * (1.0f - landT)));
        }
        paintRoundedOutline(painter, box, NAV_RADIUS, theme.color(ColorToken.BORDER_ACCENT));
        paintLeadingEdge(painter, box, NAV_RADIUS, theme.color(ColorToken.ACCENT));
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

    private void paintContent(GuiGraphics graphics, SurfacePainter painter, Font font, ShellLayout layout,
                              NavPresenter presenter, int contentScroll, int mouseX, int mouseY,
                              SettingId dragged, boolean keyboardFocus, long deltaMs) {
        Rect content = layout.content();
        if (content.isEmpty()) {
            return;
        }

        if (presenter.isDeveloperInfo()) {
            paintInfoPage(painter, font, layout, presenter, contentScroll);
        } else if (presenter.isDeveloperStats()) {
            paintStatsPage(painter, font, layout, presenter, contentScroll, mouseX, mouseY);
        } else if (OVERVIEW.equals(presenter.stack().current())) {
            paintOverview(graphics, painter, font, layout, presenter, contentScroll, mouseX, mouseY);
        } else {
            paintSettings(graphics, painter, font, layout, presenter, contentScroll, mouseX, mouseY,
                    dragged, keyboardFocus, deltaMs);
        }
        painter.flush();
    }

    public PageHeader.Band headerBand(ShellLayout layout, NavPresenter presenter) {
        if (layout == null || presenter == null) {
            throw new IllegalArgumentException("layout and presenter must not be null");
        }
        return PageHeader.of(layout.content(), hasCrumbs(presenter), hasTabs(presenter),
                subtitleKey(presenter) != null, layout.breakpoint());
    }

    public Rect contentBody(ShellLayout layout, NavPresenter presenter) {
        return layout.content().dropTop(headerBand(layout, presenter).height());
    }

    private static boolean hasCrumbs(NavPresenter presenter) {
        return presenter.stack().trail().size() >= 2;
    }

    private static boolean hasTabs(NavPresenter presenter) {
        return !PluginSettings.ROOT.equals(presenter.stack().current()) && !presenter.subTabs().isEmpty();
    }

    private static String subtitleKey(NavPresenter presenter) {
        RouteId current = presenter.stack().current();
        if (OVERVIEW.equals(current)) {
            return KEY_PROFILES_INTRO;
        }
        if (PluginSettings.ROOT.equals(current)) {
            return KEY_PLUGINS_INTRO;
        }
        if (EXPERIMENTAL.equals(current)) {
            return KEY_EXPERIMENTAL_INTRO;
        }
        return DEVELOPER.equals(current) || DEVELOPER.isAncestorOf(current) ? KEY_DEVELOPER_INTRO : null;
    }

    public List<Rect> presetCardBoxes(ShellLayout layout, NavPresenter presenter, int scroll) {
        if (layout == null || presenter == null) {
            throw new IllegalArgumentException("layout and presenter must not be null");
        }
        return PresetCardLayout.page(contentBody(layout, presenter), presenter.presetCards().size(), scroll,
                layout.breakpoint(), PresetCardModel.customTail(presenter.presetCards())).cards();
    }

    public void pressPresetCard(int index, PresetCardModel.Card model) {
        if (model != null) {
            presetFx.trigger(index, PresetFx.effectFor(model.key()));
        }
    }

    private void paintOverview(GuiGraphics graphics, SurfacePainter painter, Font font, ShellLayout layout,
                               NavPresenter presenter, int contentScroll, int mouseX, int mouseY) {
        List<PresetCardModel.Card> cards = presenter.presetCards();
        PresetCardLayout.Page page = PresetCardLayout.page(contentBody(layout, presenter), cards.size(),
                contentScroll, layout.breakpoint(), PresetCardModel.customTail(cards));
        paintProfilesLegend(painter, font, page.legend());
        for (int i = 0; i < cards.size() && i < page.cards().size(); i++) {
            Rect box = page.cards().get(i);
            boolean over = box.contains(mouseX, mouseY);
            paintCardWithFx(graphics, painter, font, box, cards.get(i), over, i, mouseX, mouseY);
        }
        paintSuggestionLine(painter, font, page.suggestion(), presenter);
    }

    private void paintProfilesLegend(SurfacePainter painter, Font font, Rect legend) {
        if (legend.isEmpty()) {
            return;
        }
        painter.smallText(legend.x(), legend.y(),
                smallTrim(painter, font, I18n.get(KEY_PROFILES_LEGEND), legend.width()),
                theme.color(ColorToken.TEXT_FAINT));
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

    private void paintCardWithFx(GuiGraphics graphics, SurfacePainter painter, Font font, Rect box,
                                 PresetCardModel.Card model, boolean hovered, int index,
                                 int mouseX, int mouseY) {
        int effect = presetFx.effect(index);
        int dx = presetFx.shakeX(index);
        int dy = presetFx.shakeY(index);

        if (effect == PresetFx.ERUPT && presetFx.shattered(index)) {
            paintShards(graphics, painter, font, box, model, index);
            return;
        }

        if (effect == PresetFx.SKIP) {
            for (int ghost = 0; ghost < PresetFx.GHOSTS; ghost++) {
                painter.setOffset(presetFx.ghostOffset(index, ghost), 0);
                painter.setAlpha(0.3f);
                paintPresetCard(painter, font, box, model, false);
            }
            painter.setOffset(0, 0);
            painter.setAlpha(1.0f);
        }

        if (effect == PresetFx.HAZE) {
            paintHaze(graphics, painter, font, box, model, hovered, index);
        } else {
            float tilt = effect == PresetFx.ROCK ? presetFx.rockAngle(index)
                    : hovered && effect == PresetFx.NONE && box.height() > box.width()
                    ? PresetFx.tiltDegrees(PresetFx.tiltStep(box, mouseX)) : 0.0f;
            painter.setOffset(dx, dy);
            if (tilt == 0.0f) {
                paintPresetCard(painter, font, box, model, hovered);
                painter.flush();
            } else {
                painter.flush();
                graphics.pose().pushPose();
                graphics.pose().translate(box.x() + box.width() / 2.0f,
                        box.y() + box.height() / 2.0f, 0.0f);
                graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(tilt * 0.35f));
                graphics.pose().scale(1.0f + Math.abs(tilt) * 0.004f, 1.0f, 1.0f);
                graphics.pose().translate(-(box.x() + box.width() / 2.0f),
                        -(box.y() + box.height() / 2.0f), 0.0f);
                paintPresetCard(painter, font, box, model, hovered);
                painter.flush();
                graphics.pose().popPose();
            }
            painter.setOffset(0, 0);
        }

        if (hovered && effect == PresetFx.NONE && box.height() > box.width()) {
            paintCardGlare(painter, box, PresetIcons.tone(model.key()), mouseX, mouseY);
        }
        paintCardEffect(painter, box, model, index, effect);
        painter.flush();
    }

    private void paintShards(GuiGraphics graphics, SurfacePainter painter, Font font, Rect box,
                             PresetCardModel.Card model, int index) {
        int columns = 3;
        int rows = PresetFx.BLOCKS / columns;
        int pieceW = Math.max(1, box.width() / columns);
        int pieceH = Math.max(1, box.height() / rows);
        for (int block = 0; block < PresetFx.BLOCKS; block++) {
            int sx = box.x() + (block % columns) * pieceW;
            int sy = box.y() + (block / columns) * pieceH;
            int dx = presetFx.blockX(index, block, box) - box.width() / 2 + (sx - box.x());
            int dy = presetFx.blockY(index, block, box) - box.height() / 2 + (sy - box.y());
            slice(graphics, painter, new Rect(dx, dy, pieceW, pieceH), dx - sx, dy - sy,
                    () -> paintPresetCard(painter, font, box, model, false));
        }
    }

    private void paintHaze(GuiGraphics graphics, SurfacePainter painter, Font font, Rect box,
                           PresetCardModel.Card model, boolean hovered, int index) {
        int band = Math.max(2, box.height() / PresetFx.BANDS);
        for (int slice = 0; slice < PresetFx.BANDS; slice++) {
            int top = box.y() + slice * band;
            int tall = Math.min(band, box.bottom() - top);
            if (tall <= 0) {
                continue;
            }
            int shift = presetFx.bandShift(index, slice);
            slice(graphics, painter, new Rect(box.x() + shift, top, box.width(), tall), shift, 0,
                    () -> paintPresetCard(painter, font, box, model, hovered));
        }
        int heat = presetFx.heat(index);
        if (heat > 0) {
            painter.fill(box, theme.color(ColorToken.ACCENT, heat * 0.05f));
            painter.flush();
        }
    }

    private void slice(GuiGraphics graphics, SurfacePainter painter, Rect window, int dx, int dy,
                       Runnable body) {
        if (window.isEmpty()) {
            return;
        }
        graphics.enableScissor(window.x(), window.y(), window.right(), window.bottom());
        try {
            painter.setOffset(dx, dy);
            body.run();
            painter.flush();
        } finally {
            painter.setOffset(0, 0);
            graphics.disableScissor();
        }
    }

    private void paintCardGlare(SurfacePainter painter, Rect box, int tone, int mouseX, int mouseY) {
        int reach = Math.min(56, box.width() * 2 / 3);
        int outer = reach * reach;
        int core = reach * 2 / 5;
        int mid = reach * 7 / 10;
        int argb = Motion.fade(tone, 0.17f);
        for (Rect span : RoundedScanline.fillSpans(box.inset(1), SettingRowLayout.CARD_RADIUS - 1)) {
            int dy = span.y() - mouseY;
            if (dy * dy > outer) {
                continue;
            }
            for (int x = span.x(); x < span.right(); x++) {
                int dx = x - mouseX;
                int far = dx * dx + dy * dy;
                if (far > outer) {
                    continue;
                }
                boolean lit = far <= core * core ? ((x + span.y()) & 1) == 0
                        : far <= mid * mid ? ((x + span.y() * 2) & 3) == 0
                        : (x & 3) == 0 && (span.y() & 3) == 0;
                if (lit) {
                    painter.fill(new Rect(x, span.y(), 1, 1), argb);
                }
            }
        }
    }

    private void paintCardEffect(SurfacePainter painter, Rect box, PresetCardModel.Card model,
                                 int index, int effect) {
        if (effect == PresetFx.SKIP) {
            int argb = theme.color(ColorToken.ACCENT, 0.55f);
            for (int streak = 0; streak < PresetFx.STREAKS; streak++) {
                painter.fill(new Rect(box.x(), box.y() + presetFx.streakY(index, streak, box.height()),
                        box.width(), 1), argb);
            }
        } else if (effect == PresetFx.ROCK) {
            int tone = PresetIcons.tone(model.key());
            int angle = presetFx.rockAngle(index);
            int cx = box.x() + box.width() / 2;
            if (angle != 0) {
                int side = Integer.signum(angle);
                int reach = box.width() / 2 - 6;
                int drop = Math.abs(angle);
                for (int step = 1; step <= 4; step++) {
                    int x = cx + side * (reach * step / 4);
                    int y = box.bottom() - 8 + drop * step / 4;
                    painter.fill(new Rect(x - 1, y - 1, 2, 2), Motion.fade(tone, 0.30f + 0.12f * step));
                }
                painter.fill(new Rect(cx - 1, box.bottom() - 7, 2, 4), tone);
            }
            int step = presetFx.convergeStep(index);
            if (step > 0) {
                int top = box.y() + 3;
                int tall = box.height() - 6;
                int start = box.x() + 3;
                int goal = cx - 2;
                int front = start + (goal - start) * step / 4;
                int mirror = box.right() - 3 - (front - start);
                for (int side = 0; side < 2; side++) {
                    int x = side == 0 ? front : mirror - 2;
                    painter.fill(new Rect(x, top, 2, tall), Motion.fade(tone, 0.85f));
                    for (int tail = 1; tail <= 3; tail++) {
                        int tx = side == 0 ? x - tail * 3 : x + 1 + tail * 3;
                        if (tx <= box.x() + 1 || tx >= box.right() - 1) {
                            continue;
                        }
                        int from = top + Math.floorMod(tx + top, 2);
                        for (int y = from; y < top + tall; y += 2) {
                            painter.fill(new Rect(tx, y, 1, 1),
                                    Motion.fade(tone, 0.5f - 0.13f * tail));
                        }
                    }
                }
            }

            int age = presetFx.blastAge(index);
            if (age >= 0) {
                int mid = box.y() + box.height() / 2;
                if (age == 0) {
                    for (Rect span : RoundedScanline.fillSpans(box, SettingRowLayout.CARD_RADIUS)) {
                        painter.fill(span, Motion.fade(tone, 0.4f));
                    }
                }
                int beam = Math.max(1, 4 - age);
                painter.fill(new Rect(cx - beam / 2, box.y() + 2, Math.max(1, beam),
                        box.height() - 4), Motion.fade(age == 0 ? 0xFFFFF3D6 : tone,
                        0.95f - 0.18f * age));
                paintRoundedOutline(painter, box, SettingRowLayout.CARD_RADIUS,
                        Motion.fade(tone, 0.5f - 0.1f * age));

                for (int i = 0; i < 18; i++) {
                    int dir = (i & 1) == 0 ? -1 : 1;
                    int lane = cx - 7 + (i * 5) % 15;
                    int speed = 5 + (i * 7) % 9;
                    int y = mid + dir * (3 + age * speed);
                    if (y <= box.y() + 2 || y >= box.bottom() - 3) {
                        continue;
                    }
                    int size = i % 5 == 0 ? 2 : 1;
                    int argb = i % 3 == 0 ? 0xFFFFF3D6 : age >= 3
                            ? Motion.blend(tone, 0xFF000000, 0.35f) : tone;
                    painter.fill(new Rect(lane, y, size, size),
                            Motion.fade(argb, 1.0f - 0.18f * age));
                }
            }
        } else if (effect == PresetFx.ERUPT && presetFx.flashing(index)) {
            painter.fill(box, theme.color(ColorToken.TEXT_PRIMARY));
        } else if (effect == PresetFx.SCAN) {
            int y = presetFx.scanY(index, box);
            if (y >= 0) {
                painter.fill(new Rect(box.x(), y, box.width(), 1), theme.color(ColorToken.TEXT_PRIMARY));
                for (int x = box.x(); x < box.right(); x += 2) {
                    painter.fill(new Rect(x, box.y(), 1, y - box.y()),
                            theme.color(ColorToken.BORDER_ACCENT, 0.5f));
                }
            }
        }
    }

    private void paintPresetCard(SurfacePainter painter, Font font, Rect card, PresetCardModel.Card model,
                                 boolean hovered) {
        if (card.isEmpty()) {
            return;
        }
        if (card.width() > card.height()) {
            paintCustomBar(painter, font, card, model, hovered && model.selectable());
            return;
        }
        PresetCardLayout.Slots slots = PresetCardLayout.slots(card,
                card.height() >= PresetCardLayout.CARD_HEIGHT);
        if (slots.name().isEmpty()) {
            return;
        }
        boolean lit = hovered && model.selectable();
        int tone = PresetIcons.tone(model.key());
        int toneDeep = Motion.blend(tone, 0xFF000000, 0.45f);
        int edge = model.playing() ? tone
                : model.staged() ? theme.color(ColorToken.WARNING)
                : model.suggested() ? theme.color(ColorToken.BORDER_ACCENT)
                : lit ? theme.color(ColorToken.BORDER_STRONG)
                : theme.color(ColorToken.BORDER_SUBTLE);

        paintRoundedFill(painter, card, SettingRowLayout.CARD_RADIUS,
                theme.color(lit ? ColorToken.SURFACE_CARD_HOVER : ColorToken.SURFACE_CARD));
        paintCardWash(painter, card, tone, model.playing() || lit ? 0.12f : 0.06f);
        paintRoundedOutline(painter, card, SettingRowLayout.CARD_RADIUS, edge);
        if (model.playing()) {
            paintRoundedOutline(painter, card.inset(2), SettingRowLayout.CARD_RADIUS - 2,
                    Motion.fade(tone, 0.4f));
        }
        paintAccentStripe(painter, card, slots.accent(),
                model.staged() ? theme.color(ColorToken.WARNING)
                        : model.playing() || lit ? tone : toneDeep);

        String[] icon = PresetIcons.of(model.key());
        if (icon != null) {
            SettingRowRenderer.paintGlyph(painter, slots.glyph(), icon, tone, true);
        }
        painter.text(slots.name().x(), slots.name().y(),
                trimToWidth(font, I18n.get(model.key()).toUpperCase(Locale.ROOT), slots.name().width()),
                model.playing() ? tone : theme.color(ColorToken.TEXT_PRIMARY), false);
        painter.smallText(slots.tier().x(), slots.tier().y(), PresetIcons.tier(model.key()),
                theme.color(ColorToken.TEXT_FAINT));
        painter.fill(slots.rule(), theme.color(ColorToken.BORDER_SUBTLE));

        paintSmallLines(painter, font, slots.blurb(), I18n.get(model.key() + ".card"),
                ColorToken.TEXT_MUTED);

        PresetRating.Rating rating = PresetRating.of(model.key());
        paintMeter(painter, font, slots.framesLabel(), slots.framesRow(), KEY_FRAMES,
                rating == null ? 0 : rating.frames(), tone, toneDeep);
        paintMeter(painter, font, slots.looksLabel(), slots.looksRow(), KEY_LOOKS,
                rating == null ? 0 : rating.looks(), tone, toneDeep);

        paintCardStrip(painter, font, card, slots.strip(), model, lit, tone);
        if (model.playing()) {
            paintScanlines(painter, card);
        }
    }

    private void paintCustomBar(SurfacePainter painter, Font font, Rect bar, PresetCardModel.Card model,
                                boolean lit) {
        int tone = PresetIcons.tone(model.key());
        int radius = 5;
        int edge = model.playing() ? tone
                : model.staged() ? theme.color(ColorToken.WARNING)
                : model.suggested() ? theme.color(ColorToken.BORDER_ACCENT)
                : lit ? theme.color(ColorToken.BORDER_STRONG)
                : theme.color(ColorToken.BORDER_SUBTLE);

        paintRoundedFill(painter, bar, radius,
                theme.color(lit ? ColorToken.SURFACE_CARD_HOVER : ColorToken.SURFACE_CARD));
        if (model.playing() || model.staged()) {
            for (Rect span : RoundedScanline.fillSpans(bar, radius)) {
                painter.fill(span, Motion.fade(model.staged()
                        ? theme.color(ColorToken.WARNING) : tone, 0.10f));
            }
        }
        paintRoundedOutline(painter, bar, radius, edge);
        for (Rect span : RoundedScanline.fillSpans(bar, radius)) {
            painter.fill(new Rect(span.x(), span.y(), Math.min(PresetCardLayout.ACCENT_WIDTH,
                    span.width()), span.height()),
                    model.staged() ? theme.color(ColorToken.WARNING)
                            : model.playing() || lit ? tone
                            : Motion.blend(tone, 0xFF000000, 0.45f));
        }

        int left = bar.x() + PresetCardLayout.ACCENT_WIDTH + 9;
        String[] icon = PresetIcons.of(model.key());
        if (icon != null) {
            SettingRowRenderer.paintGlyph(painter,
                    new Rect(left, bar.y() + (bar.height() - PresetCardLayout.GLYPH) / 2,
                            PresetCardLayout.GLYPH, PresetCardLayout.GLYPH), icon, tone, true);
            left += PresetCardLayout.GLYPH + 8;
        }
        int textY = bar.y() + (bar.height() - TEXT_HEIGHT) / 2;
        String name = I18n.get(model.key()).toUpperCase(Locale.ROOT);
        painter.text(left, textY, name,
                model.playing() ? tone : theme.color(ColorToken.TEXT_PRIMARY), false);
        int after = left + font.width(name) + 8;
        painter.smallText(after, textY + 1, PresetIcons.tier(model.key()),
                theme.color(ColorToken.TEXT_FAINT));
        after += smallWidth(painter, font, PresetIcons.tier(model.key())) + 10;

        String measured = OverviewSignals.fpsOf(model.key());
        String status = model.playing() ? I18n.get(KEY_PLAYING_NOW)
                : model.staged() ? I18n.get(KEY_PENDING)
                : model.suggested() ? I18n.get(KEY_SUGGESTED)
                : measured != null ? measured
                : lit ? I18n.get(KEY_SELECT) : I18n.get(KEY_NOT_TRIED);
        int statusArgb = model.playing() ? theme.color(ColorToken.SUCCESS)
                : model.staged() ? theme.color(ColorToken.WARNING)
                : model.suggested() ? theme.color(ColorToken.ACCENT)
                : measured != null ? theme.color(ColorToken.SUCCESS)
                : lit ? tone : theme.color(ColorToken.TEXT_FAINT);
        int statusWidth = smallWidth(painter, font, status);
        int statusX = bar.right() - 10 - statusWidth;
        painter.smallText(statusX, textY + 1, status, statusArgb);

        int room = statusX - 12 - after;
        if (room > 40) {
            painter.smallText(after, textY + 1,
                    smallTrim(painter, font, I18n.get(model.key() + ".card"), room),
                    theme.color(ColorToken.TEXT_MUTED));
        }
    }

    private void paintCardWash(SurfacePainter painter, Rect card, int tone, float alpha) {
        paintCardWash(painter, card, SettingRowLayout.CARD_RADIUS, tone, alpha,
                PresetCardLayout.CARD_PAD + PresetCardLayout.GLYPH + 6);
    }

    private void paintCardWash(SurfacePainter painter, Rect card, int radius, int tone, float alpha,
                               int depth) {
        int argb = Motion.fade(tone, alpha);
        for (Rect span : RoundedScanline.fillSpans(card, radius)) {
            int row = span.y() - card.y();
            if (row < depth) {
                painter.fill(span, argb);
            } else if (row < depth + 4) {
                int from = span.x() + Math.floorMod(span.x() + span.y(), 2);
                for (int x = from; x < span.right(); x += 2) {
                    painter.fill(new Rect(x, span.y(), 1, 1), argb);
                }
            }
        }
    }

    private void paintMeter(SurfacePainter painter, Font font, Rect label, Rect row, String labelKey,
                            int level, int tone, int toneDeep) {
        if (label.isEmpty() || row.isEmpty()) {
            return;
        }
        painter.smallText(label.x(), label.y(), I18n.get(labelKey), theme.color(ColorToken.TEXT_FAINT));
        if (level > 0) {
            String word = I18n.get(labelKey + "." + level);
            painter.smallText(label.right() - smallWidth(painter, font, word), label.y(), word,
                    theme.color(ColorToken.TEXT_SECONDARY));
        }
        int half = row.height() / 2;
        for (int index = 0; index < PresetCardLayout.SEGMENTS; index++) {
            Rect seg = PresetCardLayout.segment(row, index);
            if (seg.isEmpty()) {
                continue;
            }
            if (index < level) {
                painter.fill(new Rect(seg.x(), seg.y(), seg.width(), half), tone);
                painter.fill(new Rect(seg.x(), seg.y() + half, seg.width(), seg.height() - half),
                        toneDeep);
            } else {
                painter.fill(seg, theme.color(ColorToken.IMPACT_TRACK));
                painter.fill(seg.inset(1), theme.color(ColorToken.SURFACE_SUNKEN));
            }
        }
    }

    private void paintCardStrip(SurfacePainter painter, Font font, Rect card, Rect strip,
                                PresetCardModel.Card model, boolean lit, int tone) {
        if (strip.isEmpty()) {
            return;
        }
        String measured = OverviewSignals.fpsOf(model.key());
        int band;
        String text;
        int argb;
        if (model.playing()) {
            band = Motion.fade(tone, 0.2f);
            text = I18n.get(KEY_PLAYING_NOW);
            argb = theme.color(ColorToken.SUCCESS);
        } else if (model.staged()) {
            band = theme.color(ColorToken.WARNING, 0.16f);
            text = I18n.get(KEY_PENDING);
            argb = theme.color(ColorToken.WARNING);
        } else if (model.suggested()) {
            band = Motion.fade(tone, 0.12f);
            text = I18n.get(KEY_SUGGESTED);
            argb = theme.color(ColorToken.ACCENT);
        } else if (measured != null) {
            band = theme.color(ColorToken.SURFACE_SUNKEN, 0.55f);
            text = measured;
            argb = theme.color(ColorToken.SUCCESS);
        } else if (lit) {
            band = Motion.fade(tone, 0.14f);
            text = I18n.get(KEY_SELECT);
            argb = tone;
        } else {
            band = theme.color(ColorToken.SURFACE_SUNKEN, 0.55f);
            text = I18n.get(KEY_NOT_TRIED);
            argb = theme.color(ColorToken.TEXT_FAINT);
        }

        painter.fill(new Rect(strip.x() + 1, strip.y() - 1, strip.width() - 2, 1),
                theme.color(ColorToken.BORDER_SUBTLE));
        for (Rect span : RoundedScanline.fillSpans(card, SettingRowLayout.CARD_RADIUS)) {
            if (span.y() >= strip.y() && span.y() < strip.bottom()) {
                int from = Math.max(span.x() + 2, strip.x());
                int to = Math.min(span.right() - 2, strip.right());
                if (to > from) {
                    painter.fill(new Rect(from, span.y(), to - from, 1), band);
                }
            }
        }
        int width = smallWidth(painter, font, text);
        painter.smallText(strip.x() + (strip.width() - width) / 2,
                strip.y() + (strip.height() - PresetCardLayout.SMALL_LINE) / 2 + 1, text, argb);
    }

    private void paintScanlines(SurfacePainter painter, Rect card) {
        int argb = theme.color(ColorToken.SURFACE_SUNKEN, 0.15f);
        for (Rect span : RoundedScanline.fillSpans(card, SettingRowLayout.CARD_RADIUS)) {
            if ((span.y() - card.y()) % 3 == 0) {
                painter.fill(span, argb);
            }
        }
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

    private void paintGroupRow(SurfacePainter painter, Font font, Rect row,
                               NavPresenter.GroupRow group, int mouseX, int mouseY) {
        if (row.isEmpty()) {
            return;
        }
        boolean lit = row.contains(mouseX, mouseY);
        Rect chevron = SettingRowLayout.groupChevron(row);
        SettingRowRenderer.paintGlyph(painter, chevron,
                group.collapsed() ? CHEVRON_RIGHT : CHEVRON_DOWN,
                theme.color(lit ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_SECONDARY), true);

        Rect label = SettingRowLayout.groupLabel(row);
        painter.text(label.x(), label.y(),
                trimToWidth(font, I18n.get(group.key()).toUpperCase(Locale.ROOT), label.width()),
                theme.color(lit ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_SECONDARY), false);

        String tally = Integer.toString(group.count());
        Rect slot = SettingRowLayout.groupCount(row);
        painter.smallText(slot.right() - smallWidth(painter, font, tally), slot.y(), tally,
                theme.color(ColorToken.TEXT_FAINT));
        painter.fill(SettingRowLayout.groupRule(row), theme.color(ColorToken.BORDER_SUBTLE));
    }

    private void paintInfoPage(SurfacePainter painter, Font font, ShellLayout layout,
                               NavPresenter presenter, int scroll) {
        List<SystemReport.Row> rows = presenter.infoRows();
        List<Rect> boxes = InfoRowLayout.rows(contentBody(layout, presenter),
                presenter.infoSections(), scroll);
        for (int index = 0; index < boxes.size() && index < rows.size(); index++) {
            Rect box = boxes.get(index);
            SystemReport.Row row = rows.get(index);
            if (row.section()) {
                painter.gradient(InfoRowLayout.sectionAccent(box),
                        theme.color(ColorToken.ACCENT_BRIGHT), theme.color(ColorToken.ACCENT_DEEP));
                Rect label = InfoRowLayout.sectionLabel(box);
                painter.smallText(label.x(), label.y(),
                        smallTrim(painter, font, I18n.get(row.labelKey()).toUpperCase(Locale.ROOT),
                                label.width()),
                        theme.color(ColorToken.TEXT_SECONDARY));
                painter.fill(InfoRowLayout.sectionRule(box), theme.color(ColorToken.BORDER_SUBTLE));
                continue;
            }
            Rect label = InfoRowLayout.label(box);
            if (!label.isEmpty()) {
                painter.text(label.x(), label.y(),
                        trimToWidth(font, I18n.get(row.labelKey()), label.width()),
                        theme.color(ColorToken.TEXT_MUTED), false);
            }
            Rect value = InfoRowLayout.value(box);
            if (!value.isEmpty()) {
                String text = trimToWidth(font, row.value(), value.width());
                painter.text(value.right() - font.width(text), value.y(), text,
                        theme.color(ColorToken.TEXT_PRIMARY), false);
            }
        }
    }

    private static FrameGraphLayout.Counts statsCounts(NavPresenter presenter) {
        return new FrameGraphLayout.Counts(presenter.fingerprint().size(), 0,
                presenter.findings().size(),
                presenter.stutters().size(), presenter.stutterProfile().size(),
                presenter.sceneRows().size(), presenter.terrainRows().size(),
                presenter.memoryRows().size(), presenter.machineRows().size(),
                presenter.allocators().size());
    }

    private FrameGraphLayout.Page statsPage(ShellLayout layout, NavPresenter presenter, int scroll) {
        return FrameGraphLayout.page(contentBody(layout, presenter), statsCounts(presenter), scroll,
                layout.breakpoint());
    }

    public int statsMaxScroll(Rect body, NavPresenter presenter, Breakpoint breakpoint) {
        return FrameGraphLayout.maxScroll(body, statsCounts(presenter), breakpoint);
    }

    public Rect statsPauseButton(ShellLayout layout, NavPresenter presenter, int scroll) {
        return statsPage(layout, presenter, scroll).pause();
    }

    public List<Rect> statsStutterRows(ShellLayout layout, NavPresenter presenter, int scroll) {
        return statsPage(layout, presenter, scroll).stutters();
    }

    public List<Rect> statsActions(ShellLayout layout, NavPresenter presenter, int scroll) {
        return statsPage(layout, presenter, scroll).actions();
    }

    private void paintStatsPage(SurfacePainter painter, Font font, ShellLayout layout,
                                NavPresenter presenter, int scroll, int mouseX, int mouseY) {
        FrameGraphLayout.Page page = statsPage(layout, presenter, scroll);
        if (page.plot().isEmpty()) {
            return;
        }
        List<FrameHistory.Bucket> columns = presenter.columns();

        float target = targetFrameMs();
        float ceiling = FrameGraphLayout.ceilingFor(presenter.historyMedian(), target);
        float spikeFloor = FrameSamples.spikeFloor(presenter.historyMedian());

        paintCellGroup(painter, font, page.fingerprint(), presenter.fingerprint(),
                KEY_STATS_GROUP_FINGERPRINT, null);
        paintUnitButton(painter, font, page.pause(), presenter.axisInFps(), mouseX, mouseY);

        paintRoundedFill(painter, page.plot(), 0, theme.color(ColorToken.SURFACE_SUNKEN));
        for (Rect line : FrameGraphLayout.gridlines(page.plot(),
                page.plot().height() >= 140 ? 4 : 2)) {
            painter.fill(line, theme.color(ColorToken.BORDER_SUBTLE));
        }

        int range = theme.color(ColorToken.BORDER_ACCENT);
        int spikeRange = theme.color(ColorToken.BORDER_STRONG);
        float worstGc = 1.0f;
        for (FrameHistory.Bucket bucket : columns) {
            worstGc = Math.max(worstGc, bucket.gcMs());
        }
        for (int index = 0; index < columns.size(); index++) {
            FrameHistory.Bucket bucket = columns.get(index);
            if (bucket.empty()) {
                continue;
            }
            boolean spike = spikeFloor > 0.0f && bucket.max() >= spikeFloor;
            Rect band = FrameGraphLayout.column(page.plot(), index, columns.size(),
                    bucket.min(), bucket.max(), ceiling);
            if (band.isEmpty()) {
                continue;
            }
            painter.fill(band, spike ? spikeRange : range);
            if (bucket.max() > ceiling) {
                painter.fill(new Rect(band.x(), page.plot().y(), band.width(), 1),
                        theme.color(ColorToken.TEXT_PRIMARY));
            }
            Rect mean = FrameGraphLayout.column(page.plot(), index, columns.size(),
                    0.0f, bucket.average(), ceiling);
            painter.fill(mean, theme.color(spike ? ColorToken.WARNING : ColorToken.ACCENT));
            if (spike) {
                painter.fill(FrameGraphLayout.marker(page.markers(), band),
                        theme.color(ColorToken.WARNING));
            }
            painter.fill(FrameGraphLayout.gcColumn(page.gcBand(), band, bucket.gcMs(), worstGc),
                    theme.color(ColorToken.WARNING));
        }
        painter.fill(FrameGraphLayout.baseline(page.plot(), target, ceiling),
                theme.color(ColorToken.SUCCESS));
        paintRoundedOutline(painter, page.plot(), 0, theme.color(ColorToken.BORDER_DEFAULT));
        painter.fill(new Rect(page.gcBand().x(), page.gcBand().bottom(), page.gcBand().width(), 1),
                theme.color(ColorToken.BORDER_SUBTLE));

        if (columns.isEmpty()) {
            paintCentredNotice(painter, font, page.plot(), I18n.get(KEY_STATS_WAITING));
        }
        paintGraphAxis(painter, font, page, ceiling, target, presenter.axisInFps());
        paintTimeAxis(painter, font, page.timeAxis());
        paintLegendKeys(painter, font, page.legendKeys());
        paintSmallLines(painter, font, page.legendLine(), I18n.get(KEY_STATS_SCALE,
                Math.round(FrameHistory.WINDOW_MS / 1000), Math.round(ceiling)),
                ColorToken.TEXT_FAINT);
        paintHoverReadout(painter, font, page, columns, presenter.captureEndMs(), mouseX, mouseY);
        paintVerdictBars(painter, font, page);
        paintFindings(painter, font, page, presenter);
        paintStutters(painter, font, page, presenter, mouseX, mouseY);
        paintStutterProfile(painter, font, page, presenter);
        paintCellGroup(painter, font, page.scene(), presenter.sceneRows(), KEY_STATS_GROUP_SCENE, null);
        paintCellGroup(painter, font, page.terrain(), presenter.terrainRows(),
                KEY_STATS_GROUP_TERRAIN, KEY_STATS_NOTE_TERRAIN);
        paintCellGroup(painter, font, page.memory(), presenter.memoryRows(),
                KEY_STATS_GROUP_MEMORY, KEY_STATS_NOTE_MEMORY);
        if (presenter.allocators().isEmpty() && !page.allocators().isEmpty()) {
            Rect row = page.allocators().get(0);
            painter.fill(row, theme.color(ColorToken.SURFACE_SUNKEN));
            painter.smallText(row.x() + 8, row.y() + (row.height() - PresetCardLayout.SMALL_LINE) / 2,
                    smallTrim(painter, font,
                            I18n.get(KEY_STATS_NO_SAMPLES, presenter.allocatorSamples()),
                            row.width() - 16),
                    theme.color(ColorToken.TEXT_FAINT));
        } else {
            paintProfileRows(painter, font, page.allocators(), presenter.allocators(),
                    KEY_STATS_ALLOCATING);
        }
        paintCellGroup(painter, font, page.machine(), presenter.machineRows(),
                KEY_STATS_GROUP_MACHINE, KEY_STATS_NOTE_MACHINE);
        paintStatsActions(painter, font, page, mouseX, mouseY);

        painter.smallText(page.bottleneck().x(), page.bottleneck().y(),
                smallTrim(painter, font, I18n.get(OverviewSignals.stickyVerdict().messageKey()),
                        page.bottleneck().width()),
                theme.color(ColorToken.TEXT_SECONDARY));
        painter.smallText(page.sampling().x(), page.sampling().y(),
                smallTrim(painter, font, samplingLine(presenter.playSummary()),
                        page.sampling().width()),
                theme.color(ColorToken.TEXT_FAINT));
        paintSmallLines(painter, font, page.legend(), I18n.get(KEY_STATS_LEGEND), ColorToken.TEXT_FAINT);
    }

    private void paintCellGroup(SurfacePainter painter, Font font, FrameGraphLayout.Group group,
                                List<StatsReport.Cell> cells, String titleKey, String noteKey) {
        if (group.heading().isEmpty()) {
            return;
        }
        paintSectionHeading(painter, font, group.heading(), titleKey, titleKey + ".note");
        if (noteKey != null && !group.note().isEmpty()) {
            painter.fill(new Rect(group.note().x(), group.note().y(), 2, group.note().height()),
                    theme.color(ColorToken.BORDER_STRONG));
            paintSmallLines(painter, font,
                    new Rect(group.note().x() + 8, group.note().y() + 2,
                            group.note().width() - 10, group.note().height() - 2),
                    I18n.get(noteKey), ColorToken.TEXT_MUTED);
        }

        for (int index = 0; index < group.cells().size() && index < cells.size(); index++) {
            Rect box = group.cells().get(index);
            StatsReport.Cell cell = cells.get(index);
            painter.fill(box, theme.color(ColorToken.SURFACE_CARD));
            if (cell.alert()) {
                painter.fill(FrameGraphLayout.cellAccent(box), theme.color(ColorToken.WARNING));
            }
            Rect label = FrameGraphLayout.cellLabel(box);
            painter.smallText(label.x(), label.y(),
                    smallTrim(painter, font, I18n.get(cell.labelKey()).toUpperCase(Locale.ROOT),
                            label.width()), theme.color(ColorToken.TEXT_FAINT));
            Rect value = FrameGraphLayout.cellValue(box);
            painter.text(value.x(), value.y(), trimToWidth(font, cell.value(), value.width()),
                    theme.color(cell.alert() ? ColorToken.WARNING : ColorToken.TEXT_PRIMARY), false);
            Rect meta = FrameGraphLayout.cellMeta(box);
            if (!meta.isEmpty() && cell.note() != null && !cell.note().isBlank()) {
                painter.smallText(meta.x(), meta.y(),
                        smallTrim(painter, font, cell.note(), meta.width()),
                        theme.color(ColorToken.TEXT_FAINT));
            }
        }
    }

    private void paintSectionHeading(SurfacePainter painter, Font font, Rect heading,
                                     String titleKey, String noteKey) {
        String title = I18n.get(titleKey).toUpperCase(Locale.ROOT);
        painter.fill(new Rect(heading.x(), heading.y() + 1, 2, PresetCardLayout.SMALL_LINE),
                theme.color(ColorToken.ACCENT));
        painter.smallText(heading.x() + 6, heading.y() + 1, title,
                theme.color(ColorToken.TEXT_SECONDARY));
        int after = heading.x() + 6 + smallWidth(painter, font, title) + 7;
        if (I18n.exists(noteKey) && after < heading.right() - 20) {
            painter.smallText(after, heading.y() + 1,
                    smallTrim(painter, font, I18n.get(noteKey), heading.right() - after),
                    theme.color(ColorToken.TEXT_FAINT));
        }
        painter.fill(new Rect(heading.x(), heading.bottom() - 1, heading.width(), 1),
                theme.color(ColorToken.BORDER_SUBTLE));
    }

    private void paintTimeAxis(SurfacePainter painter, Font font, Rect axis) {
        if (axis.isEmpty()) {
            return;
        }
        int seconds = FrameHistory.WINDOW_MS / 1000;
        String[] marks = {"-" + seconds + " s", "-" + seconds * 2 / 3 + " s",
                "-" + seconds / 3 + " s", I18n.get(KEY_STATS_NOW)};
        for (int index = 0; index < marks.length; index++) {
            int x = axis.x() + index * (axis.width() - 1) / (marks.length - 1);
            int width = smallWidth(painter, font, marks[index]);
            painter.smallText(index == 0 ? x : Math.min(x - width / 2, axis.right() - width),
                    axis.y(), marks[index], theme.color(ColorToken.TEXT_FAINT));
        }
    }

    private void paintLegendKeys(SurfacePainter painter, Font font, Rect legend) {
        ColorToken[] tokens = {ColorToken.ACCENT, ColorToken.BORDER_ACCENT, ColorToken.WARNING,
                ColorToken.SUCCESS, ColorToken.WARNING};
        String[] keys = {KEY_STATS_KEY_AVERAGE, KEY_STATS_KEY_RANGE, KEY_STATS_KEY_SPIKE,
                KEY_STATS_KEY_TARGET, KEY_STATS_KEY_GC};
        for (int index = 0; index < tokens.length; index++) {
            Rect mark = FrameGraphLayout.swatch(legend, index);
            Rect label = FrameGraphLayout.swatchLabel(legend, index);
            if (mark.isEmpty() || label.isEmpty()) {
                return;
            }
            painter.fill(mark, theme.color(tokens[index]));
            painter.smallText(label.x(), label.y(),
                    smallTrim(painter, font, I18n.get(keys[index]), label.width()),
                    theme.color(ColorToken.TEXT_FAINT));
        }
    }

    private void paintStatsActions(SurfacePainter painter, Font font, FrameGraphLayout.Page page,
                                   int mouseX, int mouseY) {
        String[] keys = {KEY_STATS_COPY, KEY_STATS_RESET, KEY_STATS_REBUILD};
        for (int index = 0; index < page.actions().size() && index < keys.length; index++) {
            Rect box = page.actions().get(index);
            boolean lit = box.contains(mouseX, mouseY);
            paintRoundedFill(painter, box, SettingRowLayout.CARD_RADIUS,
                    theme.color(lit ? ColorToken.SURFACE_CARD_HOVER : ColorToken.SURFACE_CARD));
            paintRoundedOutline(painter, box, SettingRowLayout.CARD_RADIUS,
                    theme.color(ColorToken.BORDER_STRONG));
            String text = I18n.get(keys[index]);
            painter.smallText(box.x() + (box.width() - smallWidth(painter, font, text)) / 2,
                    box.y() + (box.height() - PresetCardLayout.SMALL_LINE) / 2, text,
                    theme.color(lit ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_SECONDARY));
        }
    }

    private static float age(long endMs, FrameHistory.Bucket bucket) {
        return Math.max(0.0f, (endMs - bucket.startMs()) / 1000.0f);
    }

    private void paintHoverReadout(SurfacePainter painter, Font font, FrameGraphLayout.Page page,
                                   List<FrameHistory.Bucket> columns, long presenterEnd,
                                   int mouseX, int mouseY) {
        if (columns.isEmpty() || !page.plot().contains(mouseX, mouseY)) {
            return;
        }
        int index = FrameGraphLayout.columnAt(page.plot(), columns.size(), mouseX);
        if (index < 0) {
            return;
        }
        FrameHistory.Bucket bucket = columns.get(index);
        Rect column = FrameGraphLayout.column(page.plot(), index, columns.size(),
                bucket.min(), bucket.max(), 1.0f);
        Rect box = FrameGraphLayout.readout(page.plot(), column);
        if (box.isEmpty()) {
            return;
        }
        paintRoundedFill(painter, box, SettingRowLayout.CARD_RADIUS,
                theme.color(ColorToken.SURFACE_CARD_HOVER));
        paintRoundedOutline(painter, box, SettingRowLayout.CARD_RADIUS,
                theme.color(ColorToken.BORDER_STRONG));
        painter.fill(new Rect(column.x(), page.plot().y(), 1, page.plot().height()),
                theme.color(ColorToken.BORDER_ACCENT));
        painter.smallText(box.x() + 7, box.y() + 4, String.format(Locale.ROOT,
                "-%.1f s   worst %.1f ms", age(presenterEnd, bucket), bucket.max()),
                theme.color(ColorToken.TEXT_FAINT));
        painter.smallText(box.x() + 7, box.y() + 13, String.format(Locale.ROOT,
                "%.1f ms  %.0f fps", bucket.average(), 1000.0f / bucket.average()),
                theme.color(ColorToken.TEXT_PRIMARY));
        painter.smallText(box.x() + 7, box.y() + 22, bucket.gcMs() >= 1.0f
                        ? String.format(Locale.ROOT, "gc %.0f ms", bucket.gcMs())
                        : bucket.uploads() + " uploads",
                theme.color(ColorToken.TEXT_MUTED));
    }

    private void paintVerdictBars(SurfacePainter painter, Font font, FrameGraphLayout.Page page) {
        paintSectionHeading(painter, font, new Rect(page.verdict().x(),
                        page.verdict().y() - FrameGraphLayout.HEADING_H - 6,
                        page.verdict().width(), FrameGraphLayout.HEADING_H),
                KEY_STATS_GROUP_TIME, KEY_STATS_GROUP_TIME + ".note");
        painter.fill(page.verdict(), theme.color(ColorToken.SURFACE_CARD));
        painter.fill(new Rect(page.verdict().x(), page.verdict().y(), 2, page.verdict().height()),
                theme.color(ColorToken.WARNING));
        String verdict = I18n.get(OverviewSignals.stickyVerdict().messageKey());
        painter.text(page.verdict().x() + 9, page.verdict().y() + 5,
                trimToWidth(font, verdict, page.verdict().width() - 18),
                theme.color(ColorToken.WARNING), false);
        int after = page.verdict().x() + 9 + font.width(verdict) + 8;
        if (I18n.exists(KEY_STATS_ADVICE) && after < page.verdict().right() - 30) {
            painter.smallText(after, page.verdict().y() + 6,
                    smallTrim(painter, font, I18n.get(KEY_STATS_ADVICE),
                            page.verdict().right() - after - 6),
                    theme.color(ColorToken.TEXT_MUTED));
        }

        double frame = FrameTimer.frameMs();
        double gpu = FrameTimer.gpuMs();
        double cpu = FrameTimer.cpuBusyMs();
        if (frame > 0.0) {
            painter.smallText(page.frameLabel().x(), page.frameLabel().y(),
                    I18n.get(KEY_STATS_BAR_FRAME, String.format(Locale.ROOT, "%.1f", frame)),
                    theme.color(ColorToken.TEXT_FAINT));
            paintSplitBar(painter, font, page.frameBar(),
                    new double[] {Math.max(0.0, cpu), Math.max(0.0, gpu),
                            Math.max(0.0, frame - Math.max(0.0, cpu) - Math.max(0.0, gpu))},
                    new ColorToken[] {ColorToken.ACCENT_DEEP, ColorToken.ACCENT, ColorToken.IMPACT_TRACK},
                    new String[] {"cpu", "gpu", "wait"});
        }
        double upload = FrameTimer.uploadMs();
        double setup = FrameTimer.setupMs();
        double terrain = FrameTimer.terrainMs();
        if (upload + setup + terrain > 0.0) {
            painter.smallText(page.threadLabel().x(), page.threadLabel().y(),
                    I18n.get(KEY_STATS_BAR_THREAD,
                            String.format(Locale.ROOT, "%.1f", upload + setup + terrain)),
                    theme.color(ColorToken.TEXT_FAINT));
            paintSplitBar(painter, font, page.threadBar(),
                    new double[] {upload, setup, terrain},
                    new ColorToken[] {ColorToken.ACCENT_BRIGHT, ColorToken.ACCENT_DEEP, ColorToken.BORDER_ACCENT},
                    new String[] {"upload", "setup", "terrain"});
        }
    }

    private void paintSplitBar(SurfacePainter painter, Font font, Rect bar, double[] parts,
                               ColorToken[] tokens, String[] labels) {
        if (bar.isEmpty()) {
            return;
        }
        double total = 0.0;
        for (double part : parts) {
            total += Math.max(0.0, part);
        }
        painter.fill(bar, theme.color(ColorToken.SURFACE_SUNKEN));
        if (total <= 0.0) {
            return;
        }
        int x = bar.x();
        for (int index = 0; index < parts.length; index++) {
            int width = index == parts.length - 1 ? bar.right() - x
                    : (int) Math.round(bar.width() * Math.max(0.0, parts[index]) / total);
            if (width <= 0) {
                continue;
            }
            Rect slice = new Rect(x, bar.y(), width, bar.height());
            painter.fill(slice, theme.color(tokens[index]));
            String text = String.format(Locale.ROOT, "%s %.1f", labels[index], Math.max(0.0, parts[index]));
            int textWidth = smallWidth(painter, font, text);
            if (textWidth < width - 6) {
                painter.smallText(x + (width - textWidth) / 2, bar.y() + 4, text,
                        theme.color(ColorToken.SURFACE_BASE));
            }
            x += width;
        }
    }

    private void paintUnitButton(SurfacePainter painter, Font font, Rect box, boolean fps,
                                 int mouseX, int mouseY) {
        if (box.isEmpty()) {
            return;
        }
        boolean lit = box.contains(mouseX, mouseY);
        paintRoundedFill(painter, box, SettingRowLayout.CARD_RADIUS,
                theme.color(lit ? ColorToken.SURFACE_CARD_HOVER : ColorToken.SURFACE_CARD));
        paintRoundedOutline(painter, box, SettingRowLayout.CARD_RADIUS,
                theme.color(ColorToken.BORDER_STRONG));
        String text = I18n.get(fps ? KEY_STATS_UNIT_FPS : KEY_STATS_UNIT_MS);
        painter.smallText(box.x() + (box.width() - smallWidth(painter, font, text)) / 2,
                box.y() + (box.height() - PresetCardLayout.SMALL_LINE) / 2, text,
                theme.color(lit ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_SECONDARY));
    }

    private void paintFindings(SurfacePainter painter, Font font, FrameGraphLayout.Page page,
                               NavPresenter presenter) {
        if (page.findingHeading().isEmpty()) {
            return;
        }
        paintSectionHeading(painter, font, page.findingHeading(),
                KEY_STATS_GROUP_FINDINGS, KEY_STATS_GROUP_FINDINGS + ".note");
        List<Diagnosis.Finding> findings = presenter.findings();
        for (int index = 0; index < page.findings().size() && index < findings.size(); index++) {
            Rect box = page.findings().get(index);
            Diagnosis.Finding finding = findings.get(index);
            ColorToken tone = finding.severe() ? ColorToken.WARNING : ColorToken.ACCENT;
            painter.fill(box, theme.color(ColorToken.SURFACE_CARD));
            painter.fill(new Rect(box.x(), box.y(), 2, box.height()), theme.color(tone));
            painter.text(box.x() + 9, box.y() + 4,
                    trimToWidth(font, I18n.get(finding.titleKey()), box.width() - 18),
                    theme.color(tone), false);
            paintSmallLines(painter, font,
                    new Rect(box.x() + 9, box.y() + 15, box.width() - 18, PresetCardLayout.SMALL_LINE * 2),
                    finding.detail(), ColorToken.TEXT_MUTED);
        }
    }

    private void paintStutters(SurfacePainter painter, Font font, FrameGraphLayout.Page page,
                               NavPresenter presenter, int mouseX, int mouseY) {
        List<FrameHistory.Bucket> stutters = presenter.stutters();
        if (!page.stutterHead().isEmpty()) {
            paintSectionHeading(painter, font, page.stutterHeading(),
                    KEY_STATS_GROUP_STUTTERS, KEY_STATS_GROUP_STUTTERS + ".note");
            paintSmallLines(painter, font, page.stutterCaption(),
                    I18n.get(KEY_STATS_STUTTER_CAPTION), ColorToken.TEXT_FAINT);
            String[] heads = {KEY_STATS_COL_WHEN, KEY_STATS_COL_WORST, KEY_STATS_COL_GC,
                    KEY_STATS_COL_UPLOADS, KEY_STATS_COL_BUILDS, KEY_STATS_COL_CAUSE};
            for (int index = 0; index < heads.length; index++) {
                Rect slot = FrameGraphLayout.stutterColumn(page.stutterHead(), index);
                painter.smallText(slot.x(), slot.y(),
                        smallTrim(painter, font, I18n.get(heads[index]).toUpperCase(Locale.ROOT),
                                slot.width() - 4),
                        theme.color(ColorToken.TEXT_FAINT));
            }
        }
        for (int index = 0; index < page.stutters().size() && index < stutters.size(); index++) {
            Rect row = page.stutters().get(index);
            FrameHistory.Bucket bucket = stutters.get(index);
            boolean picked = index == presenter.selectedStutter();
            boolean lit = row.contains(mouseX, mouseY);
            painter.fill(row, theme.color(picked ? ColorToken.SURFACE_NAV_ACTIVE
                    : lit ? ColorToken.SURFACE_CARD_HOVER
                    : index % 2 == 0 ? ColorToken.SURFACE_CARD : ColorToken.SURFACE_SUNKEN));
            if (picked) {
                painter.fill(new Rect(row.x(), row.y(), 2, row.height()),
                        theme.color(ColorToken.ACCENT));
            }
            int top = row.y() + (row.height() - PresetCardLayout.SMALL_LINE) / 2;

            cell(painter, font, FrameGraphLayout.stutterColumn(row, 0), top,
                    String.format(Locale.ROOT, "-%.1f s", age(presenter.captureEndMs(), bucket)),
                    picked ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_MUTED);
            cell(painter, font, FrameGraphLayout.stutterColumn(row, 1), top,
                    String.format(Locale.ROOT, "%.1f ms", bucket.max()), ColorToken.WARNING);
            cell(painter, font, FrameGraphLayout.stutterColumn(row, 2), top,
                    bucket.gcMs() >= 1.0f ? Math.round(bucket.gcMs()) + " ms" : "-",
                    bucket.gcMs() >= 1.0f ? ColorToken.WARNING : ColorToken.TEXT_FAINT);
            cell(painter, font, FrameGraphLayout.stutterColumn(row, 3), top,
                    bucket.uploads() > 0 ? Integer.toString(bucket.uploads()) : "-",
                    bucket.uploads() > 0 ? ColorToken.ACCENT_BRIGHT : ColorToken.TEXT_FAINT);

            cell(painter, font, FrameGraphLayout.stutterColumn(row, 4), top,
                    bucket.builds() > 0 ? Integer.toString(bucket.builds()) : "-",
                    bucket.builds() > 0 ? ColorToken.SUCCESS : ColorToken.TEXT_FAINT);

            Rect causeSlot = FrameGraphLayout.stutterColumn(row, 5);
            String cause = bucket.gcMs() >= 1.0f ? I18n.get(KEY_STATS_TAG_GC)
                    : bucket.uploads() > 0 ? I18n.get(KEY_STATS_TAG_UPLOAD)
                    : I18n.get(KEY_STATS_CAUSE_NONE);
            ColorToken tone = bucket.gcMs() >= 1.0f ? ColorToken.WARNING
                    : bucket.uploads() > 0 ? ColorToken.ACCENT_BRIGHT : ColorToken.TEXT_FAINT;
            int width = smallWidth(painter, font, cause) + 8;
            if (width < causeSlot.width()) {
                paintRoundedOutline(painter, new Rect(causeSlot.x(), row.y() + 1, width,
                        row.height() - 2), 2, theme.color(tone));
            }
            cell(painter, font, new Rect(causeSlot.x() + 4, causeSlot.y(),
                    causeSlot.width() - 4, causeSlot.height()), top, cause, tone);
        }
    }

    private void cell(SurfacePainter painter, Font font, Rect slot, int top, String text,
                      ColorToken token) {
        if (slot.isEmpty()) {
            return;
        }
        painter.smallText(slot.x(), top, smallTrim(painter, font, text, slot.width() - 4),
                theme.color(token));
    }

    private void paintProfileRows(SurfacePainter painter, Font font, List<Rect> boxes,
                                  List<StackSampler.Frame> frames, String tailKey) {
        for (int index = 0; index < boxes.size() && index < frames.size(); index++) {
            Rect row = boxes.get(index);
            StackSampler.Frame frame = frames.get(index);
            painter.fill(row, theme.color(ColorToken.SURFACE_CARD));
            painter.fill(new Rect(row.x(), row.y(), Math.round(row.width() * frame.share()),
                    row.height()), theme.color(ColorToken.IMPACT_TRACK));
            int top = row.y() + (row.height() - TEXT_HEIGHT) / 2;
            painter.text(row.x() + 8, top, Math.round(frame.share() * 100) + "%",
                    theme.color(ColorToken.ACCENT_BRIGHT), false);
            String tail = I18n.get(tailKey);
            int width = smallWidth(painter, font, tail);
            painter.text(row.x() + 46, top,
                    trimToWidth(font, frame.name(), row.width() - 60 - width),
                    theme.color(ColorToken.TEXT_PRIMARY), false);
            painter.smallText(row.right() - width - 7, top + 1, tail,
                    theme.color(ColorToken.TEXT_FAINT));
        }
    }

    private void paintStutterProfile(SurfacePainter painter, Font font, FrameGraphLayout.Page page,
                                     NavPresenter presenter) {
        if (page.profileHead().isEmpty()) {
            return;
        }
        painter.smallText(page.profileHead().x(), page.profileHead().y(),
                smallTrim(painter, font, I18n.get(KEY_STATS_PROFILE, presenter.stutterSamples()),
                        page.profileHead().width()),
                theme.color(ColorToken.TEXT_FAINT));
        List<StackSampler.Frame> frames = presenter.stutterProfile();
        for (int index = 0; index < page.profile().size() && index < frames.size(); index++) {
            Rect row = page.profile().get(index);
            StackSampler.Frame frame = frames.get(index);
            painter.fill(row, theme.color(ColorToken.SURFACE_CARD));
            painter.fill(new Rect(row.x(), row.y(), Math.round(row.width() * frame.share()),
                    row.height()), theme.color(ColorToken.IMPACT_TRACK));
            int top = row.y() + (row.height() - TEXT_HEIGHT) / 2;
            painter.text(row.x() + 8, top, Math.round(frame.share() * 100) + "%",
                    theme.color(ColorToken.ACCENT_BRIGHT), false);
            String samples = I18n.get(KEY_STATS_SAMPLES, frame.samples());
            int tail = smallWidth(painter, font, samples);
            painter.text(row.x() + 46, top,
                    trimToWidth(font, frame.name(), row.width() - 60 - tail),
                    theme.color(ColorToken.TEXT_PRIMARY), false);
            painter.smallText(row.right() - tail - 7, top + 1, samples,
                    theme.color(ColorToken.TEXT_FAINT));
        }
    }

    private void paintGraphAxis(SurfacePainter painter, Font font, FrameGraphLayout.Page page,
                                float ceiling, float target, boolean fpsAxis) {
        int divisions = page.plot().height() >= 140 ? 4 : 2;
        for (int index = 0; index <= divisions; index++) {
            Rect slot = FrameGraphLayout.axisLabel(page.axis(), index, divisions);
            if (slot.isEmpty()) {
                continue;
            }
            float ms = ceiling * index / divisions;
            String text = index == 0 ? "0"
                    : fpsAxis ? String.format(Locale.ROOT, "%.0f fps", 1000.0f / ms)
                    : String.format(Locale.ROOT, "%.1f ms", ms);
            painter.smallText(slot.right() - smallWidth(painter, font, text), slot.y(), text,
                    theme.color(ColorToken.TEXT_FAINT));
        }
        Rect line = FrameGraphLayout.baseline(page.plot(), target, ceiling);
        if (!line.isEmpty()) {
            String text = String.format(Locale.ROOT, "%.0f fps", 1000.0f / target);
            painter.smallText(line.right() - smallWidth(painter, font, text) - 3, line.y() - 8, text,
                    theme.color(ColorToken.SUCCESS));
        }
    }

    private void paintStatTiles(SurfacePainter painter, Font font, FrameGraphLayout.Page page,
                                FrameSamples.Summary play) {
        for (int index = 0; index < page.tiles().size() && index < STAT_TILES.length; index++) {
            Rect tile = page.tiles().get(index);
            paintRoundedFill(painter, tile, SettingRowLayout.CARD_RADIUS,
                    theme.color(ColorToken.SURFACE_CARD));
            Rect label = FrameGraphLayout.tileLabel(tile);
            painter.smallText(label.x() + 6, label.y(),
                    smallTrim(painter, font, I18n.get(STAT_TILES[index]), label.width() - 12),
                    theme.color(ColorToken.TEXT_FAINT));
            Rect value = FrameGraphLayout.tileValue(tile);
            String text = statValue(index, play);
            painter.text(value.x() + 6, value.y(), trimToWidth(font, text, value.width() - 12),
                    theme.color(text.equals(DASH) ? ColorToken.TEXT_MUTED : ColorToken.TEXT_PRIMARY), false);
        }
    }

    private static String statValue(int index, FrameSamples.Summary play) {
        if (play.count() < FrameSamples.READY_AT) {
            return DASH;
        }
        return switch (index) {
            case 0 -> String.format(Locale.ROOT, "%.0f fps", 1000.0f / play.average());
            case 1 -> String.format(Locale.ROOT, "%.1f ms", play.median());
            case 2 -> play.low1() < 0 ? DASH : String.format(Locale.ROOT, "%.0f fps", 1000.0f / play.low1());
            case 3 -> play.low01() < 0 ? DASH : String.format(Locale.ROOT, "%.0f fps", 1000.0f / play.low01());
            case 4 -> String.format(Locale.ROOT, "%.1f ms", play.p95());
            default -> Integer.toString(play.spikes());
        };
    }

    private static String samplingLine(FrameSamples.Summary play) {
        return play.count() < FrameSamples.READY_AT
                ? I18n.get("vulkanmod.ui.overview.sampling", play.count(), FrameSamples.READY_AT)
                : I18n.get("vulkanmod.ui.overview.from_frames", play.count());
    }

    private static float targetFrameMs() {
        try {
            int limit = Minecraft.getInstance().options.framerateLimit().get();
            if (limit > 0 && limit < 260) {
                return 1000.0f / limit;
            }
            int refresh = Minecraft.getInstance().getWindow().getRefreshRate();
            return refresh > 0 ? 1000.0f / refresh : 16.7f;
        } catch (Throwable unavailable) {
            return 16.7f;
        }
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

    private int lastIndicatorScroll = Integer.MIN_VALUE;
    private long indicatorGlow;

    private void paintScrollIndicator(SurfacePainter painter, ShellLayout layout, NavPresenter presenter,
                                      int contentScroll, long deltaMs) {
        if (contentScroll != lastIndicatorScroll) {
            this.lastIndicatorScroll = contentScroll;
            this.indicatorGlow = 500L;
        } else {
            this.indicatorGlow = Math.max(0L, indicatorGlow - deltaMs);
        }
        ScrollIndicator indicator = contentScrollIndicator(layout, presenter, contentScroll);
        if (!indicator.visible()) {
            return;
        }
        float lit = Motion.step(indicatorGlow / 500.0f, 3);
        int radius = indicator.track().width() / 2;
        paintRoundedFill(painter, indicator.track(), radius,
                theme.color(ColorToken.BORDER_SUBTLE, 0.5f + 0.5f * lit));
        paintRoundedFill(painter, indicator.thumb(), radius,
                Motion.blend(theme.color(ColorToken.BORDER_ACCENT), theme.color(ColorToken.ACCENT), lit));
    }

    private void paintScrollIndicator(SurfacePainter painter, ScrollIndicator indicator) {
        if (!indicator.visible()) {
            return;
        }
        int radius = indicator.track().width() / 2;
        paintRoundedFill(painter, indicator.track(), radius, theme.color(ColorToken.BORDER_SUBTLE));
        paintRoundedFill(painter, indicator.thumb(), radius, theme.color(ColorToken.BORDER_ACCENT));
    }

    private void paintSettings(GuiGraphics graphics, SurfacePainter painter, Font font, ShellLayout layout,
                               NavPresenter presenter,
                               int contentScroll, int mouseX, int mouseY, SettingId dragged,
                               boolean keyboardFocus, long deltaMs) {
        List<SettingMeta> settings = presenter.settings();
        if (settings.isEmpty()
                && paintEmptyPage(graphics, painter, font, layout, presenter, keyboardFocus, deltaMs,
                        contentScroll, mouseX, mouseY)) {
            return;
        }

        List<Rect> boxes = settingRowBoxes(layout, presenter, contentScroll);
        List<NavPresenter.ContentRow> rows = presenter.contentRows();
        SettingsCatalog catalog = presenter.catalog();
        String focusedId = focusedIn(presenter, NavPresenter.REGION_CONTENT);
        SettingMeta pointed = hoveredSetting(layout, presenter, contentScroll, mouseX, mouseY);
        for (int i = 0; i < boxes.size() && i < rows.size(); i++) {
            Rect box = boxes.get(i);
            painter.setOffset(0, 0);
            if (rows.get(i) instanceof NavPresenter.GroupRow group) {
                paintGroupRow(painter, font, box, group, mouseX, mouseY);
                continue;
            }
            SettingMeta meta = ((NavPresenter.SettingRow) rows.get(i)).meta();
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
                    onRow && SettingRowLayout.cyclerNextBox(box).contains(mouseX, mouseY), deltaMs);

            if (keyboardFocus && key.equals(focusedId)) {
                paintRoundedOutline(painter, SettingRowLayout.cardBox(box), SettingRowLayout.CARD_RADIUS,
                        theme.color(ColorToken.ACCENT));
            }
        }
        painter.setOffset(0, 0);
    }

    private boolean paintEmptyPage(GuiGraphics graphics, SurfacePainter painter, Font font,
                                   ShellLayout layout, NavPresenter presenter,
                                   boolean keyboardFocus, long deltaMs,
                                   int contentScroll, int mouseX, int mouseY) {
        RouteId current = presenter.stack().current();
        if (FAVORITES.equals(current)) {
            paintCentredNotice(painter, font, contentBody(layout, presenter), I18n.get(KEY_FAVORITES_EMPTY));
            return true;
        }
        if (MODS.equals(current)) {
            paintCentredNotice(painter, font, contentBody(layout, presenter), I18n.get(KEY_MODS_EMPTY));
            return true;
        }
        if (PLUGINS.equals(current)) {
            paintPluginsPage(graphics, painter, font, layout, presenter, keyboardFocus, deltaMs,
                    contentScroll, mouseX, mouseY);
            return true;
        }
        if (PLUGINS.equals(current.parent())) {
            paintCentredNotice(painter, font, contentBody(layout, presenter), I18n.get(KEY_PLUGIN_EMPTY));
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

    public List<Rect> pluginRowBoxes(ShellLayout layout, NavPresenter presenter, int scroll) {
        PluginPageLayout.Block block = pluginPage(layout, presenter, scroll).plugins();
        return block.placeholder() ? List.of() : shiftedRows(block.rows(), 0);
    }

    private List<Rect> shiftedRows(List<Rect> rows, int indexBase) {
        if (pageElapsed >= Motion.SEQUENCE_MS && rowsElapsed >= Motion.SEQUENCE_MS) {
            return rows;
        }
        List<Rect> shifted = new ArrayList<>(rows.size());
        for (int index = 0; index < rows.size(); index++) {
            shifted.add(rows.get(index).translated(
                    Motion.slide(Motion.rowReveal(pageElapsed, indexBase + index), pageDirection,
                            Motion.PAGE_TRAVEL), 0));
        }
        return List.copyOf(shifted);
    }

    private PluginPageLayout.Page pluginPage(ShellLayout layout, NavPresenter presenter, int scroll) {
        return PluginPageLayout.page(contentBody(layout, presenter), presenter.pluginPages().size(),
                presenter.catalog().modIds().size(), scroll, layout.breakpoint(),
                presenter.expandedPluginIndex());
    }

    public Rect pluginShowcaseFrame(ShellLayout layout, NavPresenter presenter, int scroll) {
        return pluginPage(layout, presenter, scroll).showcase();
    }

    private void paintPluginsPage(GuiGraphics graphics, SurfacePainter painter, Font font,
                                  ShellLayout layout, NavPresenter presenter,
                                  boolean keyboardFocus, long deltaMs,
                                  int scroll, int mouseX, int mouseY) {
        PluginPageLayout.Page page = pluginPage(layout, presenter, scroll);
        if (!page.empty().isEmpty()) {
            paintPluginEmptyCard(painter, font, page.empty());
            return;
        }

        List<NavPresenter.PluginPage> plugins = presenter.pluginPages();
        PluginPageLayout.Block block = page.plugins();
        paintPluginHeading(painter, font, block, I18n.get(KEY_PLUGINS_INSTALLED),
                plugins.size(), ColorToken.TEXT_SECONDARY, true);
        if (block.placeholder()) {
            paintPluginNote(painter, font, block.rows().get(0), I18n.get(KEY_PLUGINS_NONE));
        } else {
            String focusedId = focusedIn(presenter, NavPresenter.REGION_CONTENT);
            List<Rect> rows = shiftedRows(block.rows(), 0);
            int expanded = presenter.expandedPluginIndex();
            for (int index = 0; index < rows.size() && index < plugins.size(); index++) {
                paintPluginRow(painter, font, rows.get(index), plugins.get(index),
                        keyboardFocus ? focusedId : null, index == expanded, deltaMs, mouseX, mouseY);
            }
            if (!page.showcase().isEmpty() && expanded >= 0 && expanded < plugins.size()) {
                painter.flush();
                paintShowcase(graphics, painter, font, page.showcase(), plugins.get(expanded),
                        deltaMs, mouseX, mouseY);
            }
        }

        List<String> mods = presenter.catalog().modIds();
        PluginPageLayout.Block modBlock = page.mods();
        if (modBlock.rows().isEmpty()) {
            return;
        }
        paintPluginHeading(painter, font, modBlock, I18n.get(KEY_PLUGINS_MODS),
                mods.size(), ColorToken.TEXT_FAINT, false);
        List<Rect> modRows = shiftedRows(modBlock.rows(), plugins.size());
        for (int index = 0; index < modRows.size() && index < mods.size(); index++) {
            paintPluginNote(painter, font, modRows.get(index),
                    NavPresenter.modName(mods.get(index)));
        }
    }

    private void paintShowcase(GuiGraphics graphics, SurfacePainter painter, Font font, Rect frame,
                               NavPresenter.PluginPage plugin, long deltaMs, int mouseX, int mouseY) {
        if (plugin.id().equals(showcaseShownId)) {
            this.showcaseElapsed = Math.min(9_999L, showcaseElapsed + Math.max(0L, deltaMs));
        } else {
            this.showcaseShownId = plugin.id();
            this.showcaseElapsed = 0L;
        }
        float reveal = motionEnabled() ? Motion.easeOut(showcaseElapsed, 200) : 1.0f;
        MenuPlugins.Showcase art = MenuPlugins.showcaseOf(plugin.id());
        PluginShowcase.Slots slots = PluginShowcase.slots(frame);

        if (art.banner() != null) {
            PluginShowcase.Crop crop = PluginShowcase.cover(frame.width(), frame.height(),
                    art.banner().width(), art.banner().height());
            graphics.setColor(1.0f, 1.0f, 1.0f, reveal);
            graphics.blit(art.banner().texture(), frame.x(), frame.y(), frame.width(), frame.height(),
                    crop.u(), crop.v(), crop.uw(), crop.vh(),
                    art.banner().width(), art.banner().height());
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        painter.setAlpha(reveal);
        try {
            if (art.banner() == null) {
                int tone = PresetIcons.tone(null);
                painter.fill(frame, theme.color(ColorToken.SURFACE_SUNKEN));
                painter.gradient(frame, Motion.fade(tone, 0.0f), Motion.fade(tone, 0.3f));
                int seed = plugin.id().hashCode();
                for (int dot = 0; dot < 70; dot++) {
                    int h = seed * 31 + dot * 0x9E3779B9;
                    int dx = Math.floorMod(h, Math.max(1, frame.width() - 2));
                    int dy = Math.floorMod(h >> 8, Math.max(1, frame.height() / 3));
                    painter.fill(new Rect(frame.x() + 1 + dx,
                            frame.bottom() - 2 - dy, 1 + Math.floorMod(h >> 16, 2), 1),
                            Motion.fade(tone, 0.25f + 0.04f * Math.floorMod(h >> 20, 8)));
                }
            }

            int shadeTop = frame.y() + frame.height() / 4;
            painter.gradient(new Rect(frame.x(), shadeTop, frame.width(), frame.bottom() - shadeTop),
                    theme.color(ColorToken.SURFACE_BASE, 0.0f), theme.color(ColorToken.SURFACE_BASE, 0.92f));
            painter.fill(new Rect(frame.x(), frame.y(), frame.width(), 1),
                    theme.color(ColorToken.BORDER_ACCENT));
            painter.fill(new Rect(frame.x(), frame.bottom() - 1, frame.width(), 1),
                    theme.color(ColorToken.BORDER_ACCENT));
            painter.fill(new Rect(frame.x(), frame.y() + 1, 1, frame.height() - 2),
                    theme.color(ColorToken.BORDER_ACCENT));
            painter.fill(new Rect(frame.right() - 1, frame.y() + 1, 1, frame.height() - 2),
                    theme.color(ColorToken.BORDER_ACCENT));
            for (int y = frame.y() + 2; y < frame.bottom() - 2; y += 3) {
                painter.fill(new Rect(frame.x() + 1, y, frame.width() - 2, 1),
                        theme.color(ColorToken.SURFACE_SUNKEN, 0.08f));
            }
            painter.flush();

            painter.setOffset(0, Motion.slide(reveal, 1, 6));
            paintShowcaseContent(graphics, painter, font, plugin, art, slots, reveal, mouseX, mouseY);
            painter.flush();
        } finally {
            painter.setOffset(0, 0);
            painter.setAlpha(1.0f);
        }
    }

    private void paintShowcaseContent(GuiGraphics graphics, SurfacePainter painter, Font font,
                                      NavPresenter.PluginPage plugin, MenuPlugins.Showcase art,
                                      PluginShowcase.Slots slots, float reveal,
                                      int mouseX, int mouseY) {
        Rect icon = slots.icon();
        if (!icon.isEmpty()) {
            painter.fill(icon.inset(-2), theme.color(ColorToken.SURFACE_SUNKEN, 0.85f));
            painter.fill(icon.inset(-1), theme.color(ColorToken.ACCENT_DEEP));
            painter.fill(icon, theme.color(ColorToken.SURFACE_SUNKEN));
            if (art.icon() != null) {
                painter.flush();
                PluginShowcase.Crop crop = PluginShowcase.cover(icon.width(), icon.height(),
                        art.icon().width(), art.icon().height());
                graphics.setColor(1.0f, 1.0f, 1.0f, reveal);
                graphics.blit(art.icon().texture(), icon.x(), icon.y(), icon.width(), icon.height(),
                        crop.u(), crop.v(), crop.uw(), crop.vh(),
                        art.icon().width(), art.icon().height());
                graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                String letter = plugin.name().isEmpty() ? "?"
                        : plugin.name().substring(0, 1).toUpperCase(Locale.ROOT);
                painter.text(icon.x() + (icon.width() - font.width(letter)) / 2,
                        icon.y() + (icon.height() - TEXT_HEIGHT) / 2, letter,
                        theme.color(ColorToken.ACCENT), false);
            }
        }

        if (!slots.title().isEmpty()) {
            painter.text(slots.title().x(), slots.title().y(),
                    trimToWidth(font, plugin.name(), slots.title().width()),
                    theme.color(ColorToken.TEXT_PRIMARY), true);
        }
        if (!slots.byline().isEmpty() && art.byline() != null) {
            painter.smallText(slots.byline().x(), slots.byline().y(),
                    smallTrim(painter, font, art.byline().toUpperCase(Locale.ROOT),
                            slots.byline().width()),
                    theme.color(ColorToken.TEXT_SECONDARY));
        }
        if (!slots.desc().isEmpty()) {
            String description = art.description() != null ? art.description()
                    : plugin.groups().isEmpty() ? I18n.get(KEY_PLUGIN_NO_SETTINGS)
                    : I18n.get(KEY_PLUGIN_SETTINGS, plugin.groups().size());
            paintSmallLines(painter, font, slots.desc(), description, ColorToken.TEXT_SECONDARY);
        }
        if (!slots.tags().isEmpty()) {
            int x = slots.tags().x();
            List<String> tags = new ArrayList<>(art.tags());
            tags.add(I18n.get(KEY_PLUGINS_REQUIRES));
            for (int index = 0; index < tags.size(); index++) {
                String label = tags.get(index).toUpperCase(Locale.ROOT);
                boolean dim = index == tags.size() - 1;
                int width = smallWidth(painter, font, label) + 10;
                if (x + width > slots.tags().right()) {
                    break;
                }
                Rect chip = new Rect(x, slots.tags().y(), width, slots.tags().height());
                painter.fill(chip, theme.color(ColorToken.SURFACE_CHROME, 0.85f));
                painter.fill(new Rect(chip.x(), chip.y(), chip.width(), 1),
                        theme.color(dim ? ColorToken.BORDER_DEFAULT : ColorToken.BORDER_ACCENT));
                painter.fill(new Rect(chip.x(), chip.bottom() - 1, chip.width(), 1),
                        theme.color(dim ? ColorToken.BORDER_DEFAULT : ColorToken.BORDER_ACCENT));
                painter.fill(new Rect(chip.x(), chip.y() + 1, 1, chip.height() - 2),
                        theme.color(dim ? ColorToken.BORDER_DEFAULT : ColorToken.BORDER_ACCENT));
                painter.fill(new Rect(chip.right() - 1, chip.y() + 1, 1, chip.height() - 2),
                        theme.color(dim ? ColorToken.BORDER_DEFAULT : ColorToken.BORDER_ACCENT));
                painter.smallText(chip.x() + 5, chip.y() + 2, label,
                        theme.color(dim ? ColorToken.TEXT_SECONDARY : ColorToken.ACCENT_BRIGHT));
                x += width + 5;
            }
        }
        if (!slots.button().isEmpty()) {
            paintBarButton(painter, font, slots.button(), I18n.get(KEY_PLUGINS_SETTINGS),
                    ColorToken.ACCENT, mouseX, mouseY);
        }
    }

    private void paintPluginHeading(SurfacePainter painter, Font font, PluginPageLayout.Block block,
                                    String label, int count, ColorToken token, boolean lead) {
        if (block.heading().isEmpty()) {
            return;
        }
        painter.smallText(block.heading().x(), block.heading().y(),
                smallTrim(painter, font, label, block.heading().width()), theme.color(token));

        String tally = Integer.toString(count);
        Rect slot = block.count();
        painter.smallText(slot.right() - smallWidth(painter, font, tally), slot.y(), tally,
                theme.color(ColorToken.TEXT_FAINT));

        painter.fill(block.rule(), theme.color(ColorToken.BORDER_SUBTLE));
        if (lead) {
            painter.fill(PluginPageLayout.ruleLead(block.rule()), theme.color(ColorToken.ACCENT_DEEP));
        }
    }

    private void paintPluginRow(SurfacePainter painter, Font font, Rect row,
                                NavPresenter.PluginPage plugin, String focusedId, boolean expanded,
                                long deltaMs, int mouseX, int mouseY) {
        if (row.isEmpty()) {
            return;
        }
        boolean on = plugin.enabled();
        boolean hovered = row.contains(mouseX, mouseY);
        String hoverKey = "plugin:" + plugin.id();
        float lit = hover.advance(hoverKey, hovered, deltaMs);
        paintRoundedFill(painter, row, SettingRowLayout.CARD_RADIUS, Motion.blend(
                theme.color(on ? ColorToken.SURFACE_CARD : ColorToken.SURFACE_SUNKEN),
                theme.color(on ? ColorToken.SURFACE_CARD_HOVER : ColorToken.SURFACE_CARD), lit));
        if (on) {
            paintCardWash(painter, row, SettingRowLayout.CARD_RADIUS,
                    theme.color(ColorToken.ACCENT), 0.03f + 0.05f * lit, row.height() / 2);
        }
        paintRoundedOutline(painter, row, SettingRowLayout.CARD_RADIUS, Motion.blend(
                theme.color(on ? ColorToken.BORDER_DEFAULT : ColorToken.BORDER_SUBTLE),
                theme.color(ColorToken.BORDER_ACCENT), lit));

        PluginPageLayout.Slots slots = PluginPageLayout.slots(row, plugin.toggleable());
        paintAccentStripe(painter, row, slots.accent(),
                theme.color(on ? ColorToken.ACCENT : ColorToken.BORDER_STRONG));

        if (!slots.name().isEmpty()) {
            painter.text(slots.name().x(), slots.name().y(),
                    trimToWidth(font, plugin.name(), slots.name().width()),
                    theme.color(on ? ColorToken.TEXT_PRIMARY : ColorToken.TEXT_MUTED), false);
        }
        if (!slots.note().isEmpty()) {
            String note = expanded ? I18n.get(KEY_PLUGINS_CLOSE)
                    : hovered ? I18n.get(KEY_PLUGINS_OPEN)
                    : plugin.groups().isEmpty() ? I18n.get(KEY_PLUGIN_NO_SETTINGS)
                    : I18n.get(KEY_PLUGIN_SETTINGS, plugin.groups().size());
            painter.smallText(slots.note().x(), slots.note().y(),
                    smallTrim(painter, font, note, slots.note().width()),
                    theme.color(hovered ? ColorToken.TEXT_SECONDARY : ColorToken.TEXT_FAINT));
        }
        if (!slots.state().isEmpty()) {
            String state = I18n.get(on ? KEY_PLUGINS_ON : KEY_PLUGINS_DISABLED);
            painter.smallText(slots.state().right() - smallWidth(painter, font, state),
                    slots.state().y(), state,
                    theme.color(on ? ColorToken.SUCCESS : ColorToken.TEXT_FAINT));
        }
        rowRenderer.paintToggle(painter, slots.toggle(), on, hoverKey, deltaMs);

        if (PluginSettings.routeOf(plugin.id()).toString().equals(focusedId)) {
            paintRoundedOutline(painter, row, SettingRowLayout.CARD_RADIUS,
                    theme.color(ColorToken.ACCENT));
        }
    }

    private void paintPluginNote(SurfacePainter painter, Font font, Rect row, String text) {
        Rect label = PluginPageLayout.modLabel(row);
        if (label.isEmpty()) {
            return;
        }
        painter.text(label.x(), label.y(), trimToWidth(font, text, label.width()),
                theme.color(ColorToken.TEXT_SECONDARY), false);
    }

    private void paintPluginEmptyCard(SurfacePainter painter, Font font, Rect card) {
        paintRoundedFill(painter, card, SettingRowLayout.CARD_RADIUS,
                theme.color(ColorToken.SURFACE_SUNKEN));
        paintRoundedOutline(painter, card, SettingRowLayout.CARD_RADIUS,
                theme.color(ColorToken.BORDER_SUBTLE));
        Rect title = PluginPageLayout.emptyTitle(card);
        if (!title.isEmpty()) {
            painter.text(title.x(), title.y(),
                    trimToWidth(font, I18n.get(KEY_PLUGINS_EMPTY), title.width()),
                    theme.color(ColorToken.TEXT_DEFAULT), false);
        }
        paintSmallLines(painter, font, PluginPageLayout.emptyBody(card),
                I18n.get(KEY_PLUGINS_EMPTY_HINT), ColorToken.TEXT_FAINT);
    }

    private void paintCentredNotice(SurfacePainter painter, Font font, Rect content, String text) {
        if (content.isEmpty()) {
            return;
        }
        painter.text(content.x() + (content.width() - font.width(text)) / 2,
                content.y() + (content.height() - TEXT_HEIGHT) / 2, text,
                theme.color(ColorToken.TEXT_MUTED), false);
    }

    public record Crumbs(List<Rect> boxes, List<String> labels) {
    }

    public Crumbs crumbs(Font font, float smallScale, ShellLayout layout, NavPresenter presenter) {
        requireInputs(font, layout, presenter);
        Rect box = headerBand(layout, presenter).crumbs();
        List<RouteId> trail = presenter.stack().trail();
        if (box.isEmpty() || trail.size() < 2) {
            return new Crumbs(List.of(), List.of());
        }

        List<String> labels = new ArrayList<>();
        int[] widths = new int[trail.size() - 1];
        int room = box.width() - Math.round(font.width(BREADCRUMB_SEPARATOR) * smallScale);
        for (int i = 0; i < trail.size() - 1; i++) {
            String label = trimToWidth(font, label(presenter, trail.get(i)),
                    Math.max(0, Math.round(room / smallScale)));
            labels.add(label);
            widths[i] = Math.round(font.width(label) * smallScale);
            room -= widths[i] + BreadcrumbModel.SEPARATOR_ADVANCE;
        }
        return new Crumbs(BreadcrumbModel.layout(widths, box.x(), box.y()), List.copyOf(labels));
    }

    private void paintBand(SurfacePainter painter, Font font, ShellLayout layout, NavPresenter presenter,
                           long deltaMs) {
        PageHeader.Band band = headerBand(layout, presenter);
        if (band.bounds().isEmpty()) {
            return;
        }
        painter.fill(band.bounds(), theme.color(ColorToken.SURFACE_BASE));
        painter.gradient(band.bar(), theme.color(ColorToken.ACCENT_BRIGHT),
                theme.color(ColorToken.ACCENT_DEEP));

        paintCrumbs(painter, font, layout, presenter, band);
        if (!band.title().isEmpty()) {
            painter.text(band.title().x(), band.title().y(),
                    trimToWidth(font, I18n.get(presenter.currentTitleKey()), band.title().width()),
                    theme.color(ColorToken.TEXT_PRIMARY), false);
        }
        String subtitle = subtitleKey(presenter);
        if (subtitle != null) {
            paintSmallLines(painter, font, band.subtitle(), I18n.get(subtitle), ColorToken.TEXT_MUTED);
        }
        paintTabStrip(painter, font, layout, presenter, deltaMs);
        painter.fill(band.rule(), theme.color(ColorToken.BORDER_SUBTLE));
    }

    private void paintCrumbs(SurfacePainter painter, Font font, ShellLayout layout,
                             NavPresenter presenter, PageHeader.Band band) {
        Crumbs crumbs = crumbs(font, painter.smallScale(), layout, presenter);
        if (crumbs.boxes().isEmpty()) {
            return;
        }
        int faint = theme.color(ColorToken.TEXT_FAINT);
        int separator = Math.round(font.width(BREADCRUMB_SEPARATOR) * painter.smallScale());
        int last = crumbs.boxes().size() - 1;
        for (int i = 0; i <= last && i < crumbs.labels().size(); i++) {
            Rect box = crumbs.boxes().get(i);
            painter.smallText(box.x(), box.y() + PageHeader.CRUMB_TEXT_DY, crumbs.labels().get(i),
                    theme.color(i == last ? ColorToken.TEXT_MUTED : ColorToken.TEXT_FAINT));
            int gap = BreadcrumbModel.SEPARATOR_ADVANCE;
            painter.smallText(box.right() + (gap - separator) / 2, box.y() + PageHeader.CRUMB_TEXT_DY,
                    BREADCRUMB_SEPARATOR, faint);
        }
    }

    private void paintTabMarker(SurfacePainter painter, List<Rect> boxes, List<NavNode> tabs,
                                RouteId current, long deltaMs) {
        Rect target = null;
        for (int i = 0; i < boxes.size() && i < tabs.size(); i++) {
            if (tabs.get(i).route().equals(current)) {
                target = boxes.get(i);
                break;
            }
        }
        if (target == null) {
            this.tabMarkerPlaced = false;
            return;
        }
        int origin = boxes.get(0).x();
        if (tabMarkerPlaced && origin != tabStripOrigin) {
            tabMarkerX.jumpTo(tabMarkerX.value() + origin - tabStripOrigin);
        }
        this.tabStripOrigin = origin;
        if (!tabMarkerPlaced) {
            this.tabMarkerPlaced = true;
            tabMarkerX.jumpTo(target.x());
            tabMarkerWidth.jumpTo(target.width());
            this.tabLand = 0L;
        }
        boolean travelling = !(tabMarkerX.settled(target.x()) && tabMarkerWidth.settled(target.width()));
        if (travelling) {
            this.tabDir = Float.compare(target.x(), tabMarkerX.value()) >= 0 ? 1 : -1;
        }
        int drawnX = Math.round(tabMarkerX.advance(target.x(), deltaMs));
        int drawnW = Math.round(tabMarkerWidth.advance(target.width(), deltaMs));
        this.tabLand = Math.max(0L, tabLand - deltaMs);
        if (travelling && tabMarkerX.settled(target.x()) && tabMarkerWidth.settled(target.width())) {
            this.tabLand = MARKER_LAND_MS;
        }
        float landT = 1.0f - tabLand / (float) MARKER_LAND_MS;
        int bounce = tabLand > 0L
                ? Math.round(tabDir * MARKER_OVERSHOOT
                        * (float) Math.sin(Math.PI * landT) * (1.0f - landT)) : 0;
        Rect box = new Rect(drawnX + bounce, target.y(), drawnW, target.height());
        this.tabPillNow = box;
        paintRoundedGradient(painter, box, PILL_RADIUS,
                theme.color(ColorToken.ACCENT_BRIGHT), theme.color(ColorToken.ACCENT_DEEP));
        if (tabLand > 0L) {
            paintRoundedFill(painter, box, PILL_RADIUS,
                    Motion.fade(theme.color(ColorToken.ACCENT_BRIGHT), 0.35f * (1.0f - landT)));
        }
    }

    private void paintTabStrip(SurfacePainter painter, Font font, ShellLayout layout, NavPresenter presenter,
                               long deltaMs) {
        List<NavNode> tabs = presenter.subTabs();
        if (tabs.isEmpty()) {
            return;
        }

        List<Rect> boxes = tabStripBoxes(font, layout, presenter);
        RouteId current = presenter.stack().current();
        String focusedId = focusedIn(presenter, NavPresenter.REGION_CONTENT);
        int gradientTop = theme.color(ColorToken.ACCENT_BRIGHT);
        int gradientBottom = theme.color(ColorToken.ACCENT_DEEP);
        paintTabMarker(painter, boxes, tabs, current, deltaMs);

        for (int i = 0; i < boxes.size(); i++) {
            Rect box = boxes.get(i);
            NavNode tab = tabs.get(i);

            if (tab.route().toString().equals(focusedId) && !tab.route().equals(current)) {
                paintRoundedOutline(painter, box, PILL_RADIUS, theme.color(ColorToken.ACCENT));
            }

            float covered = 0.0f;
            if (!tabPillNow.isEmpty() && box.width() > 0) {
                int overlap = Math.min(tabPillNow.right(), box.right())
                        - Math.max(tabPillNow.x(), box.x());
                covered = Math.max(0.0f, Math.min(1.0f, overlap / (float) box.width()));
            }
            String text = I18n.get(tab.titleKey());
            painter.text(box.x() + (box.width() - font.width(text)) / 2, box.y() + (box.height() - TEXT_HEIGHT) / 2,
                    text, Motion.blend(theme.color(ColorToken.TEXT_SECONDARY),
                            theme.color(ColorToken.TEXT_PRIMARY), covered), false);
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
