package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NavTree {
    private final Map<RouteId, NavNode> nodes;
    private final Map<RouteId, List<NavNode>> children;
    private final List<NavNode> sidebarRows;

    private NavTree(Map<RouteId, NavNode> nodes) {
        this.nodes = nodes;

        Map<RouteId, List<NavNode>> childrenByParent = new LinkedHashMap<>();
        List<NavNode> rows = new ArrayList<>();
        for (NavNode node : nodes.values()) {
            childrenByParent.computeIfAbsent(node.route().parent(), key -> new ArrayList<>()).add(node);
            if (node.route().depth() == 1 && node.sidebarVisible()) {
                rows.add(node);
            }
        }

        Map<RouteId, List<NavNode>> immutableChildren = new LinkedHashMap<>();
        for (Map.Entry<RouteId, List<NavNode>> entry : childrenByParent.entrySet()) {
            immutableChildren.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.children = immutableChildren;
        this.sidebarRows = List.copyOf(rows);
    }

    public NavNode find(RouteId route) {
        return nodes.get(route);
    }

    public List<NavNode> children(RouteId route) {
        return children.getOrDefault(route, List.of());
    }

    public List<NavNode> sidebarRows() {
        return sidebarRows;
    }

    public boolean contains(RouteId route) {
        return nodes.containsKey(route);
    }

    public int size() {
        return nodes.size();
    }

    public RouteId defaultRoute() {
        if (sidebarRows.isEmpty()) {
            throw new IllegalStateException("tree has no sidebar rows, so it has no default route");
        }
        return sidebarRows.get(0).route();
    }

    public static final class Builder {
        private final Map<RouteId, NavNode> nodes = new LinkedHashMap<>();

        public Builder add(NavNode node) {
            RouteId route = node.route();
            if (nodes.containsKey(route)) {
                throw new IllegalArgumentException("duplicate route: " + route);
            }
            RouteId parent = route.parent();
            if (!parent.equals(RouteId.root()) && !nodes.containsKey(parent)) {
                throw new IllegalArgumentException("parent route not present: " + parent);
            }
            nodes.put(route, node);
            return this;
        }

        public NavTree build() {
            return new NavTree(new LinkedHashMap<>(nodes));
        }
    }
}
