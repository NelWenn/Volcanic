package net.vulkanmod.config.ui.core;

import java.util.ArrayList;
import java.util.List;

public final class FrameGraphLayout {
    public static final int GOOD = 0;
    public static final int SLOW = 1;
    public static final int SPIKE = 2;
    public static final int CLIPPED = 3;

    public static final int PAD_X = 14;
    public static final int TOP = 6;
    public static final int AXIS_W = 44;
    public static final int MARKER_H = 3;
    public static final int MARKER_GAP = 2;
    public static final int GC_BAND_H = 14;
    public static final int PAUSE_W = 72;
    public static final int PAUSE_H = 17;
    public static final int STUTTER_H = 13;
    public static final int PROFILE_H = 13;
    public static final int BLOCK_GAP = 16;
    public static final int HEADING_H = 11;
    public static final int HEADING_GAP = 6;
    public static final int CELL_H = 31;
    public static final int CELL_PAD = 6;
    public static final int CELL_GAP = 4;
    public static final int BAR_H = 12;
    public static final int READOUT_W = 132;
    public static final int READOUT_H = 34;
    public static final int NOTE_H = 24;
    public static final int ACTION_W = 104;
    public static final int ACTION_H = 16;
    public static final int ACTION_GAP = 6;
    public static final int SWATCH = 6;
    private static final int MAX_USABLE = 560;
    public static final int TILE_H = 20;
    public static final int LINE = 7;
    public static final int BOTTOM = 12;

    private static final int[] STUTTER_SHARE = {13, 17, 12, 15, 15, 28};
    private static final float[] LADDER = {8.4f, 16.7f, 33.4f, 50.0f, 66.7f, 100.0f, 200.0f, 500.0f};
    private static final int PLOT_H = 150;
    private static final int PLOT_H_MEDIUM = 116;
    private static final int PLOT_H_COMPACT = 88;

    public record Group(Rect heading, List<Rect> cells, Rect note) {
        public Group {
            cells = List.copyOf(cells);
        }
    }

    public record Page(Group fingerprint, Rect markers, Rect plot, Rect gcBand, Rect axis, Rect pause,
                       Rect legendLine, Rect legendKeys, Rect timeAxis, Rect verdict,
                       Rect frameLabel, Rect frameBar, Rect threadLabel, Rect threadBar,
                       List<Rect> tiles, Rect findingHeading, List<Rect> findings,
                       Rect stutterHeading, Rect stutterCaption, Rect stutterHead,
                       List<Rect> stutters,
                       Rect profileHead, List<Rect> profile, List<Rect> actions,
                       Group scene, Group terrain, Group memory, List<Rect> allocators, Group machine,
                       Rect bottleneck, Rect sampling, Rect legend, int height) {
        public Page {
            tiles = List.copyOf(tiles);
            findings = List.copyOf(findings);
            stutters = List.copyOf(stutters);
            profile = List.copyOf(profile);
        }
    }

    private static final Group NO_GROUP = new Group(Rect.EMPTY, List.of(), Rect.EMPTY);
    private static final Page NO_PAGE = new Page(NO_GROUP, Rect.EMPTY, Rect.EMPTY, Rect.EMPTY,
            Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, Rect.EMPTY,
            Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, List.of(), Rect.EMPTY, List.of(),
            Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, List.of(),
            Rect.EMPTY, List.of(), List.of(),
            NO_GROUP, NO_GROUP, NO_GROUP, List.of(), NO_GROUP, Rect.EMPTY, Rect.EMPTY, Rect.EMPTY, 0);

    private FrameGraphLayout() {
    }

    public record Counts(int fingerprint, int tiles, int findings, int stutters, int profile,
                        int scene, int terrain, int memory, int machine, int allocators) {
    }

