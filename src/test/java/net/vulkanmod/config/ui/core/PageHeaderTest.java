package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PageHeader")
class PageHeaderTest {
    private static final Rect CONTENT = new Rect(132, 32, 388, 400);

    private static PageHeader.Band band(boolean crumbs, boolean tabs, boolean subtitle) {
        return PageHeader.of(CONTENT, crumbs, tabs, subtitle, Breakpoint.WIDE);
    }

    @Test
    @DisplayName("an element that is not there costs nothing, so nothing leaves a gap")
    void absentElementsCostNothing() {
        for (Breakpoint breakpoint : Breakpoint.values()) {
            int bare = PageHeader.height(false, false, false, breakpoint);
            int crumbCost = PageHeader.height(true, false, false, breakpoint) - bare;
            int tabCost = PageHeader.height(false, true, false, breakpoint) - bare;
            int subCost = PageHeader.height(false, false, true, breakpoint) - bare;

            assertTrue(crumbCost > 0 && tabCost > 0 && subCost > 0);
            assertEquals(bare + crumbCost + tabCost + subCost,
                    PageHeader.height(true, true, true, breakpoint),
                    "the stack must be additive at " + breakpoint);
        }
    }

    @Test
    @DisplayName("a page without a path starts its title well above one that has a path")
    void aPageWithoutAPathLiftsItsTitle() {
        assertTrue(band(false, false, false).title().y() < band(true, false, false).title().y(),
                "the title must rise when there is no path to show");
        assertTrue(PageHeader.height(false, false, false, Breakpoint.WIDE)
                        < PageHeader.height(true, true, false, Breakpoint.WIDE) - 30,
                "a bare page should win back real room, not a few pixels");
    }

    @Test
    @DisplayName("the fullest band is never taller than the old fixed top")
    void theFullestBandDoesNotPushContentDown() {
        assertEquals(70, PageHeader.height(true, true, false, Breakpoint.WIDE));
        assertEquals(60, PageHeader.height(true, true, false, Breakpoint.COMPACT));
    }

    @Test
    @DisplayName("compact is shorter than wide for every combination")
    void compactIsAlwaysShorter() {
        for (int mask = 0; mask < 8; mask++) {
            boolean crumbs = (mask & 1) != 0;
            boolean tabs = (mask & 2) != 0;
            boolean subtitle = (mask & 4) != 0;
            assertTrue(PageHeader.height(crumbs, tabs, subtitle, Breakpoint.COMPACT)
                            < PageHeader.height(crumbs, tabs, subtitle, Breakpoint.WIDE),
                    "compact must save room for " + mask);
        }
    }

    @Test
    @DisplayName("the parts of the band stack in order and never overlap")
    void partsNeverOverlap() {
        for (Breakpoint breakpoint : Breakpoint.values()) {
            PageHeader.Band full = PageHeader.of(CONTENT, true, true, true, breakpoint);

            assertTrue(full.crumbs().bottom() <= full.title().y(), "path over title");
            assertTrue(full.title().bottom() <= full.subtitle().y(), "title over subtitle");
            assertTrue(full.subtitle().bottom() <= full.tabs().y(), "subtitle over tabs");
            assertTrue(full.tabs().bottom() <= full.rule().y(), "tabs over rule");
            assertTrue(full.rule().bottom() < full.bounds().bottom(), "the rule must sit inside the band");
        }
    }

    @Test
    @DisplayName("every part stays inside the band it belongs to")
    void everyPartStaysInsideTheBand() {
        PageHeader.Band full = band(true, true, true);
        Rect bounds = full.bounds();

        for (Rect part : new Rect[] {full.bar(), full.crumbs(), full.title(), full.subtitle(),
                full.tabs(), full.rule()}) {
            assertTrue(part.y() >= bounds.y() && part.bottom() <= bounds.bottom(),
                    "a part escapes the band vertically");
            assertTrue(part.x() >= bounds.x() && part.right() <= bounds.right(),
                    "a part escapes the band horizontally");
        }
        assertEquals(full.height(), bounds.height());
    }

