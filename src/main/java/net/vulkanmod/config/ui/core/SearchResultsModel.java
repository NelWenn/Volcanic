package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SearchResultsModel {
    public static final int ROW_HEIGHT = 16;
    public static final int HEADER_HEIGHT = 13;

    private static final int PANEL_WIDTH = 260;
    private static final int PANEL_MARGIN_X = 12;
    private static final int PANEL_MARGIN_Y = 8;
    private static final int PANEL_PAD = 6;
    private static final int ROW_INSET_X = 4;

    public sealed interface Row permits Header, Hit {
    }

    public record Header(SettingSource source) implements Row {
        public Header {
            if (source == null) {
                throw new IllegalArgumentException("source must not be null");
            }
        }
    }

    public record Hit(SearchIndex.Entry entry) implements Row {
        public Hit {
            if (entry == null) {
                throw new IllegalArgumentException("entry must not be null");
            }
        }
    }

    private final Rect panel;
    private final List<Row> rows;
    private final List<Rect> boxes;

    private SearchResultsModel(Rect panel, List<Row> rows, List<Rect> boxes) {
        this.panel = panel;
        this.rows = rows;
        this.boxes = boxes;
    }

    public static SearchResultsModel of(List<SearchIndex.Entry> entries, Rect container) {
        if (entries == null) {
            throw new IllegalArgumentException("entries must not be null");
        }
        if (container == null) {
            throw new IllegalArgumentException("container must not be null");
        }

        List<Row> grouped = group(entries);
        Rect panel = panel(container, grouped);
        if (panel.isEmpty()) {
            return new SearchResultsModel(Rect.EMPTY, List.of(), List.of());
        }

        List<Row> placed = new ArrayList<>(grouped.size());
        List<Rect> boxes = new ArrayList<>(grouped.size());
        int top = panel.y() + PANEL_PAD;
        for (Row row : grouped) {
            int height = heightOf(row);
            if (top + height > panel.bottom() - PANEL_PAD) {
                break;
            }
            placed.add(row);
            boxes.add(new Rect(panel.x() + ROW_INSET_X, top,
                    panel.width() - ROW_INSET_X * 2, height));
            top += height;
        }
        while (!placed.isEmpty() && placed.get(placed.size() - 1) instanceof Header) {
            placed.remove(placed.size() - 1);
            boxes.remove(boxes.size() - 1);
        }
        return new SearchResultsModel(panel, List.copyOf(placed), List.copyOf(boxes));
    }

    public Rect panel() {
        return panel;
    }

    public List<Row> rows() {
        return rows;
    }

    public List<Rect> boxes() {
        return boxes;
    }

    public int indexAt(int mouseX, int mouseY) {
        return TabStripModel.indexAt(boxes, mouseX, mouseY);
    }

    public Optional<SearchIndex.Entry> hitAt(int index) {
        if (index < 0 || index >= rows.size()) {
            return Optional.empty();
        }
        return rows.get(index) instanceof Hit hit ? Optional.of(hit.entry()) : Optional.empty();
    }

    public int firstHit() {
        for (int index = 0; index < rows.size(); index++) {
            if (rows.get(index) instanceof Hit) {
                return index;
            }
        }
        return -1;
    }

    public int nextHit(int from, int direction) {
        if (direction == 0) {
            throw new IllegalArgumentException("direction must not be zero");
        }
        if (hitAt(from).isEmpty()) {
            return firstHit();
        }
        for (int index = from + direction; index >= 0 && index < rows.size(); index += direction) {
            if (rows.get(index) instanceof Hit) {
                return index;
            }
        }
        return from;
    }

    private static List<Row> group(List<SearchIndex.Entry> entries) {
        for (SearchIndex.Entry entry : entries) {
            if (entry == null) {
                throw new IllegalArgumentException("entry must not be null");
            }
        }
        List<Row> grouped = new ArrayList<>(entries.size() + SettingSource.values().length);
        for (SettingSource source : SettingSource.values()) {
            boolean announced = false;
            for (SearchIndex.Entry entry : entries) {
                if (entry.source() != source) {
                    continue;
                }
                if (!announced) {
                    grouped.add(new Header(source));
                    announced = true;
                }
                grouped.add(new Hit(entry));
            }
        }
        return grouped;
    }

    private static Rect panel(Rect container, List<Row> rows) {
        int width = Math.min(PANEL_WIDTH, container.width() - PANEL_MARGIN_X * 2);
        int available = container.height() - PANEL_MARGIN_Y * 2;
        int wanted = PANEL_PAD * 2 + Math.max(ROW_HEIGHT, heightOf(rows));
        int height = Math.min(available, wanted);
        if (container.isEmpty() || width <= 0 || height < PANEL_PAD * 2 + ROW_HEIGHT) {
            return Rect.EMPTY;
        }
        return new Rect(container.x() + (container.width() - width) / 2,
                container.y() + PANEL_MARGIN_Y, width, height);
    }

    private static int heightOf(List<Row> rows) {
        int total = 0;
        for (Row row : rows) {
            total += heightOf(row);
        }
        return total;
    }

    private static int heightOf(Row row) {
        return row instanceof Header ? HEADER_HEIGHT : ROW_HEIGHT;
    }
}