    public static Page page(Rect content, Counts counts, int scroll, Breakpoint breakpoint) {
        if (content == null || counts == null || breakpoint == null) {
            throw new IllegalArgumentException("content, counts and breakpoint must not be null");
        }
        int margin = sideMargin(content.width());
        int usable = content.width() - margin * 2;
        if (content.isEmpty() || usable <= AXIS_W + PAUSE_W) {
            return NO_PAGE;
        }

        int left = content.x() + margin;
        int base = content.y() - scroll;
        int cursor = TOP;

        Group fingerprint = group(left, base, cursor, usable, counts.fingerprint(),
                gridColumns(breakpoint), CELL_H);
        cursor += groupHeight(counts.fingerprint(), gridColumns(breakpoint), CELL_H);

        Rect pause = new Rect(left + usable - PAUSE_W, base + cursor, PAUSE_W, PAUSE_H);
        cursor += PAUSE_H + 6;

        Rect markers = new Rect(left + AXIS_W, base + cursor, usable - AXIS_W, MARKER_H);
        cursor += MARKER_H + MARKER_GAP;
        int plotHeight = plotHeight(breakpoint);
        Rect plot = new Rect(left + AXIS_W, base + cursor, usable - AXIS_W, plotHeight);
        Rect axis = new Rect(left, base + cursor, AXIS_W - 4, plotHeight);
        cursor += plotHeight + 1;
        Rect gcBand = new Rect(left + AXIS_W, base + cursor, usable - AXIS_W, GC_BAND_H);
        cursor += GC_BAND_H + 5;
        Rect timeAxis = new Rect(left + AXIS_W, base + cursor, usable - AXIS_W, LINE);
        cursor += LINE + 5;
        Rect legendKeys = new Rect(left, base + cursor, usable, LINE);
        cursor += LINE + 4;
        Rect legendLine = new Rect(left, base + cursor, usable, LINE * 2);
        cursor += LINE * 2 + BLOCK_GAP;

        Rect verdict = new Rect(left, base + cursor, usable, TextScale.LINE_HEIGHT + 8);
        cursor += TextScale.LINE_HEIGHT + 8 + 9;
        Rect frameLabel = new Rect(left, base + cursor, usable, LINE);
        cursor += LINE + 3;
        Rect frameBar = new Rect(left, base + cursor, usable, BAR_H);
        cursor += BAR_H + 8;
        Rect threadLabel = new Rect(left, base + cursor, usable, LINE);
        cursor += LINE + 3;
        Rect threadBar = new Rect(left, base + cursor, usable, BAR_H);
        cursor += BAR_H + BLOCK_GAP;

        int tileColumns = tileColumns(breakpoint);
        int shownTiles = visibleTiles(counts.tiles(), breakpoint);
        List<Rect> tiles = cells(left, base, cursor, usable, shownTiles, tileColumns, TILE_H);
        cursor += gridHeight(shownTiles, tileColumns, TILE_H) + BLOCK_GAP;

        Rect findingHeading = Rect.EMPTY;
        List<Rect> findings = List.of();
        if (counts.findings() > 0) {
            findingHeading = new Rect(left, base + cursor, usable, HEADING_H);
            cursor += HEADING_H + HEADING_GAP;
            findings = cells(left, base, cursor, usable, counts.findings(), 1, NOTE_H);
            cursor += counts.findings() * (NOTE_H + CELL_GAP) - CELL_GAP + BLOCK_GAP;
        }

        Rect stutterHeading = Rect.EMPTY;
        Rect stutterCaption = Rect.EMPTY;
        Rect stutterHead = Rect.EMPTY;
        if (counts.stutters() > 0) {
            stutterHeading = new Rect(left, base + cursor, usable, HEADING_H);
            cursor += HEADING_H + HEADING_GAP;
            stutterCaption = new Rect(left, base + cursor, usable, LINE);
            cursor += LINE + 6;
            stutterHead = new Rect(left, base + cursor, usable, LINE);
            cursor += LINE + 4;
        }
        List<Rect> stutters = cells(left, base, cursor, usable, counts.stutters(), 1, STUTTER_H);
        cursor += gridHeight(counts.stutters(), 1, STUTTER_H) + BLOCK_GAP;

        Rect profileHead = counts.profile() > 0 ? new Rect(left, base + cursor, usable, LINE) : Rect.EMPTY;
        cursor += counts.profile() > 0 ? LINE + 4 : 0;
        List<Rect> profile = cells(left, base, cursor, usable, counts.profile(), 1, PROFILE_H);
        cursor += gridHeight(counts.profile(), 1, PROFILE_H) + BLOCK_GAP;

        int wide = gridColumns(breakpoint);
        Group scene = group(left, base, cursor, usable, counts.scene(), wide, CELL_H);
        cursor += groupHeight(counts.scene(), wide, CELL_H);
        Group terrain = group(left, base, cursor, usable, counts.terrain(), wide, CELL_H, true);
        cursor += groupHeight(counts.terrain(), wide, CELL_H, true);
        Group memory = group(left, base, cursor, usable, counts.memory(), wide, CELL_H, true);
        cursor += groupHeight(counts.memory(), wide, CELL_H, true);
        int allocatorRows = Math.max(1, counts.allocators());
        List<Rect> allocators = cells(left, base, cursor, usable, allocatorRows, 1, PROFILE_H);
        cursor += gridHeight(allocatorRows, 1, PROFILE_H) + BLOCK_GAP;
        Group machine = group(left, base, cursor, usable, counts.machine(), wide, CELL_H, true);
        cursor += groupHeight(counts.machine(), wide, CELL_H, true);

        List<Rect> actions = new ArrayList<>(3);
        for (int index = 0; index < 3; index++) {
            actions.add(new Rect(left + index * (ACTION_W + ACTION_GAP), base + cursor,
                    ACTION_W, ACTION_H));
        }
        cursor += ACTION_H + BLOCK_GAP;

        Rect bottleneck = new Rect(left, base + cursor, usable, LINE);
        cursor += LINE + 3;
        Rect sampling = new Rect(left, base + cursor, usable, LINE);
        cursor += LINE + 3;
        Rect legend = new Rect(left, base + cursor, usable, LINE * 2);
        return new Page(fingerprint, markers, plot, gcBand, axis, pause, legendLine, legendKeys,
                timeAxis, verdict, frameLabel, frameBar, threadLabel, threadBar, tiles,
                findingHeading, findings, stutterHeading, stutterCaption, stutterHead, stutters,
                profileHead, profile,
                List.copyOf(actions), scene, terrain, memory, allocators, machine,
                bottleneck, sampling, legend,
                contentHeight(counts, breakpoint));
    }

