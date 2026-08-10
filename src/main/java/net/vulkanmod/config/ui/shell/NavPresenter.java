package net.vulkanmod.config.ui.shell;

import net.neoforged.fml.ModList;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.PerformancePreset;
import net.vulkanmod.config.ui.core.DeferredValues;
import net.vulkanmod.config.ui.core.Favorites;
import net.vulkanmod.config.ui.core.FocusHandoff;
import net.vulkanmod.config.ui.core.FocusModel;
import net.vulkanmod.config.ui.core.FocusRing;
import net.vulkanmod.config.ui.core.NavNode;
import net.vulkanmod.config.ui.core.NavStack;
import net.vulkanmod.config.ui.core.NavTree;
import net.vulkanmod.config.ui.core.PendingChanges;
import net.vulkanmod.config.ui.core.PresetCardModel;
import net.vulkanmod.config.ui.core.ProfileChipRow;
import net.vulkanmod.config.ui.core.ProfileMatcher;
import net.vulkanmod.config.ui.core.FrameSamples;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingType;
import net.vulkanmod.config.ui.core.SidebarModel;
import net.vulkanmod.config.ui.mods.ModScreens;
import net.vulkanmod.config.ui.mods.ModSettings;
import net.vulkanmod.config.ui.settings.SettingBinding;
import net.vulkanmod.config.ui.settings.PluginSettings;
import net.vulkanmod.config.ui.settings.SettingsCatalog;
import net.vulkanmod.config.ui.settings.UiState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class NavPresenter {
    public static final String REGION_SIDEBAR = "sidebar";
    public static final String REGION_CONTENT = "content";
    public static final String MOD_SCREEN_FOCUS = "mods:screen";
    public static final String FAVORITES_FOCUS = "favorites:button";
    public static final RouteId FAVORITES = RouteId.parse("favorites");

    private final NavTree tree;
    private final NavStack stack;
    private SidebarModel sidebar;
    private final java.util.Set<String> collapsed = new java.util.LinkedHashSet<>();
    private boolean collapsedLoaded;
    private final java.util.Set<RouteId> greyedRows = new java.util.LinkedHashSet<>();
    private final FocusModel focus;
    private final SettingsCatalog catalog = new SettingsCatalog();
    private final PendingChanges pending = new PendingChanges();
    private final DeferredValues deferred = new DeferredValues();
    private final List<String> screenOnlyModIds;
    private List<PluginPage> pluginPages;
    private List<ProfileChipRow.Chip> profileChips;
    private List<PresetCardModel.Card> presetCards;
    private Favorites favorites;

    public NavPresenter() {
        List<String> modIds = catalog.modIds();
        this.screenOnlyModIds = screenOnlyModIds(modIds);
        this.pluginPages = discoverPluginPages();
        this.tree = buildTree(modIds, screenOnlyModIds, pluginPages);
        markGreyedPlugins();
        this.stack = new NavStack(tree, destinationOf(tree, tree.defaultRoute()));
        this.sidebar = new SidebarModel(tree, collapsed);
        this.focus = new FocusModel();
        this.focus.addRegion(REGION_SIDEBAR);
        this.focus.addRegion(REGION_CONTENT);
        FocusRing sidebarRing = this.focus.ring(REGION_SIDEBAR);
        for (NavNode row : tree.sidebarRows()) {
            sidebarRing.register(row.route().toString(), true);
        }
        rebuildContentRing();
    }

    public NavTree tree() {
        return tree;
    }

    public NavStack stack() {
        return stack;
    }

    public SidebarModel sidebar() {
        ensureCollapsedLoaded();
        return sidebar;
    }

    private void ensureCollapsedLoaded() {
        if (collapsedLoaded) {
            return;
        }
        this.collapsedLoaded = true;
        collapsed.addAll(SidebarModel.collapsedOrDefault(UiState.load(UiState.PATH).collapsed()));
        rebuildSidebar();
    }

    private void rebuildSidebar() {
        this.sidebar = new SidebarModel(tree, collapsed);
        FocusRing ring = focus.ring(REGION_SIDEBAR);
        for (NavNode row : tree.sidebarRows()) {
            ring.setEnabled(row.route().toString(), visibleInSidebar(row));
        }
    }

    public List<PluginPage> pluginPages() {
        return pluginPages;
    }

    public void togglePlugin(String pluginId) {
        net.vulkanmod.api.MenuPlugin plugin =
                net.vulkanmod.config.ui.settings.MenuPlugins.byId(pluginId);
        if (plugin == null
                || !net.vulkanmod.config.ui.settings.MenuPlugins.toggleableOf(plugin)) {
            return;
        }
        net.vulkanmod.config.ui.settings.MenuPlugins.setEnabled(plugin,
                !net.vulkanmod.config.ui.settings.MenuPlugins.enabledOf(plugin));
        this.pluginPages = discoverPluginPages();
        markGreyedPlugins();
    }

    private void markGreyedPlugins() {
        for (PluginPage page : pluginPages) {
            RouteId route = PluginSettings.routeOf(page.id());
            if (page.enabled()) {
                greyedRows.remove(route);
            } else {
                greyedRows.add(route);
            }
        }
    }

    private static List<PluginPage> discoverPluginPages() {
        List<PluginPage> pages = new ArrayList<>();
        for (net.vulkanmod.api.MenuPlugin plugin
                : net.vulkanmod.config.ui.settings.MenuPlugins.discover()) {
            pages.add(new PluginPage(plugin.id(),
                    net.vulkanmod.config.ui.settings.MenuPlugins.displayNameOf(plugin),
                    net.vulkanmod.config.ui.settings.MenuPlugins.groupsOf(plugin),
                    net.vulkanmod.config.ui.settings.MenuPlugins.enabledOf(plugin),
                    net.vulkanmod.config.ui.settings.MenuPlugins.toggleableOf(plugin)));
        }
        return List.copyOf(pages);
    }

    public boolean rowGreyed(RouteId route) {
        return greyedRows.contains(route);
    }

    public void toggleSection(String sectionKey) {
        if (sectionKey == null || sectionKey.isBlank()) {
            throw new IllegalArgumentException("sectionKey must not be blank");
        }
        ensureCollapsedLoaded();
        if (!collapsed.remove(sectionKey)) {
            collapsed.add(sectionKey);
        }
        rebuildSidebar();
        UiState.save(UiState.PATH, favorites(), collapsed);
    }

    private boolean visibleInSidebar(NavNode row) {
        String section = null;
        for (NavNode node : tree.sidebarRows()) {
            if (node.sectionKey() != null) {
                section = node.sectionKey();
            }
            if (node.route().equals(row.route())) {
                return section == null || !collapsed.contains(section);
            }
        }
        return true;
    }

    public FocusModel focus() {
        return focus;
    }

    public SettingsCatalog catalog() {
        return catalog;
    }

    public PendingChanges pending() {
        return pending;
    }

    public List<SettingMeta> settings() {
        if (FAVORITES_ROUTE.equals(stack.current())) {
            return favoriteSettings();
        }
        return catalog.registry().forRoute(stack.current());
    }

    public boolean isFavorite(SettingId id) {
        return favorites().contains(id);
    }

    public void toggleFavorite(SettingId id) {
        favorites().toggle(id);
        UiState.save(UiState.PATH, favorites(), collapsed);
        if (!FAVORITES_ROUTE.equals(stack.current())) {
            return;
        }
        String focused = focus.ring(REGION_CONTENT).focused();
        rebuildContentRing();
        if (focused != null) {
            focus.ring(REGION_CONTENT).focus(focused);
        }
    }

    private Favorites favorites() {
        if (favorites == null) {
            this.favorites = UiState.load(UiState.PATH).favorites();
        }
        return favorites;
    }

    private List<SettingMeta> favoriteSettings() {
        List<SettingMeta> resolved = new ArrayList<>();
        for (SettingId id : favorites().ids()) {
            if (catalog.registry().contains(id)) {
                resolved.add(catalog.registry().get(id));
            }
        }
        return List.copyOf(resolved);
    }

    public Object valueOf(SettingMeta meta) {
        if (meta == null) {
            throw new IllegalArgumentException("meta must not be null");
        }
        return deferred.valueOr(meta.id(), catalog.binding(meta.id()).get());
    }

    public boolean activate(SettingMeta meta) {
        if (meta == null) {
            throw new IllegalArgumentException("meta must not be null");
        }
        if (!catalog.enabled(meta.id())) {
            return false;
        }

        return switch (meta.type()) {
            case BOOL -> set(meta, !boolValue(meta, valueOf(meta)));
            case ENUM -> step(meta, 1);
            case INT -> false;
        };
    }

    public boolean step(SettingMeta meta, int direction) {
        if (meta == null) {
            throw new IllegalArgumentException("meta must not be null");
        }
        if (direction == 0 || !catalog.enabled(meta.id())) {
            return false;
        }
        if (meta.type() == SettingType.ENUM) {
            List<String> choices = catalog.binding(meta.id()).choices();
            return !choices.isEmpty() && set(meta, stepped(choices, valueOf(meta), direction));
        }
        if (!meta.type().slider()) {
            return false;
        }

        SettingBinding binding = catalog.binding(meta.id());
        int current = intValue(meta, valueOf(meta));
        return set(meta, Math.max(binding.min(),
                Math.min(binding.max(), current + Integer.signum(direction) * binding.step())));
    }

    public boolean set(SettingMeta meta, Object value) {
        if (meta == null) {
            throw new IllegalArgumentException("meta must not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value must not be null for setting " + meta.id());
        }
        if (!catalog.enabled(meta.id()) || value.equals(valueOf(meta))) {
            return false;
        }

        this.profileChips = null;
        this.presetCards = null;
        SettingBinding binding = catalog.binding(meta.id());
        if (deferred.set(meta.id(), value, binding.get())) {
            pending.mark(meta.id(), meta.scope());
        } else {
            pending.unmark(meta.id());
        }
        return true;
    }

    private static final RouteId OVERVIEW_ROUTE = RouteId.parse("overview");
    private static final RouteId FAVORITES_ROUTE = RouteId.parse("favorites");
    private static final RouteId MODS_ROUTE = RouteId.parse("mods");

    public Optional<String> modScreen() {
        return modScreenOf(stack.current(), screenOnlyModIds);
    }

    public boolean favoritesFocused() {
        return REGION_CONTENT.equals(focus.activeRegion()) && FAVORITES_FOCUS.equals(focus.focused());
    }

    public boolean openFavorites() {
        boolean moved = navigate(FAVORITES);
        FocusHandoff.enter(focus, REGION_CONTENT, FAVORITES_FOCUS);
        return moved;
    }

    public boolean modScreenFocused() {
        return REGION_CONTENT.equals(focus.activeRegion()) && MOD_SCREEN_FOCUS.equals(focus.focused());
    }

    static Optional<String> modScreenOf(RouteId route, List<String> screenOnlyModIds) {
        if (route == null) {
            throw new IllegalArgumentException("route must not be null");
        }
        if (screenOnlyModIds == null) {
            throw new IllegalArgumentException("screenOnlyModIds must not be null");
        }
        if (route.depth() != 2 || !MODS_ROUTE.equals(route.parent())) {
            return Optional.empty();
        }
        String modId = route.segments().get(route.depth() - 1);
        return screenOnlyModIds.contains(modId) ? Optional.of(modId) : Optional.empty();
    }

    private static List<String> screenOnlyModIds(List<String> modIds) {
        try {
            return ModScreens.without(modIds);
        } catch (Throwable t) {
            return List.of();
        }
    }

    public static final RouteId DEVELOPER_INFO = RouteId.parse("developer.info");
    public static final RouteId DEVELOPER_STATS = RouteId.parse("developer.stats");

    private List<net.vulkanmod.config.ui.settings.SystemReport.Row> infoRows = List.of();
    private FrameSamples.Summary playSummary = new FrameSamples().summary();
    private List<net.vulkanmod.config.ui.core.FrameHistory.Bucket> columns = List.of();
    private List<net.vulkanmod.config.ui.core.FrameHistory.Bucket> stutters = List.of();
    private List<net.vulkanmod.render.profiling.StackSampler.Frame> stutterProfile = List.of();
    private int stutterSamples;
    private List<net.vulkanmod.render.profiling.StackSampler.Frame> allocators = List.of();
    private int allocatorSamples;
    private float historyPeak;
    private float historyMedian = -1.0f;
    private long captureEndMs;
    private int selectedStutter;
    private List<net.vulkanmod.config.ui.settings.StatsReport.Cell> fingerprint = List.of();
    private List<net.vulkanmod.config.ui.settings.StatsReport.Cell> sceneRows = List.of();
    private List<net.vulkanmod.config.ui.settings.StatsReport.Cell> terrainRows = List.of();
    private List<net.vulkanmod.config.ui.settings.StatsReport.Cell> memoryRows = List.of();
    private List<net.vulkanmod.config.ui.settings.StatsReport.Cell> machineRows = List.of();
    private List<net.vulkanmod.config.ui.settings.Diagnosis.Finding> findings = List.of();

    private static final RouteId DEVELOPER = RouteId.parse("developer");

    public boolean isDeveloper() {
        RouteId current = stack.current();
        return DEVELOPER.equals(current) || DEVELOPER.isAncestorOf(current);
    }

    public boolean isDeveloperInfo() {
        return DEVELOPER_INFO.equals(stack.current());
    }

    public boolean isDeveloperStats() {
        return DEVELOPER_STATS.equals(stack.current());
    }

    public List<net.vulkanmod.config.ui.settings.SystemReport.Row> infoRows() {
        return infoRows;
    }

    public List<net.vulkanmod.config.ui.settings.StatsReport.Cell> fingerprint() {
        return fingerprint;
    }

    public List<net.vulkanmod.config.ui.settings.Diagnosis.Finding> findings() {
        return findings;
    }

    public List<net.vulkanmod.config.ui.settings.StatsReport.Cell> sceneRows() {
        return sceneRows;
    }

    public List<net.vulkanmod.config.ui.settings.StatsReport.Cell> terrainRows() {
        return terrainRows;
    }

    public List<net.vulkanmod.config.ui.settings.StatsReport.Cell> memoryRows() {
        return memoryRows;
    }

    public List<net.vulkanmod.config.ui.settings.StatsReport.Cell> machineRows() {
        return machineRows;
    }

    public List<net.vulkanmod.config.ui.core.FrameHistory.Bucket> columns() {
        return columns;
    }

    public List<net.vulkanmod.config.ui.core.FrameHistory.Bucket> stutters() {
        return stutters;
    }

    public List<net.vulkanmod.render.profiling.StackSampler.Frame> stutterProfile() {
        return stutterProfile;
    }

    public int stutterSamples() {
        return stutterSamples;
    }

    public List<net.vulkanmod.render.profiling.StackSampler.Frame> allocators() {
        return allocators;
    }

    public int allocatorSamples() {
        return allocatorSamples;
    }

    public float historyPeak() {
        return historyPeak;
    }

    public float historyMedian() {
        return historyMedian;
    }

    public long captureEndMs() {
        return captureEndMs;
    }

    public int selectedStutter() {
        return selectedStutter;
    }

    public void selectStutter(int index) {
        this.selectedStutter = index < 0 || index >= stutters.size() ? 0 : index;
        refreshStutterProfile();
    }

    private boolean axisInFps = true;

    public boolean axisInFps() {
        return axisInFps;
    }

    public void toggleAxisUnit() {
        this.axisInFps = !axisInFps;
    }

    public void rebuildWorld() {
        try {
            net.vulkanmod.render.chunk.WorldRenderer.getInstance().allChanged();
        } catch (Throwable unavailable) {
            net.vulkanmod.Initializer.LOGGER.warn("Could not rebuild the world from the menu");
        }
    }

    public void resetHistory() {
        net.vulkanmod.vulkan.SessionSamples.history().clear();
        net.vulkanmod.render.profiling.StackSampler.clear();
        this.columns = List.of();
        this.stutters = List.of();
        this.stutterProfile = List.of();
        this.stutterSamples = 0;
        this.historyPeak = 0.0f;
        this.historyMedian = -1.0f;
    }

    public String report() {
        return net.vulkanmod.config.ui.settings.DiagnosticsReport.build(
                new net.vulkanmod.config.ui.settings.DiagnosticsReport.Input(
                        fingerprint, sceneRows, terrainRows, memoryRows, machineRows,
                        net.vulkanmod.config.ui.settings.SystemReport.rows(), findings, stutters,
                        allocators, playSummary, net.vulkanmod.vulkan.SessionSamples.history(),
                        captureEndMs,
                        net.vulkanmod.config.ui.settings.OverviewSignals.stickyVerdict().messageKey()));
    }

    public FrameSamples.Summary playSummary() {
        return playSummary;
    }

    public void refreshDiagnostics(int wantedColumns) {
        if (isDeveloperInfo()) {
            this.infoRows = net.vulkanmod.config.ui.settings.SystemReport.rows();
            return;
        }
        if (isDeveloperStats()) {
            net.vulkanmod.render.profiling.StackSampler.setRunning(true);
            net.vulkanmod.render.profiling.Telemetry.setUnifiedDie(
                    net.vulkanmod.config.ui.settings.StatsReport.unifiedDie());
            net.vulkanmod.render.profiling.Telemetry.setRunning(true);
            net.vulkanmod.config.ui.core.FrameHistory history =
                    net.vulkanmod.vulkan.SessionSamples.history();
            this.fingerprint = net.vulkanmod.config.ui.settings.StatsReport.fingerprint();
            this.sceneRows = net.vulkanmod.config.ui.settings.StatsReport.scene();
            this.terrainRows = net.vulkanmod.config.ui.settings.StatsReport.terrain();
            this.memoryRows = net.vulkanmod.config.ui.settings.StatsReport.memory(
                    history.allocationRateMbPerSecond());
            this.machineRows = net.vulkanmod.config.ui.settings.StatsReport.machine();
            this.columns = history.columns(Math.max(1, wantedColumns));
            this.historyPeak = history.peak();
            this.historyMedian = history.medianAverage();
            this.stutters = history.worst(5, FrameSamples.spikeFloor(historyMedian));
            this.captureEndMs = history.size() == 0 ? System.currentTimeMillis()
                    : history.at(history.size() - 1).startMs() + net.vulkanmod.config.ui.core.FrameHistory.BUCKET_MS;
            if (selectedStutter >= stutters.size()) {
                this.selectedStutter = 0;
            }
            this.playSummary = net.vulkanmod.vulkan.SessionSamples.samples().summary();
            refreshStutterProfile();
            refreshAllocators(history);
            this.findings = net.vulkanmod.config.ui.settings.Diagnosis.of(history, playSummary,
                    stutters, terrainRows, memoryRows, machineRows);
        }
    }

    private void refreshAllocators(net.vulkanmod.config.ui.core.FrameHistory history) {
        net.vulkanmod.config.ui.core.FrameHistory.Bucket busiest = history.busiestAllocator();
        List<net.vulkanmod.render.profiling.StackSampler.Frame> hot = List.of();
        if (busiest != null) {
            hot = net.vulkanmod.render.profiling.StackSampler.profile(busiest.startMs(),
                    busiest.startMs() + net.vulkanmod.config.ui.core.FrameHistory.BUCKET_MS);
        }
        if (hot.isEmpty() && captureEndMs > 0L) {
            hot = net.vulkanmod.render.profiling.StackSampler.profile(captureEndMs - 30_000L,
                    captureEndMs);
        }
        this.allocators = hot;
        this.allocatorSamples = net.vulkanmod.render.profiling.StackSampler.recorded();
    }

    private void refreshStutterProfile() {
        if (stutters.isEmpty()) {
            this.stutterProfile = List.of();
            this.stutterSamples = 0;
            return;
        }
        net.vulkanmod.config.ui.core.FrameHistory.Bucket worst =
                stutters.get(Math.max(0, Math.min(selectedStutter, stutters.size() - 1)));
        long span = Math.max(net.vulkanmod.config.ui.core.FrameHistory.BUCKET_MS,
                Math.round(worst.max()));
        long from = worst.startMs() - span;
        long to = worst.startMs() + span;
        this.stutterProfile = net.vulkanmod.render.profiling.StackSampler.profile(from, to);
        this.stutterSamples = net.vulkanmod.render.profiling.StackSampler.samplesIn(from, to);
    }

    public boolean[] infoSections() {
        boolean[] sections = new boolean[infoRows.size()];
        for (int index = 0; index < sections.length; index++) {
            sections[index] = infoRows.get(index).section();
        }
        return sections;
    }

    public int contentRowCount() {
        if (isDeveloperInfo()) {
            return infoRows.size();
        }
        return isOverview() ? catalog.overview().rows().size() : contentRows().size();
    }

    public sealed interface ContentRow permits GroupRow, SettingRow {
    }

    public record GroupRow(String key, boolean collapsed, int count) implements ContentRow {
    }

    public record SettingRow(SettingMeta meta) implements ContentRow {
    }

    public List<ContentRow> contentRows() {
        List<ContentRow> rows = new ArrayList<>();
        List<SettingMeta> all = settings();
        for (int index = 0; index < all.size(); index++) {
            SettingMeta meta = all.get(index);
            String group = meta.groupKey();
            if (group == null) {
                rows.add(new SettingRow(meta));
                continue;
            }
            int end = index;
            while (end < all.size() && group.equals(all.get(end).groupKey())) {
                end++;
            }
            boolean folded = groupCollapsed(group);
            rows.add(new GroupRow(group, folded, end - index));
            if (!folded) {
                for (int member = index; member < end; member++) {
                    rows.add(new SettingRow(all.get(member)));
                }
            }
            index = end - 1;
        }
        return List.copyOf(rows);
    }

    private static final String EXPANDED = "expanded/";

    public boolean groupCollapsed(String groupKey) {
        ensureCollapsedLoaded();
        return !collapsed.contains(EXPANDED + groupKey);
    }

    public void toggleGroup(String groupKey) {
        if (groupKey == null || groupKey.isBlank()) {
            throw new IllegalArgumentException("groupKey must not be blank");
        }
        ensureCollapsedLoaded();
        String marker = EXPANDED + groupKey;
        if (!collapsed.remove(marker)) {
            collapsed.add(marker);
        }
        UiState.save(UiState.PATH, favorites(), collapsed);
    }

    public boolean isOverview() {
        return OVERVIEW_ROUTE.equals(stack().current());
    }

    private void applyPlugins() {
        for (net.vulkanmod.api.MenuPlugin plugin
                : net.vulkanmod.config.ui.settings.MenuPlugins.discover()) {
            net.vulkanmod.config.ui.settings.MenuPlugins.applyTo(plugin);
        }
    }

    public void apply() {
        this.profileChips = null;
        this.presetCards = null;
        boolean[] touchedPlugin = {false};
        deferred.drainTo((id, value) -> {
            catalog.binding(id).set(value);
            pending.unmark(id);
            touchedPlugin[0] |= catalog.pluginIds().contains(id.namespace());
        });
        if (touchedPlugin[0]) {
            applyPlugins();
        }
        Initializer.CONFIG.write();
    }

    public void discard() {
        this.profileChips = null;
        this.presetCards = null;
        deferred.clear();
        pending.clear();
    }

    public List<ProfileChipRow.Chip> profileChips() {
        if (profileChips == null) {
            Map<String, Map<SettingId, Object>> profiles = changeableProfiles();
            this.profileChips = ProfileChipRow.chips(List.copyOf(profiles.keySet()),
                    PerformancePreset.CUSTOM.translationKey,
                    ProfileMatcher.match(profiles, changeableValues()));
        }
        return profileChips;
    }

    public List<PresetCardModel.Card> presetCards() {
        net.vulkanmod.config.ui.settings.OverviewSignals.harvest(playingProfileKey());
        if (presetCards == null) {
            this.presetCards = PresetCardModel.cards(changeableProfiles(), committedValues(), changeableValues(),
                    PerformancePreset.CUSTOM.translationKey,
                    net.vulkanmod.config.ui.settings.OverviewSignals.suggestedPresetKey(playingProfileKey()));
        }
        return presetCards;
    }

    public boolean applyProfile(String profileKey) {
        if (profileKey == null) {
            throw new IllegalArgumentException("profileKey must not be null");
        }
        Map<SettingId, Object> values = catalog.profileValues().get(profileKey);
        if (values == null) {
            return false;
        }
        boolean changed = false;
        for (Map.Entry<SettingId, Object> entry : values.entrySet()) {
            changed |= set(catalog.registry().get(entry.getKey()), entry.getValue());
        }
        return changed;
    }

    private Map<String, Map<SettingId, Object>> changeableProfiles() {
        Map<String, Map<SettingId, Object>> profiles = new LinkedHashMap<>();
        catalog.profileValues().forEach((key, values) -> {
            Map<SettingId, Object> governed = new LinkedHashMap<>();
            values.forEach((id, value) -> {
                if (catalog.enabled(id)) {
                    governed.put(id, value);
                }
            });
            profiles.put(key, governed);
        });
        return profiles;
    }

    public String playingProfileKey() {
        Map<SettingId, Object> live = committedValues();
        for (Map.Entry<String, Map<SettingId, Object>> profile : changeableProfiles().entrySet()) {
            if (PresetCardModel.differing(profile.getValue(), live) == 0) {
                return profile.getKey();
            }
        }
        return null;
    }

    private Map<SettingId, Object> committedValues() {
        Map<SettingId, Object> values = new LinkedHashMap<>();
        catalog.currentProfileValues().forEach((id, value) -> {
            if (catalog.enabled(id)) {
                values.put(id, value);
            }
        });
        return values;
    }

    private Map<SettingId, Object> changeableValues() {
        Map<SettingId, Object> values = new LinkedHashMap<>();
        catalog.currentProfileValues().forEach((id, value) -> {
            if (catalog.enabled(id)) {
                values.put(id, deferred.valueOr(id, value));
            }
        });
        return values;
    }

    public boolean resettable(SettingMeta meta) {
        if (meta == null) {
            throw new IllegalArgumentException("meta must not be null");
        }
        if (!catalog.enabled(meta.id())) {
            return false;
        }

        SettingBinding binding = catalog.binding(meta.id());
        return binding.hasDefault() && !binding.defaultValue().equals(valueOf(meta));
    }

    public boolean reset(SettingMeta meta) {
        if (!resettable(meta)) {
            return false;
        }
        return set(meta, catalog.binding(meta.id()).defaultValue());
    }

    public boolean navigate(RouteId route) {
        if (route == null) {
            throw new IllegalArgumentException("route must not be null");
        }
        boolean moved = stack.navigate(destinationOf(tree, route));
        if (moved) {
            rebuildContentRing();
        }
        return moved;
    }

    public boolean reveal(SettingId id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        navigate(catalog.registry().get(id).route());
        return FocusHandoff.enter(focus, REGION_CONTENT, id.toString())
                && focus.ring(REGION_CONTENT).focus(id.toString());
    }

    public boolean back() {
        boolean moved = stack.back();
        if (moved) {
            rebuildContentRing();
        }
        return moved;
    }

    public String currentTitleKey() {
        return titleKeyOf(stack.current());
    }

    public String titleKeyOf(RouteId route) {
        if (route == null) {
            throw new IllegalArgumentException("route must not be null");
        }
        NavNode node = tree.find(route);
        if (node == null) {
            throw new IllegalArgumentException("route is not present in the tree: " + route);
        }
        return node.titleKey();
    }

    public RouteId activeSidebarRoute() {
        RouteId route = stack.current();
        while (route.depth() > 1 && !isSidebarRow(route)) {
            route = route.parent();
        }
        return route;
    }

    private boolean isSidebarRow(RouteId route) {
        NavNode node = tree.find(route);
        return node != null && node.sidebarVisible();
    }

    public List<NavNode> subTabs() {
        return tree.children(activeSidebarRoute());
    }

    public RouteId focusedRoute() {
        String focusedId = focus.focused();
        if (focusedId == null) {
            return null;
        }
        for (NavNode node : activatableNodes(focus.activeRegion())) {
            if (node.route().toString().equals(focusedId)) {
                return node.route();
            }
        }
        return null;
    }

    public SettingMeta focusedSetting() {
        if (!REGION_CONTENT.equals(focus.activeRegion())) {
            return null;
        }
        String focusedId = focus.focused();
        if (focusedId == null) {
            return null;
        }
        for (SettingMeta meta : settings()) {
            if (meta.id().toString().equals(focusedId)) {
                return meta;
            }
        }
        return null;
    }

    private List<NavNode> activatableNodes(String regionId) {
        if (REGION_SIDEBAR.equals(regionId)) {
            return tree.sidebarRows();
        }
        if (REGION_CONTENT.equals(regionId)) {
            return subTabs();
        }
        return List.of();
    }

    static String cycled(List<String> choices, Object current) {
        return stepped(choices, current, 1);
    }

    static String stepped(List<String> choices, Object current, int direction) {
        if (choices == null || choices.isEmpty()) {
            throw new IllegalArgumentException("choices must not be empty");
        }
        int size = choices.size();
        int next = (choices.indexOf(current) + Integer.signum(direction)) % size;
        return choices.get(next < 0 ? next + size : next);
    }

    private static int intValue(SettingMeta meta, Object value) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("setting " + meta.id() + " is INT but its value is " + value);
        }
        return number.intValue();
    }

    private static boolean boolValue(SettingMeta meta, Object value) {
        if (!(value instanceof Boolean flag)) {
            throw new IllegalArgumentException("setting " + meta.id() + " is BOOL but its value is " + value);
        }
        return flag;
    }

    private static RouteId destinationOf(NavTree tree, RouteId route) {
        RouteId destination = route;
        List<NavNode> children = tree.children(destination);
        while (!children.isEmpty() && !children.get(0).sidebarVisible()) {
            destination = children.get(0).route();
            children = tree.children(destination);
        }
        return destination;
    }

    private void rebuildContentRing() {
        FocusRing ring = focus.ring(REGION_CONTENT);
        ring.clear();
        for (NavNode child : subTabs()) {
            ring.register(child.route().toString(), true);
        }
        for (SettingMeta meta : settings()) {
            ring.register(meta.id().toString(), true);
        }
        if (modScreen().isPresent()) {
            ring.register(MOD_SCREEN_FOCUS, true);
        }
        ring.register(FAVORITES_FOCUS, true);
    }

    public record PluginPage(String id, String name, List<String> groups, boolean enabled,
                             boolean toggleable) {
    }

    static NavTree buildTree(List<String> modIds, List<String> screenOnlyModIds) {
        return buildTree(modIds, screenOnlyModIds, List.of());
    }

    static NavTree buildTree(List<String> modIds, List<String> screenOnlyModIds,
                             List<PluginPage> pluginPages) {
        if (modIds == null || screenOnlyModIds == null || pluginPages == null) {
            throw new IllegalArgumentException("modIds and screenOnlyModIds must not be null");
        }
        NavTree.Builder builder = new NavTree.Builder()
                .add(new NavNode(RouteId.parse("overview"), "vulkanmod.ui.page.overview", "vulkanmod.ui.section.volcanic", true))
                .add(new NavNode(RouteId.parse("display"), "vulkanmod.ui.page.display", null, true))
                .add(new NavNode(RouteId.parse("display.general"), "vulkanmod.ui.page.display.general", null, false))
                .add(new NavNode(RouteId.parse("display.interface"), "vulkanmod.ui.page.display.interface", null, false))
                .add(new NavNode(RouteId.parse("display.advanced"), "vulkanmod.ui.page.display.advanced", null, false))
                .add(new NavNode(RouteId.parse("display.volcanic"), "vulkanmod.ui.page.display.volcanic", null, false))
                .add(new NavNode(RouteId.parse("rendering"), "vulkanmod.ui.page.rendering", null, true))
                .add(new NavNode(RouteId.parse("rendering.general"), "vulkanmod.ui.page.rendering.general", null, false))
                .add(new NavNode(RouteId.parse("rendering.resolution"), "vulkanmod.ui.page.rendering.resolution", null, false))
                .add(new NavNode(RouteId.parse("rendering.culling"), "vulkanmod.ui.page.rendering.culling", null, false))
                .add(new NavNode(RouteId.parse("rendering.entities"), "vulkanmod.ui.page.rendering.entities", null, false))
                .add(new NavNode(RouteId.parse("rendering.advanced"), "vulkanmod.ui.page.rendering.advanced", null, false))
                .add(new NavNode(RouteId.parse("performance"), "vulkanmod.ui.page.performance", null, true))
                .add(new NavNode(RouteId.parse("performance.general"), "vulkanmod.ui.page.performance.general", null, false))
                .add(new NavNode(RouteId.parse("performance.gpu"), "vulkanmod.ui.page.performance.gpu", null, false))
                .add(new NavNode(RouteId.parse("performance.chunks"), "vulkanmod.ui.page.performance.chunks", null, false))
                .add(new NavNode(RouteId.parse("performance.synchronization"), "vulkanmod.ui.page.performance.synchronization", null, false))
                .add(new NavNode(RouteId.parse("quality"), "vulkanmod.ui.page.quality", null, true))
                .add(new NavNode(RouteId.parse("quality.general"), "vulkanmod.ui.page.quality.general", null, false))
                .add(new NavNode(RouteId.parse("quality.textures"), "vulkanmod.ui.page.quality.textures", null, false))
                .add(new NavNode(RouteId.parse("quality.lighting"), "vulkanmod.ui.page.quality.lighting", null, false))
                .add(new NavNode(RouteId.parse("quality.environment"), "vulkanmod.ui.page.quality.environment", null, false))
                .add(new NavNode(RouteId.parse("shaders"), "vulkanmod.ui.page.shaders", null, true))
                .add(new NavNode(RouteId.parse("shaders.current"), "vulkanmod.ui.page.shaders.current", null, false))
                .add(new NavNode(RouteId.parse("shaders.packs"), "vulkanmod.ui.page.shaders.packs", null, false))
                .add(new NavNode(RouteId.parse("shaders.profiles"), "vulkanmod.ui.page.shaders.profiles", null, false))
                .add(new NavNode(RouteId.parse("shaders.settings"), "vulkanmod.ui.page.shaders.settings", null, false))
                .add(new NavNode(RouteId.parse("mods"), "vulkanmod.ui.page.mods", "vulkanmod.ui.section.content", true))
                .add(new NavNode(RouteId.parse("favorites"), "vulkanmod.ui.page.favorites", null, false))
                .add(new NavNode(RouteId.parse("plugins"), "vulkanmod.ui.page.plugins", null, true));

        for (PluginPage plugin : pluginPages) {
            builder.add(new NavNode(PluginSettings.routeOf(plugin.id()), plugin.name(), null, true));
            for (String group : plugin.groups()) {
                builder.add(new NavNode(PluginSettings.routeOf(plugin.id(), group),
                        "vulkanmod.ui.plugin.group." + group, null, false));
            }
        }

        builder.add(new NavNode(RouteId.parse("advanced"), "vulkanmod.ui.page.advanced", "vulkanmod.ui.section.system", true))
                .add(new NavNode(RouteId.parse("advanced.renderer"), "vulkanmod.ui.page.advanced.renderer", null, false))
                .add(new NavNode(RouteId.parse("advanced.synchronization"), "vulkanmod.ui.page.advanced.synchronization", null, false))
                .add(new NavNode(RouteId.parse("advanced.compatibility"), "vulkanmod.ui.page.advanced.compatibility", null, false))
                .add(new NavNode(RouteId.parse("experimental"), "vulkanmod.ui.page.experimental", null, true))
                .add(new NavNode(RouteId.parse("developer"), "vulkanmod.ui.page.developer", null, true))
                .add(new NavNode(RouteId.parse("developer.tools"), "vulkanmod.ui.page.developer.tools", null, false))
                .add(new NavNode(RouteId.parse("developer.info"), "vulkanmod.ui.page.developer.info", null, false))
                .add(new NavNode(RouteId.parse("developer.stats"), "vulkanmod.ui.page.developer.stats", null, false));

        for (String modId : modIds) {
            builder.add(new NavNode(ModSettings.routeOf(modId), modName(modId), null, false));
        }
        for (String modId : screenOnlyModIds) {
            builder.add(new NavNode(ModSettings.routeOf(modId), modName(modId), null, false));
        }
        return builder.build();
    }

    public static String modName(String modId) {
        try {
            return ModList.get().getModContainerById(modId)
                    .map(container -> container.getModInfo().getDisplayName())
                    .filter(name -> !name.isBlank())
                    .orElse(modId);
        } catch (Throwable t) {
            return modId;
        }
    }
}
