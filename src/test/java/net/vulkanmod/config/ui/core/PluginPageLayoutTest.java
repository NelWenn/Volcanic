package net.vulkanmod.config.ui.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PluginPageLayout")
class PluginPageLayoutTest {
    private static final Rect CONTENT = new Rect(132, 32, 388, 400);

    private static PluginPageLayout.Page page(int plugins, int mods) {
        return PluginPageLayout.page(CONTENT, plugins, mods, 0, Breakpoint.WIDE);
    }

    @Test
    @DisplayName("nothing on the page ever overlaps anything else")
    void bandsNeverOverlap() {
        PluginPageLayout.Page page = page(3, 4);

        List<Rect> stack = new java.util.ArrayList<>();
        stack.add(page.header());
        stack.add(page.plugins().rule());
        stack.addAll(page.plugins().rows());
        stack.add(page.mods().rule());
        stack.addAll(page.mods().rows());

        for (int index = 1; index < stack.size(); index++) {
            assertTrue(stack.get(index).y() >= stack.get(index - 1).bottom(),
                    "band " + index + " starts at " + stack.get(index).y()
                            + " before the previous one ends at " + stack.get(index - 1).bottom());
        }
    }

    @Test
    @DisplayName("the page reports exactly the height its last row needs")
    void heightCoversTheLastRow() {
        for (int plugins = 0; plugins <= 3; plugins++) {
            for (int mods = 0; mods <= 3; mods++) {
                PluginPageLayout.Page page = page(plugins, mods);
                List<Rect> rows = page.mods().rows().isEmpty()
                        ? page.plugins().rows() : page.mods().rows();
                if (rows.isEmpty()) {
                    continue;
                }
                int used = rows.get(rows.size() - 1).bottom() - CONTENT.y() + PluginPageLayout.BOTTOM;
                assertEquals(used, page.height(),
                        "height disagrees with the last row for " + plugins + " plugins / " + mods + " mods");
            }
        }
    }

    @Test
    @DisplayName("page height and contentHeight can never disagree")
    void heightMatchesContentHeight() {
        for (Breakpoint breakpoint : Breakpoint.values()) {
            for (int plugins = 0; plugins <= 4; plugins++) {
                for (int mods = 0; mods <= 4; mods++) {
                    assertEquals(PluginPageLayout.contentHeight(plugins, mods, breakpoint),
                            PluginPageLayout.page(CONTENT, plugins, mods, 0, breakpoint).height(),
                            "scrolling would stop short at " + breakpoint);
                }
            }
        }
    }

    @Test
    @DisplayName("an empty block costs nothing at all")
    void emptyBlocksTakeNoRoom() {
        assertTrue(page(2, 0).mods().rows().isEmpty());
        assertTrue(page(2, 0).mods().rule().isEmpty());
        assertEquals(page(2, 0).height(),
                PluginPageLayout.contentHeight(2, 0, Breakpoint.WIDE));
    }

    @Test
    @DisplayName("no plugins but mods present keeps the block and shows one unclickable placeholder")
    void placeholderStandsInForAnEmptyPluginBlock() {
        PluginPageLayout.Page page = page(0, 2);

        assertTrue(page.plugins().placeholder());
        assertEquals(1, page.plugins().rows().size());
        assertFalse(page.plugins().rule().isEmpty());
        assertFalse(page.mods().rows().isEmpty());
    }

    @Test
    @DisplayName("with nothing installed the page is one centred card and no blocks")
    void totallyEmptyPageIsACard() {
        PluginPageLayout.Page page = page(0, 0);

        assertFalse(page.empty().isEmpty());
        assertTrue(page.header().isEmpty());
        assertTrue(page.plugins().rows().isEmpty());
        assertTrue(page.mods().rows().isEmpty());
        assertTrue(PluginPageLayout.emptyTitle(page.empty()).y() < PluginPageLayout.emptyBody(page.empty()).y());
        assertTrue(PluginPageLayout.emptyBody(page.empty()).bottom() <= page.empty().bottom());
    }