    public static Rect readout(Rect plot, Rect column) {
        if (plot.isEmpty() || column.isEmpty()) {
            return Rect.EMPTY;
        }
        int x = Math.max(plot.x(), Math.min(column.x() - READOUT_W / 2, plot.right() - READOUT_W));
        return new Rect(x, plot.y() + 4, Math.min(READOUT_W, plot.width()), READOUT_H);
    }

    public static int columnAt(Rect plot, int total, int mouseX) {
        if (plot.isEmpty() || total <= 0 || mouseX < plot.x() || mouseX >= plot.right()) {
            return -1;
        }
        int width = Math.max(1, plot.width() / total);
        int index = total - 1 - (plot.right() - 1 - mouseX) / width;
        return index < 0 || index >= total ? -1 : index;
    }

    private static int gridColumns(Breakpoint breakpoint) {
        return requireBreakpoint(breakpoint) == Breakpoint.WIDE ? 6
                : breakpoint == Breakpoint.MEDIUM ? 5 : 3;
    }

    private static Group group(int left, int base, int cursor, int usable, int count, int columns,
                               int cellHeight) {
        return group(left, base, cursor, usable, count, columns, cellHeight, false);
    }

    private static Group group(int left, int base, int cursor, int usable, int count, int columns,
                               int cellHeight, boolean noted) {
        if (count <= 0) {
            return NO_GROUP;
        }
        Rect heading = new Rect(left, base + cursor, usable, HEADING_H);
        List<Rect> cells = cells(left, base, cursor + HEADING_H + HEADING_GAP, usable,
                count, columns, cellHeight);
        Rect note = noted
                ? new Rect(left, base + cursor + HEADING_H + HEADING_GAP
                        + gridHeight(count, columns, cellHeight) + CELL_GAP, usable, NOTE_H)
                : Rect.EMPTY;
        return new Group(heading, cells, note);
    }

