package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SearchIndex {
    private static final int TITLE_PREFIX = 0;
    private static final int TITLE_SUBSTRING = 1;
    private static final int CHOICE = 2;
    private static final int DESCRIPTION = 3;
    private static final int NO_MATCH = -1;

    public record Entry(SettingId id, String title, String description, List<String> choices,
                        RouteId route, SettingSource source) {
        public Entry {
            if (id == null) {
                throw new IllegalArgumentException("id must not be null");
            }
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("title must not be blank: " + id);
            }
            if (description == null) {
                throw new IllegalArgumentException("description must not be null; use \"\": " + id);
            }
            if (choices == null) {
                throw new IllegalArgumentException("choices must not be null; use an empty list: " + id);
            }
            for (String choice : choices) {
                if (choice == null || choice.isBlank()) {
                    throw new IllegalArgumentException("choice must not be blank: " + id);
                }
            }
            if (route == null) {
                throw new IllegalArgumentException("route must not be null: " + id);
            }
            if (source == null) {
                throw new IllegalArgumentException("source must not be null: " + id);
            }
            choices = List.copyOf(choices);
        }
    }

    private record Row(Entry entry, String title, String description, List<String> choices) {
    }

    private final List<Row> rows;

    private SearchIndex(List<Row> rows) {
        this.rows = rows;
    }

    public static SearchIndex of(List<Entry> entries) {
        if (entries == null) {
            throw new IllegalArgumentException("entries must not be null");
        }
        List<Row> rows = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            if (entry == null) {
                throw new IllegalArgumentException("entry must not be null");
            }
            rows.add(new Row(entry, fold(entry.title()), fold(entry.description()),
                    entry.choices().stream().map(SearchIndex::fold).toList()));
        }
        return new SearchIndex(List.copyOf(rows));
    }

    public List<Entry> search(String query, int limit) {
        if (query == null) {
            throw new IllegalArgumentException("query must not be null");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1: " + limit);
        }
        String needle = fold(query.trim());
        if (needle.isEmpty()) {
            return List.of();
        }
        List<Entry> hits = new ArrayList<>();
        for (int tier = TITLE_PREFIX; tier <= DESCRIPTION; tier++) {
            for (Row row : rows) {
                if (hits.size() == limit) {
                    return List.copyOf(hits);
                }
                if (rank(row, needle) == tier) {
                    hits.add(row.entry());
                }
            }
        }
        return List.copyOf(hits);
    }

    private static int rank(Row row, String needle) {
        if (row.title().startsWith(needle)) {
            return TITLE_PREFIX;
        }
        if (row.title().contains(needle)) {
            return TITLE_SUBSTRING;
        }
        for (String choice : row.choices()) {
            if (choice.contains(needle)) {
                return CHOICE;
            }
        }
        if (row.description().contains(needle)) {
            return DESCRIPTION;
        }
        return NO_MATCH;
    }

    private static String fold(String text) {
        return text.toLowerCase(Locale.ROOT);
    }
}
