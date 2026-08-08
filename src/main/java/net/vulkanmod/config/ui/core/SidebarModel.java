package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public final class SidebarModel {
    private static final int ROW_HEIGHT = 25;
    private static final int SECTION_HEIGHT = 16;

    public sealed interface Entry permits Section, Row {
    }

    public record Section(String labelKey) implements Entry {
        public Section {
            if (labelKey == null || labelKey.isBlank()) {
                throw new IllegalArgumentException("labelKey must not be blank");
            }
        }
    }

    public record Row(RouteId route, String titleKey) implements Entry {
        public Row {
            if (route == null) {
                throw new IllegalArgumentException("route must not be null");
            }
            if (titleKey == null || titleKey.isBlank()) {
                throw new IllegalArgumentException("titleKey must not be blank");
            }
        }
    }

    private final List<Entry> entries;
    private final ListModel listModel;

    public SidebarModel(NavTree tree) {
        if (tree == null) {
            throw new IllegalArgumentException("tree must not be null");
        }
        List<Entry> built = new ArrayList<>();
        ListModel model = new ListModel(0);
        for (NavNode node : tree.sidebarRows()) {
            if (node.sectionKey() != null) {
                built.add(new Section(node.sectionKey()));
                model.add(SECTION_HEIGHT);
            }
            built.add(new Row(node.route(), node.titleKey()));
            model.add(ROW_HEIGHT);
        }
        this.entries = List.copyOf(built);
        this.listModel = model;
    }

    public List<Entry> entries() {
        return entries;
    }

    public int entryCount() {
        return entries.size();
    }

    public int heightOf(int index) {
        return listModel.heightOf(index);
    }

    public int offsetOf(int index) {
        return listModel.offsetOf(index);
    }

    public int totalHeight() {
        return listModel.totalHeight();
    }

    public int maxScroll(int viewportHeight) {
        return listModel.maxScroll(viewportHeight);
    }

    public int clampScroll(int scroll, int viewportHeight) {
        return listModel.clampScroll(scroll, viewportHeight);
    }

    public int firstVisible(int scroll) {
        return listModel.firstVisible(scroll);
    }

    public int lastVisible(int scroll, int viewportHeight) {
        return listModel.lastVisible(scroll, viewportHeight);
    }

    public RouteId routeAt(int contentY) {
        int index = listModel.indexAt(contentY);
        if (index < 0) {
            return null;
        }
        return switch (entries.get(index)) {
            case Row(RouteId route, String titleKey) -> route;
            case Section(String labelKey) -> null;
        };
    }

    public int indexOfRoute(RouteId route) {
        if (route == null) {
            throw new IllegalArgumentException("route must not be null");
        }
        for (int i = 0; i < entries.size(); i++) {
            switch (entries.get(i)) {
                case Row(RouteId candidate, String titleKey) -> {
                    if (candidate.equals(route)) {
                        return i;
                    }
                }
                case Section(String labelKey) -> {
                }
            }
        }
        return -1;
    }
}