    private static List<Rect> cells(int left, int base, int cursor, int usable, int count,
                                    int columns, int cellHeight) {
        if (count <= 0) {
            return List.of();
        }
        int width = (usable - CELL_GAP * (columns - 1)) / Math.max(1, columns);
        List<Rect> out = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            out.add(new Rect(left + (index % columns) * (width + CELL_GAP),
                    base + cursor + (index / columns) * (cellHeight + CELL_GAP), width, cellHeight));
        }
        return List.copyOf(out);
    }

    private static int gridHeight(int count, int columns, int cellHeight) {
        if (count <= 0) {
            return 0;
        }
        int rows = (count + columns - 1) / columns;
        return rows * (cellHeight + CELL_GAP) - CELL_GAP;
    }

    private static int groupHeight(int count, int columns, int cellHeight) {
        return groupHeight(count, columns, cellHeight, false);
    }

    private static int groupHeight(int count, int columns, int cellHeight, boolean noted) {
        return count <= 0 ? 0
                : HEADING_H + HEADING_GAP + gridHeight(count, columns, cellHeight)
                        + (noted ? CELL_GAP + NOTE_H : 0) + BLOCK_GAP;
    }

    public static Rect cellLabel(Rect cell) {
        int width = cell.width() - CELL_PAD * 2;
        return cell.isEmpty() || width <= 0 ? Rect.EMPTY
                : new Rect(cell.x() + CELL_PAD, cell.y() + 4, width, LINE);
    }

    public static Rect cellValue(Rect cell) {
        int width = cell.width() - CELL_PAD * 2;
        return cell.isEmpty() || width <= 0 ? Rect.EMPTY
                : new Rect(cell.x() + CELL_PAD, cell.y() + 13, width, TextScale.LINE_HEIGHT);
    }

    public static Rect cellMeta(Rect cell) {
        int width = cell.width() - CELL_PAD * 2;
        return cell.isEmpty() || width <= 0 || cell.height() < 29 ? Rect.EMPTY
                : new Rect(cell.x() + CELL_PAD, cell.y() + 23, width, LINE);
    }

    public static Rect cellAccent(Rect cell) {
        return cell.isEmpty() ? Rect.EMPTY : new Rect(cell.x(), cell.y(), 2, cell.height());
    }

    public static Rect stutterColumn(Rect row, int index) {
        if (row.isEmpty() || index < 0 || index >= STUTTER_SHARE.length) {
            return Rect.EMPTY;
        }
        int x = row.x();
        for (int before = 0; before < index; before++) {
            x += row.width() * STUTTER_SHARE[before] / 100;
        }
        return new Rect(x, row.y(), row.width() * STUTTER_SHARE[index] / 100, row.height());
    }

    public static Rect swatch(Rect legend, int index) {
        return legend.isEmpty() ? Rect.EMPTY
                : new Rect(legend.x() + index * (legend.width() / 5),
                        legend.y() + (legend.height() - SWATCH) / 2, SWATCH, SWATCH);
    }

    public static Rect swatchLabel(Rect legend, int index) {
        Rect mark = swatch(legend, index);
        int width = legend.width() / 5 - SWATCH - 6;
        return mark.isEmpty() || width <= 0 ? Rect.EMPTY
                : new Rect(mark.right() + 3, legend.y(), width, legend.height());
    }

    public static int sideMargin(int contentWidth) {
        return Math.max(PAD_X, Math.min(contentWidth / 8, (contentWidth - MAX_USABLE) / 2));
    }

    public static int contentHeight(Counts counts, Breakpoint breakpoint) {
        int wide = gridColumns(breakpoint);
        int height = TOP
                + groupHeight(counts.fingerprint(), wide, CELL_H)
                + PAUSE_H + 6 + MARKER_H + MARKER_GAP + plotHeight(breakpoint) + 1 + GC_BAND_H + 5
                + LINE + 5 + LINE + 4 + LINE * 2 + BLOCK_GAP
                + TextScale.LINE_HEIGHT + 8 + 9 + LINE + 3 + BAR_H + 8 + LINE + 3 + BAR_H + BLOCK_GAP
                + gridHeight(visibleTiles(counts.tiles(), breakpoint), tileColumns(breakpoint), TILE_H)
                + BLOCK_GAP
                + gridHeight(counts.stutters(), 1, STUTTER_H) + BLOCK_GAP;
        if (counts.findings() > 0) {
            height += HEADING_H + HEADING_GAP
                    + counts.findings() * (NOTE_H + CELL_GAP) - CELL_GAP + BLOCK_GAP;
        }
        if (counts.stutters() > 0) {
            height += HEADING_H + HEADING_GAP + LINE + 6 + LINE + 4;
        }
        if (counts.profile() > 0) {
            height += LINE + 4;
        }
        height += gridHeight(counts.profile(), 1, PROFILE_H) + BLOCK_GAP
                + groupHeight(counts.scene(), wide, CELL_H)
                + groupHeight(counts.terrain(), wide, CELL_H, true)
                + groupHeight(counts.memory(), wide, CELL_H, true)
                + gridHeight(Math.max(1, counts.allocators()), 1, PROFILE_H) + BLOCK_GAP
                + groupHeight(counts.machine(), wide, CELL_H, true)
                + ACTION_H + BLOCK_GAP + LINE + 3 + LINE + 3 + LINE * 2;
        requireBreakpoint(breakpoint);
        return height + BOTTOM;
    }

    public static int maxScroll(Rect content, Counts counts, Breakpoint breakpoint) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        return Math.max(0, contentHeight(counts, breakpoint) - content.height());
    }

    public static int plotHeight(Breakpoint breakpoint) {
        return switch (requireBreakpoint(breakpoint)) {
            case COMPACT -> PLOT_H_COMPACT;
            case MEDIUM -> PLOT_H_MEDIUM;
            case WIDE -> PLOT_H;
        };
    }

    public static int tileColumns(Breakpoint breakpoint) {
        return requireBreakpoint(breakpoint) == Breakpoint.WIDE ? 6
                : breakpoint == Breakpoint.MEDIUM ? 4 : 3;
    }

    public static int visibleTiles(int tileCount, Breakpoint breakpoint) {
        requireBreakpoint(breakpoint);
        return tileCount;
    }

    public static float ceilingFor(float medianMs, float targetMs) {
        float typical = Math.max(medianMs <= 0.0f ? 0.0f : medianMs * 2.5f,
                targetMs <= 0.0f ? 0.0f : targetMs * 2.0f);
        return ceiling(typical <= 0.0f ? LADDER[1] : typical);
    }

    public static float ceiling(float maxMs) {
        for (float rung : LADDER) {
            if (maxMs <= rung) {
                return rung;
            }
        }
        return LADDER[LADDER.length - 1];
    }

    public static float spikeFloor(float medianMs) {
        return FrameSamples.spikeFloor(medianMs);
    }

    public static int classify(float ms, float targetMs, float spikeFloorMs, float ceilingMs) {
        if (ms > ceilingMs) {
            return CLIPPED;
        }
        if (spikeFloorMs > 0.0f && ms >= spikeFloorMs) {
            return SPIKE;
        }
        return targetMs > 0.0f && ms <= targetMs ? GOOD : SLOW;
    }

    public static int columnTop(Rect plot, float ms, float ceilingMs) {
        if (plot.isEmpty() || ceilingMs <= 0.0f) {
            return plot.y();
        }
        float clamped = Math.max(0.0f, Math.min(ms, ceilingMs));
        return plot.bottom() - Math.round(plot.height() * clamped / ceilingMs);
    }

    public static Rect column(Rect plot, int index, int total, float lowMs, float highMs,
                              float ceilingMs) {
        if (plot.isEmpty() || total <= 0 || index < 0 || index >= total) {
            return Rect.EMPTY;
        }
        int width = Math.max(1, plot.width() / total);
        int x = plot.right() - (total - index) * width;
        if (x < plot.x()) {
            return Rect.EMPTY;
        }
        int top = columnTop(plot, highMs, ceilingMs);
        int bottom = columnTop(plot, lowMs, ceilingMs);
        return new Rect(x, top, width, Math.max(1, bottom - top));
    }

    public static Rect gcColumn(Rect band, Rect column, float gcMs, float fullMs) {
        if (band.isEmpty() || column.isEmpty() || gcMs <= 0.0f || fullMs <= 0.0f) {
            return Rect.EMPTY;
        }
        int height = Math.max(1, Math.round(band.height() * Math.min(gcMs, fullMs) / fullMs));
        return new Rect(column.x(), band.y(), column.width(), height);
    }

    public static Rect marker(Rect markers, Rect column) {
        return markers.isEmpty() || column.isEmpty() ? Rect.EMPTY
                : new Rect(column.x(), markers.y(), 1, markers.height());
    }

    public static Rect baseline(Rect plot, float targetMs, float ceilingMs) {
        if (plot.isEmpty() || targetMs <= 0.0f || targetMs > ceilingMs) {
            return Rect.EMPTY;
        }
        return new Rect(plot.x(), columnTop(plot, targetMs, ceilingMs), plot.width(), 1);
    }

    public static List<Rect> gridlines(Rect plot, int divisions) {
        if (plot.isEmpty() || divisions < 2) {
            return List.of();
        }
        List<Rect> lines = new ArrayList<>(divisions - 1);
        for (int i = 1; i < divisions; i++) {
            lines.add(new Rect(plot.x(), plot.bottom() - plot.height() * i / divisions, plot.width(), 1));
        }
        return List.copyOf(lines);
    }

    public static Rect axisLabel(Rect axis, int index, int divisions) {
        if (axis.isEmpty() || divisions < 1 || index < 0 || index > divisions) {
            return Rect.EMPTY;
        }
        int y = axis.bottom() - axis.height() * index / divisions;
        return new Rect(axis.x(), Math.min(axis.bottom() - LINE, Math.max(axis.y(), y - LINE / 2)),
                axis.width(), LINE);
    }

    public static Rect tileLabel(Rect tile) {
        return tile.isEmpty() ? Rect.EMPTY : new Rect(tile.x(), tile.y() + 3, tile.width(), LINE);
    }

    public static Rect tileValue(Rect tile) {
        return tile.isEmpty() ? Rect.EMPTY
                : new Rect(tile.x(), tile.y() + 13, tile.width(), TextScale.LINE_HEIGHT);
    }

    private static Breakpoint requireBreakpoint(Breakpoint breakpoint) {
        if (breakpoint == null) {
            throw new IllegalArgumentException("breakpoint must not be null");
        }
        return breakpoint;
    }
}
