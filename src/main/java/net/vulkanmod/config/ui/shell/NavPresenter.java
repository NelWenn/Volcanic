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
import net.vulkanmod.config.ui.core.ProfileChipRow;
import net.vulkanmod.config.ui.core.ProfileMatcher;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.SettingId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SettingType;
import net.vulkanmod.config.ui.core.SidebarModel;
import net.vulkanmod.config.ui.mods.ModScreens;
import net.vulkanmod.config.ui.mods.ModSettings;
import net.vulkanmod.config.ui.settings.SettingBinding;
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

    private final NavTree tree;
    private final NavStack stack;
    private final SidebarModel sidebar;
    private final FocusModel focus;
    private final SettingsCatalog catalog = new SettingsCatalog();
    private final PendingChanges pending = new PendingChanges();
    private final DeferredValues deferred = new DeferredValues();
    private final List<String> screenOnlyModIds;
    private List<ProfileChipRow.Chip> profileChips;
    private Favorites favorites;

    public NavPresenter() {
        List<String> modIds = catalog.modIds();
        this.screenOnlyModIds = screenOnlyModIds(modIds);
        this.tree = buildTree(modIds, screenOnlyModIds);
        this.stack = new NavStack(tree, destinationOf(tree, tree.defaultRoute()));
        this.sidebar = new SidebarModel(tree);
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
        return sidebar;
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
        UiState.save(UiState.PATH, favorites());
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
            this.favorites = UiState.load(UiState.PATH);
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

    public int contentRowCount() {
        return isOverview() ? catalog.overview().rows().size() : settings().size();
    }

    public boolean isOverview() {
        return OVERVIEW_ROUTE.equals(stack().current());
    }

    public void apply() {
        this.profileChips = null;
        deferred.drainTo((id, value) -> {
            catalog.binding(id).set(value);
            pending.unmark(id);
        });
        Initializer.CONFIG.write();
    }

    public void discard() {
        this.profileChips = null;
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
        while (route.depth() > 1) {
            route = route.parent();
        }
        return route;
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
        while (!children.isEmpty()) {
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
    }

    static NavTree buildTree(List<String> modIds, List<String> screenOnlyModIds) {
        if (modIds == null || screenOnlyModIds == null) {
            throw new IllegalArgumentException("modIds and screenOnlyModIds must not be null");
        }
        NavTree.Builder builder = new NavTree.Builder()
                .add(new NavNode(RouteId.parse("overview"), "vulkanmod.ui.page.overview", "vulkanmod.ui.section.volcanic", true))
                .add(new NavNode(RouteId.parse("display"), "vulkanmod.ui.page.display", null, true))
                .add(new NavNode(RouteId.parse("display.general"), "vulkanmod.ui.page.display.general", null, false))
                .add(new NavNode(RouteId.parse("display.interface"), "vulkanmod.ui.page.display.interface", null, false))
                .add(new NavNode(RouteId.parse("display.advanced"), "vulkanmod.ui.page.display.advanced", null, false))
                .add(new NavNode(RouteId.parse("rendering"), "vulkanmod.ui.page.rendering", null, true))
                .add(new NavNode(RouteId.parse("rendering.general"), "vulkanmod.ui.page.rendering.general", null, false))
                .add(new NavNode(RouteId.parse("rendering.distance"), "vulkanmod.ui.page.rendering.distance", null, false))
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
                .add(new NavNode(RouteId.parse("quality.particles"), "vulkanmod.ui.page.quality.particles", null, false))
                .add(new NavNode(RouteId.parse("quality.entities"), "vulkanmod.ui.page.quality.entities", null, false))
                .add(new NavNode(RouteId.parse("shaders"), "vulkanmod.ui.page.shaders", null, true))
                .add(new NavNode(RouteId.parse("shaders.current"), "vulkanmod.ui.page.shaders.current", null, false))
                .add(new NavNode(RouteId.parse("shaders.packs"), "vulkanmod.ui.page.shaders.packs", null, false))
                .add(new NavNode(RouteId.parse("shaders.profiles"), "vulkanmod.ui.page.shaders.profiles", null, false))
                .add(new NavNode(RouteId.parse("shaders.settings"), "vulkanmod.ui.page.shaders.settings", null, false))
                .add(new NavNode(RouteId.parse("mods"), "vulkanmod.ui.page.mods", "vulkanmod.ui.section.content", true))
                .add(new NavNode(RouteId.parse("favorites"), "vulkanmod.ui.page.favorites", null, true))
                .add(new NavNode(RouteId.parse("advanced"), "vulkanmod.ui.page.advanced", "vulkanmod.ui.section.system", true))
                .add(new NavNode(RouteId.parse("advanced.renderer"), "vulkanmod.ui.page.advanced.renderer", null, false))
                .add(new NavNode(RouteId.parse("advanced.synchronization"), "vulkanmod.ui.page.advanced.synchronization", null, false))
                .add(new NavNode(RouteId.parse("advanced.compatibility"), "vulkanmod.ui.page.advanced.compatibility", null, false))
                .add(new NavNode(RouteId.parse("experimental"), "vulkanmod.ui.page.experimental", null, true))
                .add(new NavNode(RouteId.parse("developer"), "vulkanmod.ui.page.developer", null, true));

        for (String modId : modIds) {
            builder.add(new NavNode(ModSettings.routeOf(modId), modName(modId), null, false));
        }
        for (String modId : screenOnlyModIds) {
            builder.add(new NavNode(ModSettings.routeOf(modId), modName(modId), null, false));
        }
        return builder.build();
    }

    private static String modName(String modId) {
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