    @Test
    @DisplayName("row parts stay inside the row and never overlap each other")
    void slotsStayInsideTheRow() {
        for (Breakpoint breakpoint : Breakpoint.values()) {
            Rect row = PluginPageLayout.page(CONTENT, 1, 0, 0, breakpoint).plugins().rows().get(0);
            PluginPageLayout.Slots slots = PluginPageLayout.slots(row, true);

            assertFalse(slots.toggle().isEmpty(), "a toggleable plugin must show its switch at " + breakpoint);
            assertTrue(slots.toggle().right() <= row.right(), "toggle escapes the row");
            assertTrue(slots.toggle().y() >= row.y() && slots.toggle().bottom() <= row.bottom());
            assertTrue(slots.name().right() <= slots.toggle().x(), "name runs under the toggle");
            assertTrue(slots.name().bottom() <= row.bottom(), "name escapes the row");
            if (!slots.note().isEmpty()) {
                assertTrue(slots.note().y() >= slots.name().bottom(), "note overlaps the name");
                assertTrue(slots.note().bottom() <= row.bottom(), "note escapes the row");
            }
        }
    }

    @Test
    @DisplayName("a compact row drops the note rather than stacking text on itself")
    void compactRowsDropTheNote() {
        Rect wide = PluginPageLayout.page(CONTENT, 1, 0, 0, Breakpoint.WIDE).plugins().rows().get(0);
        Rect compact = PluginPageLayout.page(CONTENT, 1, 0, 0, Breakpoint.COMPACT).plugins().rows().get(0);

        assertFalse(PluginPageLayout.slots(wide, true).note().isEmpty());
        if (compact.height() < PluginPageLayout.TWO_LINE_MIN) {
            assertTrue(PluginPageLayout.slots(compact, true).note().isEmpty());
        }
    }

    @Test
    @DisplayName("a plugin that cannot be switched shows a state word instead of a switch")
    void nonToggleablePluginsShowStateNotASwitch() {
        Rect row = page(1, 0).plugins().rows().get(0);

        assertTrue(PluginPageLayout.slots(row, false).toggle().isEmpty());
        assertFalse(PluginPageLayout.slots(row, false).state().isEmpty());
        assertTrue(PluginPageLayout.slots(row, true).state().isEmpty());
    }

    @Test
    @DisplayName("a row too narrow for a control gives up the control, never a broken one")
    void narrowRowsDegradeRatherThanBreak() {
        Rect narrow = new Rect(0, 0, 40, 27);
        PluginPageLayout.Slots slots = PluginPageLayout.slots(narrow, true);

        assertTrue(slots.toggle().isEmpty());
        assertTrue(slots.name().isEmpty() || slots.name().right() <= narrow.right());
    }

    @Test
    @DisplayName("scrolling moves every band by exactly the scroll amount")
    void scrollShiftsTheWholePage() {
        PluginPageLayout.Page resting = page(2, 2);
        PluginPageLayout.Page scrolled = PluginPageLayout.page(CONTENT, 2, 2, 40, Breakpoint.WIDE);

        assertEquals(resting.header().y() - 40, scrolled.header().y());
        assertEquals(resting.plugins().rows().get(0).y() - 40, scrolled.plugins().rows().get(0).y());
        assertEquals(resting.mods().rows().get(1).y() - 40, scrolled.mods().rows().get(1).y());
        assertEquals(resting.height(), scrolled.height(), "scrolling must not change the page height");
    }

    @Test
    @DisplayName("the accent lead only marks the plugins rule, and stays on it")
    void ruleLeadStaysOnItsRule() {
        Rect rule = page(1, 1).plugins().rule();
        Rect lead = PluginPageLayout.ruleLead(rule);

        assertEquals(rule.x(), lead.x());
        assertEquals(rule.y(), lead.y());
        assertTrue(lead.right() <= rule.right());
        assertTrue(PluginPageLayout.ruleLead(Rect.EMPTY).isEmpty());
    }

    @Test
    @DisplayName("a content area with no usable width yields nothing rather than negative boxes")
    void degenerateContentYieldsNothing() {
        PluginPageLayout.Page page =
                PluginPageLayout.page(new Rect(0, 0, PluginPageLayout.PAD_X * 2, 100), 2, 2, 0, Breakpoint.WIDE);

        assertEquals(0, page.height());
        assertTrue(page.header().isEmpty());
        assertTrue(page.plugins().rows().isEmpty());
    }
}
