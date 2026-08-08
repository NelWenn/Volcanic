package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public record RouteId(List<String> segments) {
    private static final char SEPARATOR = '.';
    private static final RouteId ROOT = new RouteId(List.of());

    public RouteId {
        if (segments == null) {
            throw new IllegalArgumentException("segments must not be null");
        }
        for (String segment : segments) {
            if (segment == null || segment.isBlank()) {
                throw new IllegalArgumentException("route segment must not be blank");
            }
        }
        segments = List.copyOf(segments);
    }

    public static RouteId root() {
        return ROOT;
    }

    public static RouteId parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        if (value.isEmpty()) {
            return ROOT;
        }
        return new RouteId(List.of(value.split("\\" + SEPARATOR, -1)));
    }

    public RouteId child(String segment) {
        List<String> next = new ArrayList<>(segments);
        next.add(segment);
        return new RouteId(next);
    }

    public RouteId parent() {
        if (segments.isEmpty()) {
            return ROOT;
        }
        return new RouteId(segments.subList(0, segments.size() - 1));
    }

    public int depth() {
        return segments.size();
    }

    public boolean isAncestorOf(RouteId other) {
        if (other == null || other.depth() <= depth()) {
            return false;
        }
        return other.segments.subList(0, depth()).equals(segments);
    }

    @Override
    public String toString() {
        return String.join(String.valueOf(SEPARATOR), segments);
    }
}
