package net.vulkanmod.config.ui.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class NavStack {
    private final NavTree tree;
    private final Deque<RouteId> back = new ArrayDeque<>();
    private final Deque<RouteId> forward = new ArrayDeque<>();
    private RouteId current;

    public NavStack(NavTree tree, RouteId initial) {
        if (tree == null) {
            throw new IllegalArgumentException("tree must not be null");
        }
        if (initial == null || !tree.contains(initial)) {
            throw new IllegalArgumentException("initial route must be present in the tree: " + initial);
        }
        this.tree = tree;
        this.current = initial;
    }

    public RouteId current() {
        return current;
    }

    public boolean navigate(RouteId route) {
        if (route == null) {
            throw new IllegalArgumentException("route must not be null");
        }
        if (route.equals(current) || !tree.contains(route)) {
            return false;
        }
        back.push(current);
        current = route;
        forward.clear();
        return true;
    }

    public boolean back() {
        if (back.isEmpty()) {
            return false;
        }
        forward.push(current);
        current = back.pop();
        return true;
    }

    public boolean forward() {
        if (forward.isEmpty()) {
            return false;
        }
        back.push(current);
        current = forward.pop();
        return true;
    }

    public boolean canGoBack() {
        return !back.isEmpty();
    }

    public boolean canGoForward() {
        return !forward.isEmpty();
    }

    public List<RouteId> trail() {
        List<RouteId> reversed = new ArrayList<>();
        RouteId route = current;
        while (route.depth() > 0) {
            reversed.add(route);
            route = route.parent();
        }
        List<RouteId> result = new ArrayList<>(reversed.size());
        for (int i = reversed.size() - 1; i >= 0; i--) {
            result.add(reversed.get(i));
        }
        return List.copyOf(result);
    }
}
