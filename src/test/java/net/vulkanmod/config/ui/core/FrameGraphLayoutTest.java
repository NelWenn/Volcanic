package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FrameGraphLayout")
class FrameGraphLayoutTest {
    private static final Rect CONTENT = new Rect(132, 32, 500, 600);
    private static final FrameGraphLayout.Counts COUNTS =
            new FrameGraphLayout.Counts(6, 6, 3, 3, 4, 6, 4, 4, 6, 2);

    private static FrameGraphLayout.Counts counts(int tiles) {
        return new FrameGraphLayout.Counts(6, tiles, 3, 3, 4, 6, 4, 4, 6, 2);
    }

    @Test
    @DisplayName("the scale follows the usual frame time, so one spike cannot squash the plot")
    void theScaleFollowsTheTypicalFrame() {
        float steady = FrameGraphLayout.ceilingFor(8.3f, 8.3f);

        assertEquals(33.4f, steady);
        assertEquals(steady, FrameGraphLayout.ceilingFor(8.3f, 8.3f),
                "a single 500 ms stall must not change the scale — it clips instead");
        assertTrue(FrameGraphLayout.ceilingFor(33.0f, 16.7f) > steady,
                "a genuinely slower session does raise the scale");
    }

    @Test
    @DisplayName("the ceiling snaps to a rung that is a round frame rate")
    void ceilingSnapsToTheLadder() {
        assertEquals(16.7f, FrameGraphLayout.ceiling(16.7f));
        assertEquals(16.7f, FrameGraphLayout.ceiling(9.0f));
        assertEquals(8.4f, FrameGraphLayout.ceiling(8.4f));
        assertEquals(33.4f, FrameGraphLayout.ceiling(20.0f));
        assertEquals(500.0f, FrameGraphLayout.ceiling(9000.0f), "an absurd stall must still land on the top rung");
    }

    @Test
    @DisplayName("a column grows upward with the frame time and never leaves the plot")
    void columnsGrowUpwardAndStayInside() {
        Rect plot = FrameGraphLayout.page(CONTENT, COUNTS, 0, Breakpoint.WIDE).plot();

        assertEquals(plot.bottom(), FrameGraphLayout.columnTop(plot, 0.0f, 33.4f));
        assertEquals(plot.y(), FrameGraphLayout.columnTop(plot, 33.4f, 33.4f));
        assertEquals(plot.y(), FrameGraphLayout.columnTop(plot, 900.0f, 33.4f),
                "an off-scale frame clamps to the top rather than escaping");
        assertTrue(FrameGraphLayout.columnTop(plot, 16.7f, 33.4f) > plot.y());
    }

    @Test
    @DisplayName("the newest bucket is flush with the right edge")
    void theNewestBucketSitsAtTheRightEdge() {
        Rect plot = FrameGraphLayout.page(CONTENT, COUNTS, 0, Breakpoint.WIDE).plot();

        Rect last = FrameGraphLayout.column(plot, 39, 40, 8.0f, 16.0f, 33.4f);
        Rect first = FrameGraphLayout.column(plot, 0, 40, 8.0f, 16.0f, 33.4f);

        assertEquals(plot.right(), last.right());
        assertTrue(first.x() >= plot.x());
        assertTrue(last.y() >= plot.y() && last.bottom() <= plot.bottom());
    }

    @Test
    @DisplayName("a bucket band spans from its best frame to its worst")
    void aBandSpansMinToMax() {
        Rect plot = FrameGraphLayout.page(CONTENT, COUNTS, 0, Breakpoint.WIDE).plot();

        Rect narrow = FrameGraphLayout.column(plot, 10, 40, 15.0f, 16.0f, 33.4f);
        Rect wide = FrameGraphLayout.column(plot, 10, 40, 4.0f, 30.0f, 33.4f);

        assertTrue(wide.height() > narrow.height(), "a volatile bucket must read as a taller band");
        assertTrue(wide.y() < narrow.y(), "the worst frame sets the top of the band");
    }

    @Test
    @DisplayName("a frame is graded against the target, the spike floor and the ceiling")
    void classifyMarksTheBoundaries() {
        assertEquals(FrameGraphLayout.GOOD, FrameGraphLayout.classify(16.0f, 16.7f, 40.0f, 33.4f));
        assertEquals(FrameGraphLayout.GOOD, FrameGraphLayout.classify(16.7f, 16.7f, 40.0f, 33.4f));
        assertEquals(FrameGraphLayout.SLOW, FrameGraphLayout.classify(20.0f, 16.7f, 40.0f, 33.4f));
        assertEquals(FrameGraphLayout.SPIKE, FrameGraphLayout.classify(40.0f, 16.7f, 40.0f, 100.0f));
        assertEquals(FrameGraphLayout.CLIPPED, FrameGraphLayout.classify(120.0f, 16.7f, 40.0f, 100.0f));
    }

    @Test
    @DisplayName("the spike floor never calls a fast frame a spike")
    void spikeFloorHasAnAbsoluteGuard() {
        assertEquals(7.0f, FrameGraphLayout.spikeFloor(2.0f),
                "at 500 fps a 4.5 ms frame is not a stall");
        assertEquals(40.0f, FrameGraphLayout.spikeFloor(20.0f));
    }

