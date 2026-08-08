package net.vulkanmod.config.ui.shell;

import net.vulkanmod.config.ui.core.FocusModel;
import net.vulkanmod.config.ui.core.FocusRing;
import net.vulkanmod.config.ui.core.NavNode;
import net.vulkanmod.config.ui.core.NavStack;
import net.vulkanmod.config.ui.core.NavTree;
import net.vulkanmod.config.ui.core.PendingChanges;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.SettingMeta;
import net.vulkanmod.config.ui.core.SidebarModel;
import net.vulkanmod.config.ui.settings.SettingBinding;
import net.vulkanmod.config.ui.settings.SettingsCatalog;

import java.util.List;

public final class NavPresenter {
    public static final String REGION_SIDEBAR = "sidebar";
    public static final String REGION_CONTENT = "content";

    private final NavTree tree;
    private final NavStack stack;
    private final SidebarModel sidebar;
    private final FocusModel focus;
    private final SettingsCatalog catalog = new SettingsCatalog();
    private final PendingChanges pending = new PendingChanges();

    public NavPresenter() {
        this.tree = buildTree();
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
        return catalog.registry().forRoute(stack.current());
    }

    public boolean activate(SettingMeta meta) {
        if (meta == null) {
            throw new IllegalArgumentException("meta must not be null");
        }
        if (!catalog.enabled(meta.id())) {
            return false;
        }

        SettingBinding binding = catalog.binding(meta.id());
        switch (meta.type()) {
            case BOOL -> binding.set(!boolValue(meta, binding.get()));
            case ENUM -> {
                List<String> choices = binding.choices();
                if (choices.isEmpty()) {
                    return false;
                }
                binding.set(cycled(choices, binding.get()));
            }
            case INT -> {
                return false;
            }
        }
        pending.mark(meta.id(), meta.scope());
        return true;
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
        if (choices == null || choices.isEmpty()) {
            throw new IllegalArgumentException("choices must not be empty");
        }
        return choices.get((choices.indexOf(current) + 1) % choices.size());
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
    }

    private static NavTree buildTree() {
        return new NavTree.Builder()
                .add(new NavNode(RouteId.parse("overview"), "vulkanmod.ui.page.overview", "vulkanmod.ui.section.volcanic", true))
                .add(new NavNode(RouteId.parse("display"), "vulkanmod.ui.page.display", null, true))
                .add(new NavNode(RouteId.parse("display.general"), "vulkanmod.ui.page.display.general", null, false))
                .add(new NavNode(RouteId.parse("display.resolution"), "vulkanmod.ui.page.display.resolution", null, false))
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
                .add(new NavNode(RouteId.parse("developer"), "vulkanmod.ui.page.developer", null, true))
                .build();
    }
}
