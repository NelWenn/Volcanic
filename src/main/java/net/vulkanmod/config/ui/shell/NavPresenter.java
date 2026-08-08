package net.vulkanmod.config.ui.shell;

import net.vulkanmod.config.ui.core.FocusModel;
import net.vulkanmod.config.ui.core.NavNode;
import net.vulkanmod.config.ui.core.NavStack;
import net.vulkanmod.config.ui.core.NavTree;
import net.vulkanmod.config.ui.core.RouteId;
import net.vulkanmod.config.ui.core.SidebarModel;

import java.util.List;

public final class NavPresenter {
    private final NavTree tree;
    private final NavStack stack;
    private final SidebarModel sidebar;
    private final FocusModel focus;

    public NavPresenter() {
        this.tree = buildTree();
        this.stack = new NavStack(tree, tree.defaultRoute());
        this.sidebar = new SidebarModel(tree);
        this.focus = new FocusModel();
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

    public boolean navigate(RouteId route) {
        return stack.navigate(route);
    }

    public String currentTitleKey() {
        return tree.find(stack.current()).titleKey();
    }

    public List<NavNode> subTabs() {
        RouteId topLevel = stack.current();
        while (topLevel.depth() > 1) {
            topLevel = topLevel.parent();
        }
        return tree.children(topLevel);
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
                .add(new NavNode(RouteId.parse("favorites"), "vulkanmod.ui.page.favorites", "vulkanmod.ui.page.favorites", true))
                .add(new NavNode(RouteId.parse("advanced"), "vulkanmod.ui.page.advanced", "vulkanmod.ui.section.system", true))
                .add(new NavNode(RouteId.parse("advanced.renderer"), "vulkanmod.ui.page.advanced.renderer", null, false))
                .add(new NavNode(RouteId.parse("advanced.synchronization"), "vulkanmod.ui.page.advanced.synchronization", null, false))
                .add(new NavNode(RouteId.parse("advanced.compatibility"), "vulkanmod.ui.page.advanced.compatibility", null, false))
                .add(new NavNode(RouteId.parse("experimental"), "vulkanmod.ui.page.experimental", null, true))
                .add(new NavNode(RouteId.parse("developer"), "vulkanmod.ui.page.developer", null, true))
                .build();
    }
}
