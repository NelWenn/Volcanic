package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SidebarModel {
    private static final int ROW_HEIGHT = 25;
    public static final int ROW_TEXT_X = 17;
    public static final int ROW_INDENT = 10;
    public static final int ROW_INSET_X = 4;
    public static final int ROW_INSET_Y = 1;
    public static final int STEM_X = ROW_INSET_X + ROW_INDENT / 2;
    public static final int TICK_GAP = 2;
    private static final Rail NO_RAIL = new Rail(Rect.EMPTY, List.of());
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

    public record Rail(Rect stem, List<Rect> ticks) {
        public Rail {
            ticks = List.copyOf(ticks);
        }
    }

    public static Rect rowBox(Rect sidebar, int top, int height, int depth) {
        if (sidebar == null) {
            throw new IllegalArgumentException("sidebar must not be null");
        }
        int inset = ROW_INSET_X + Math.max(0, depth - 1) * ROW_INDENT;
        return new Rect(sidebar.x() + inset, top + ROW_INSET_Y,
                Math.max(0, sidebar.width() - inset - ROW_INSET_X),
                Math.max(0, height - ROW_INSET_Y * 2));
    }

    public Rail rail(Rect sidebar, int scroll, RouteId parent) {
        if (sidebar == null || parent == null) {
            throw new IllegalArgumentException("sidebar and parent must not be null");
        }
        int index = indexOfRow(parent);
        if (sidebar.isEmpty() || index < 0) {
            return NO_RAIL;
        }

        int stemX = sidebar.x() + STEM_X;
        int top = sidebar.y() - scroll + offsetOf(index) + heightOf(index) - ROW_INSET_Y;
        List<Rect> ticks = new ArrayList<>();
        int last = top;
        for (int child = index + 1; child < entries.size(); child++) {
            if (!(entries.get(child) instanceof Row(RouteId route, String ignored, int depth))
                    || !parent.isAncestorOf(route)) {
                break;
            }
            int childTop = sidebar.y() - scroll + offsetOf(child);
            Rect box = rowBox(sidebar, childTop, heightOf(child), depth);
            int centre = childTop + heightOf(child) / 2;
            last = centre;
            int width = box.x() - TICK_GAP - stemX;
            if (width > 0 && centre >= sidebar.y() && centre < sidebar.bottom()) {
                ticks.add(new Rect(stemX, centre, width, 1));
            }
        }
        if (ticks.isEmpty() && last == top) {
            return NO_RAIL;
        }

        int stemTop = Math.max(top, sidebar.y());
        int stemBottom = Math.min(last, sidebar.bottom());
        Rect stem = stemBottom <= stemTop ? Rect.EMPTY
                : new Rect(stemX, stemTop, 1, stemBottom - stemTop);
        return new Rail(stem, ticks);
    }

    private int indexOfRow(RouteId route) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index) instanceof Row(RouteId candidate, String ignored, int depth)
                    && candidate.equals(route)) {
                return index;
            }
        }
        return -1;
    }
}