    @Test
    @DisplayName("nothing on the page overlaps anything else")
    void pagePartsNeverOverlap() {
        for (Breakpoint breakpoint : Breakpoint.values()) {
            FrameGraphLayout.Page page = FrameGraphLayout.page(CONTENT, COUNTS, 0, breakpoint);

            assertTrue(page.markers().bottom() <= page.plot().y(), "markers over the plot");
            assertTrue(page.plot().bottom() <= page.tiles().get(0).y(), "plot over the tiles");
            Rect last = page.tiles().get(page.tiles().size() - 1);
            assertTrue(last.bottom() <= page.bottleneck().y(), "tiles over the footer");
            assertTrue(page.bottleneck().bottom() <= page.sampling().y());
            assertEquals(page.plot().y(), page.axis().y(), "the axis must line up with the plot");
        }
    }

    @Test
    @DisplayName("tiles form a grid that never runs past the content")
    void tilesStayInsideTheContent() {
        for (Breakpoint breakpoint : Breakpoint.values()) {
            FrameGraphLayout.Page page = FrameGraphLayout.page(CONTENT, COUNTS, 0, breakpoint);
            int columns = FrameGraphLayout.tileColumns(breakpoint);

            assertEquals(FrameGraphLayout.visibleTiles(6, breakpoint), page.tiles().size());
            for (Rect tile : page.tiles()) {
                assertTrue(tile.x() >= CONTENT.x() && tile.right() <= CONTENT.right(),
                        "a tile escapes at " + breakpoint);
            }
            for (int i = columns; i < page.tiles().size(); i++) {
                assertTrue(page.tiles().get(i).y() > page.tiles().get(i - columns).y(),
                        "the grid must wrap onto a new row");
            }
        }
    }

    @Test
    @DisplayName("compact keeps every tile and wraps them instead of hiding data")
    void compactKeepsEveryTile() {
        assertEquals(6, FrameGraphLayout.visibleTiles(6, Breakpoint.COMPACT));
        assertEquals(6, FrameGraphLayout.visibleTiles(6, Breakpoint.WIDE));
        assertTrue(FrameGraphLayout.tileColumns(Breakpoint.COMPACT)
                < FrameGraphLayout.tileColumns(Breakpoint.WIDE),
                "a narrow screen fits fewer per row, so it takes more rows");
        assertTrue(FrameGraphLayout.contentHeight(COUNTS, Breakpoint.COMPACT)
                > FrameGraphLayout.contentHeight(COUNTS, Breakpoint.WIDE),
                "compact wraps the same cells onto more rows, so the page gets taller, not shorter");
    }

    @Test
    @DisplayName("page height and contentHeight can never disagree")
    void heightMatchesContentHeight() {
        for (Breakpoint breakpoint : Breakpoint.values()) {
            for (int tiles = 0; tiles <= 6; tiles++) {
                assertEquals(FrameGraphLayout.contentHeight(counts(tiles), breakpoint),
                        FrameGraphLayout.page(CONTENT, counts(tiles), 0, breakpoint).height());
            }
        }
    }

    @Test
    @DisplayName("the target line sits on the plot, and vanishes when it is off scale")
    void baselineOnlyShowsWhenItFits() {
        Rect plot = FrameGraphLayout.page(CONTENT, COUNTS, 0, Breakpoint.WIDE).plot();

        Rect line = FrameGraphLayout.baseline(plot, 16.7f, 33.4f);
        assertFalse(line.isEmpty());
        assertTrue(line.y() >= plot.y() && line.bottom() <= plot.bottom());
        assertTrue(FrameGraphLayout.baseline(plot, 60.0f, 33.4f).isEmpty());
    }

    @Test
    @DisplayName("gridlines divide the plot and stay on it")
    void gridlinesStayOnThePlot() {
        Rect plot = FrameGraphLayout.page(CONTENT, COUNTS, 0, Breakpoint.WIDE).plot();
        List<Rect> lines = FrameGraphLayout.gridlines(plot, 4);

        assertEquals(3, lines.size());
        for (Rect line : lines) {
            assertTrue(line.y() > plot.y() && line.y() < plot.bottom());
            assertEquals(plot.width(), line.width());
        }
        assertTrue(FrameGraphLayout.gridlines(plot, 1).isEmpty());
    }

    @Test
    @DisplayName("a content area too narrow for an axis yields nothing rather than negative boxes")
    void degenerateContentYieldsNothing() {
        FrameGraphLayout.Page page = FrameGraphLayout.page(
                new Rect(0, 0, FrameGraphLayout.PAD_X * 2 + FrameGraphLayout.AXIS_W, 400),
                COUNTS, 0, Breakpoint.WIDE);

        assertEquals(0, page.height());
        assertTrue(page.plot().isEmpty());
        assertTrue(page.tiles().isEmpty());
        assertTrue(FrameGraphLayout.column(Rect.EMPTY, 0, 10, 8.0f, 16.0f, 33.4f).isEmpty());
    }

    @Test
    @DisplayName("the page rejects nulls rather than painting nowhere")
    void nullsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> FrameGraphLayout.page(null, COUNTS, 0, Breakpoint.WIDE));
        assertThrows(IllegalArgumentException.class,
                () -> FrameGraphLayout.page(CONTENT, COUNTS, 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> FrameGraphLayout.page(CONTENT, null, 0, Breakpoint.WIDE));
    }
}
