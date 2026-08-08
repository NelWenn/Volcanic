package net.vulkanmod.config.ui.core;

public record NavNode(RouteId route, String titleKey, String sectionKey, boolean sidebarVisible) {

    public NavNode {
        if (route == null) {
            throw new IllegalArgumentException("route must not be null");
        }
        if (route.equals(RouteId.root())) {
            throw new IllegalArgumentException("route must not be root");
        }
        if (titleKey == null || titleKey.isBlank()) {
            throw new IllegalArgumentException("titleKey must not be blank");
        }
    }
}
