package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SidebarModel {
    private static final int ROW_HEIGHT = 25;
    private static final int SECTION_HEIGHT = 16;
    public static final String SECTION_SYSTEM = "vulkanmod.ui.section.system";

    public sealed interface Entry permits Section, Row {
    }

    public record Section(String labelKey, boolean collapsed) implements Entry {
        public Section {
            if (labelKey == null || labelKey.isBlank()) {
                throw new IllegalArgumentException("labelKey must not be blank");
            }
        }
    }

    public record Row(RouteId route, String titleKey, int depth) implements Entry {
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
        this(tree, Set.of());
    }

    public SidebarModel(NavTree tree, Set<String> collapsed) {
        if (tree == null) {
            throw new IllegalArgumentException("tree must not be null");
        }
        if (collapsed == null) {
            throw new IllegalArgumentException("collapsed must not be null");
        }
        List<Entry> built = new ArrayList<>();
        ListModel model = new ListModel(0);
        String section = null;
        for (NavNode node : tree.sidebarRows()) {
            if (node.sectionKey() != null) {
                section = node.sectionKey();
                built.add(new Section(section, collapsed.contains(section)));
                model.add(SECTION_HEIGHT);
            }
            if (section != null && collapsed.contains(section)) {
                continue;
            }
            built.add(new Row(node.route(), node.titleKey(), node.route().depth()));
            model.add(ROW_HEIGHT);
        }
        this.entries = List.copyOf(built);
        this.listModel = model;
    }

    public static Set<String> collapsedOrDefault(List<String> stored) {
        if (stored == null) {
            return Set.of(SECTION_SYSTEM);
        }
        return Set.copyOf(new LinkedHashSet<>(stored));
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

    public int entryIndexAt(int contentY) {
        return listModel.indexAt(contentY);
    }

    public Entry entryAt(int index) {
        return index < 0 || index >= entries.size() ? null : entries.get(index);
    }
}