    @Test
    @DisplayName("the accent bar marks the identity block and stops before the tabs")
    void theBarMarksIdentityOnly() {
        PageHeader.Band full = band(true, true, true);
        assertEquals(full.crumbs().y(), full.bar().y(), "the bar starts at the path");
        assertEquals(full.subtitle().bottom(), full.bar().bottom(), "the bar ends at the subtitle");
        assertTrue(full.bar().bottom() <= full.tabs().y(), "the bar must never run alongside the tabs");

        PageHeader.Band bare = band(false, false, false);
        assertEquals(bare.title().y(), bare.bar().y());
        assertEquals(bare.title().bottom(), bare.bar().bottom());
    }

    @Test
    @DisplayName("text starts on the same column as a settings row label")
    void textSharesTheSettingsRowColumn() {
        PageHeader.Band full = band(true, true, true);
        int rowLabel = CONTENT.x() + PageHeader.PAD_X + SettingRowLayout.CARD_PAD_X;

        assertEquals(rowLabel, full.title().x());
        assertEquals(rowLabel, full.crumbs().x());
        assertEquals(rowLabel, full.subtitle().x());
        assertEquals(CONTENT.x() + PageHeader.PAD_X, full.bar().x());
        assertEquals(CONTENT.x() + PageHeader.PAD_X, full.rule().x());
        assertEquals(CONTENT.x() + PageHeader.PAD_X, full.tabs().x());
    }

    @Test
    @DisplayName("the body starts exactly where the band ends")
    void theBodyStartsWhereTheBandEnds() {
        for (Breakpoint breakpoint : Breakpoint.values()) {
            PageHeader.Band full = PageHeader.of(CONTENT, true, true, false, breakpoint);
            Rect body = CONTENT.dropTop(full.height());

            assertEquals(CONTENT.y() + full.height(), body.y());
            assertEquals(CONTENT.bottom(), body.bottom(), "the body must still end where content ends");
            assertTrue(body.y() > full.rule().bottom(), "the body would start on top of the rule");
        }
    }

    @Test
    @DisplayName("a content area with no usable width yields nothing rather than negative boxes")
    void degenerateContentYieldsNothing() {
        PageHeader.Band band = PageHeader.of(new Rect(0, 0, PageHeader.PAD_X * 2, 200),
                true, true, true, Breakpoint.WIDE);

        assertEquals(0, band.height());
        assertTrue(band.bounds().isEmpty());
        assertTrue(band.title().isEmpty());
    }

    @Test
    @DisplayName("dropping more than the height leaves an empty body, never a negative one")
    void droppingTooMuchLeavesNothing() {
        assertEquals(0, CONTENT.dropTop(CONTENT.height() + 50).height());
        assertThrows(IllegalArgumentException.class, () -> CONTENT.dropTop(-1));
    }

    @Test
    @DisplayName("the band rejects a null content area or breakpoint")
    void nullsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> PageHeader.of(null, true, true, true, Breakpoint.WIDE));
        assertThrows(IllegalArgumentException.class,
                () -> PageHeader.of(CONTENT, true, true, true, null));
        assertThrows(IllegalArgumentException.class,
                () -> PageHeader.height(true, true, true, null));
    }

    @Test
    @DisplayName("a subtitle wraps to fewer lines in compact but never changes the stack shape")
    void compactSubtitleIsOneLine() {
        assertEquals(2, PageHeader.subLines(Breakpoint.WIDE));
        assertEquals(1, PageHeader.subLines(Breakpoint.COMPACT));

        PageHeader.Band compact = PageHeader.of(CONTENT, true, true, true, Breakpoint.COMPACT);
        assertEquals(PageHeader.SUB_LINE, compact.subtitle().height());
        assertFalse(compact.subtitle().isEmpty());
    }
}
